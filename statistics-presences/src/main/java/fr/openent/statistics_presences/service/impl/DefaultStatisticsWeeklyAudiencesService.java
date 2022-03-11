package fr.openent.statistics_presences.service.impl;


import com.mongodb.QueryBuilder;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.presences.Presences;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DBService;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.bean.Register;
import fr.openent.statistics_presences.bean.timeslot.Slot;
import fr.openent.statistics_presences.bean.timeslot.Timeslot;
import fr.openent.statistics_presences.bean.weekly.WeeklyAudience;
import fr.openent.statistics_presences.helper.RegisterHelper;
import fr.openent.statistics_presences.helper.TimeslotHelper;
import fr.openent.statistics_presences.indicator.ProcessingWeeklyAudiencesManual;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import fr.openent.statistics_presences.service.StatisticsWeeklyAudiencesService;
import fr.wseduc.mongodb.MongoQueryBuilder;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultStatisticsWeeklyAudiencesService extends DBService implements StatisticsWeeklyAudiencesService {
    protected final Logger log = LoggerFactory.getLogger(DefaultStatisticsWeeklyAudiencesService.class);
    private final CommonServiceFactory commonServiceFactory;


    public DefaultStatisticsWeeklyAudiencesService(CommonServiceFactory commonServiceFactory) {
        this.commonServiceFactory = commonServiceFactory;
    }

    @Override
    public Future<JsonObject> create(String structureId, List<Integer> registerIds) {
        Promise<JsonObject> promise = Promise.promise();

        Presences.getInstance().getRegistersWithGroups(structureId, registerIds, null, null, null)
                .compose(registerResults -> {
                    List<Register> registers = RegisterHelper.getRegistersFromArray(registerResults);

                    String resStructureId = structureId;
                    if (resStructureId == null) {
                        List<String> structureIds = registers.stream()
                                .map(Register::getStructureId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .collect(Collectors.toList());

                        if (structureIds.size() != 1) {
                            String message = String.format("[StatisticsPresences@%s::create] " +
                                    "Can not save registers from different structures", this.getClass().getSimpleName());
                            return Future.failedFuture(message);
                        }
                        resStructureId = structureIds.get(0);
                    }

                    return createFromRegisters(resStructureId, RegisterHelper.getRegistersFromArray(registerResults));
                })
                .onFailure(promise::fail)
                .onSuccess(createResults -> promise.complete());

        return promise.future();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Future<JsonObject> createFromRegisters(String structureId, List<Register> registers) {
        Promise<JsonObject> promise = Promise.promise();
        List<String> audienceIds = registers.stream()
                .map(Register::getAudienceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Future<JsonArray> timeslotsFuture = Viescolaire.getInstance().getAudienceTimeslots(structureId, audienceIds);
        Future<JsonArray> countStudentsFuture = Viescolaire.getInstance().getCountStudentsByAudiences(audienceIds);
        Future<List<Register>> registersFuture = filterRegisterByUnsavedWeeklyAudiences(registers);

        CompositeFuture.all(timeslotsFuture, countStudentsFuture, registersFuture)
                .compose(futures -> {
                    List<Timeslot> timeslots = TimeslotHelper.getRegistersFromArray(timeslotsFuture.result());
                    List<JsonObject> groupCountStudents = countStudentsFuture.result().getList();
                    List<JsonObject> weeklyAudiences = mapRegistersToMongoWeeklyAudiences(registersFuture.result(), timeslots, groupCountStudents);
                    return storeValues(new JsonArray(weeklyAudiences));
                })
                .onSuccess(res -> promise.complete())
                .onFailure(error -> {
                    String message = String.format("[StatisticsPresences@%s::createFromRegisters] " +
                            "Failed create weekly audiences from registers", this.getClass().getSimpleName());
                    log.error(String.format("%s. %s", message, error));
                    promise.fail(message);
                });

        return promise.future();
    }


    private List<JsonObject> mapRegistersToMongoWeeklyAudiences(List<Register> registers, List<Timeslot> timeslots, List<JsonObject> groupCountStudents) {
        return registers.stream()
                .flatMap(register -> {
                    Timeslot timeslot = timeslots.stream()
                            .filter(timeslotFilter -> timeslotFilter.getAudienceId().equals(register.getAudienceId()))
                            .findFirst()
                            .orElse(null);

                    JsonObject countStudents = groupCountStudents.stream()
                            .filter(countStudentsFilter -> countStudentsFilter.getString(Field.ID_GROUPE).equals(register.getAudienceId()))
                            .findFirst()
                            .orElse(new JsonObject().put(Field.NB, 0));


                    return mapRegisterToMongoWeeklyAudiences(register, timeslot, countStudents).stream();
                })
                .collect(Collectors.toList());
    }

    private List<JsonObject> mapRegisterToMongoWeeklyAudiences(Register register, Timeslot timeslot, JsonObject countStudents) {
        if (timeslot == null) {
            String message = String.format("[StatisticsPresences@%s::mapRegisterToWeeklyAudiences] " +
                            "Timeslot not found in structure %s for audience %s",
                    this.getClass().getSimpleName(), register.getStructureId(), register.getAudienceId());
            log.error(message);
            return Collections.emptyList();
        }

        List<Slot> slots = TimeslotHelper.getSlotsFromPeriod(register.getStartAt(),
                register.getEndAt(), timeslot.getSlots());

        if (slots == null || slots.isEmpty()) {
            String message = String.format("[StatisticsPresences@%s::mapRegisterToWeeklyAudiences] " +
                            "Slots not found in structure %s for register %s",
                    this.getClass().getSimpleName(), register.getStructureId(), register.getId());
            log.error(message);
            return Collections.singletonList(new WeeklyAudience()
                    .setSlotId(null)
                    .setStructureId(register.getStructureId())
                    .setAudienceId(register.getAudienceId())
                    .setRegisterId(register.getId())
                    .setStartAt(register.getStartAt())
                    .setEndAt(register.getEndAt())
                    .setStudentCount(countStudents.getInteger(Field.NB, 0))
                    .toJSON());
        }
        return slots.stream()
                .map(slot -> new WeeklyAudience()
                        .setStructureId(register.getStructureId())
                        .setAudienceId(register.getAudienceId())
                        .setRegisterId(register.getId())
                        .setSlotId(slot.getId())
                        .setStartAt(DateHelper.setTimeToDate(
                                register.getStartAt(), slot.getStartHour(), DateHelper.HOUR_MINUTES, DateHelper.SQL_FORMAT
                        ))
                        .setEndAt(DateHelper.setTimeToDate(
                                register.getStartAt(), slot.getEndHour(), DateHelper.HOUR_MINUTES, DateHelper.SQL_FORMAT
                        ))
                        .setStudentCount(countStudents.getInteger(Field.NB, 0))
                        .toJSON())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Future<List<Register>> filterRegisterByUnsavedWeeklyAudiences(List<Register> registers) {
        Promise<List<Register>> promise = Promise.promise();
        List<Integer> registerIds = registers.stream().map(Register::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        getWeeklyAudiences(registerIds)
                .onFailure(err -> {
                    String message = String.format("[StatisticsPresences@%s::filterRegisterByUnsavedWeeklyAudiences]" +
                            " failed to retrieve weeklyAudiences.", this.getClass().getSimpleName());
                    log.error(String.format("%s %s", message, err.getMessage()));
                    promise.fail(message);
                })
                .onSuccess(weeklyAudiences ->
                        promise.complete(
                                registers
                                        .stream()
                                        .filter(register ->
                                                ((List<JsonObject>) weeklyAudiences.getList()).stream()
                                                        .noneMatch(weeklyAudience -> {
                                                            JsonObject id = weeklyAudience.getJsonObject(Field._ID);
                                                            return register.getId() == null || register.getAudienceId() == null ||
                                                                    (register.getId().equals(id.getInteger(Field.REGISTER_ID)) &&
                                                                            register.getAudienceId().equals(id.getString(Field.AUDIENCE_ID)));
                                                        }))
                                        .collect(Collectors.toList())
                        ));
        return promise.future();
    }

    private Future<Void> storeValues(JsonArray weeklyAudiences) {
        Promise<Void> promise = Promise.promise();
        if (!weeklyAudiences.isEmpty()) {
            mongoDb.insert(StatisticsPresences.WEEKLY_AUDIENCES_COLLECTION, weeklyAudiences,
                    MongoDbResult.validResultHandler(results -> {
                        if (results.isLeft()) {
                            String message = String.format("[StatisticsPresences@%s::createFromRegisters] " +
                                    "Failed to store new values", this.getClass().getSimpleName());

                            log.error(String.format("%s. %s", message, results.left().getValue()));
                            promise.fail(message);
                            return;
                        }
                        promise.complete();
                    }));
        } else promise.complete();

        return promise.future();
    }

    private Future<JsonArray> getWeeklyAudiences(List<Integer> registerIds) {
        Promise<JsonArray> promise = Promise.promise();
        QueryBuilder matcher = QueryBuilder.start(String.format("%s.%s", Field._ID, Field.REGISTER_ID)).in(registerIds);
        mongoDb.find(StatisticsPresences.WEEKLY_AUDIENCES_COLLECTION, MongoQueryBuilder.build(matcher),
                MongoDbResult.validResultsHandler(FutureHelper.handlerJsonArray(promise)));
        return promise.future();
    }

    @Override
    public Future<JsonObject> processWeeklyAudiencesPrefetch(List<String> structureIds, String startAt, String endAt) {
        Promise<JsonObject> promise = Promise.promise();
        if (structureIds.isEmpty()) {
            String message = String.format("[StatisticsPresences@%s::processWeeklyAudiencesPrefetch] " +
                    "No structure(s) identifier given", this.getClass().getSimpleName());
            promise.fail(message);
        } else {
            JsonObject params = new JsonObject()
                    .put(Field.STRUCTUREIDS, structureIds)
                    .put(Field.STARTAT, startAt)
                    .put(Field.ENDAT, endAt);

            commonServiceFactory.vertx().deployVerticle(ProcessingWeeklyAudiencesManual.class.getName(),
                    new DeploymentOptions().setConfig(params).setWorker(true));
            promise.complete(new JsonObject().put(Field.STATUS, Field.OK));
        }

        return promise.future();
    }

}
