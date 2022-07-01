package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.helper.PunishmentHelper;
import fr.openent.incidents.helper.PunishmentTypeHelper;
import fr.openent.incidents.model.Punishment;
import fr.openent.incidents.model.PunishmentType;
import fr.openent.incidents.model.punishmentCategory.DetentionCategory;
import fr.openent.incidents.model.punishmentCategory.ExcludeCategory;
import fr.openent.incidents.model.punishmentCategory.PunishmentCategory;
import fr.openent.incidents.service.PunishmentService;
import fr.openent.incidents.service.PunishmentTypeService;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.presences.Presences;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.common.statistics_presences.StatisticsPresences;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.EventType;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.user.UserInfos;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultPunishmentService implements PunishmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPunishmentService.class);
    private static final String COUNTID = "countId";
    private static final String $COUNTID = "$countId";
    private final PunishmentHelper punishmentHelper;
    private final Punishment punishment = new Punishment();
    private final PunishmentTypeService punishmentType = new DefaultPunishmentTypeService();
    private final UserService userService = new DefaultUserService();


    public DefaultPunishmentService(EventBus eb) {
        this.punishmentHelper = new PunishmentHelper(eb);
    }

    @Override
    public void create(UserInfos user, JsonObject body, Handler<AsyncResult<JsonArray>> handler) {
        String structureId = body.getString(Field.STRUCTURE_ID, "");
        Long typeId = body.getLong(Field.TYPEID);
        PunishmentCategory.getSpecifiedCategoryFromType(structureId, typeId)
                .onFailure(error -> {
                    String message = String.format("[Incidents@%s::createPunishments] Fail to get category label"
                            , this.getClass().getSimpleName());
                    LOGGER.error(String.format("%s %s", message, error));
                    handler.handle(Future.failedFuture(message));
                })
                .onSuccess(category -> {
                    Future<JsonObject> punishmentsFuture = Future.future();
                    Future<JsonObject> absencesFuture = Future.future();
                    createPunishments(user, body, category, punishmentsFuture);
                    createRelatedAbsences(structureId, user, body, category.getLabel(), absencesFuture);

                    FutureHelper.all(Arrays.asList(punishmentsFuture, absencesFuture))
                            .onFailure(error -> handler.handle(Future.failedFuture(error.getMessage())))
                            .onSuccess(result -> handler.handle(Future.succeededFuture(
                                    punishmentsFuture.result().getJsonArray(Field.ALL, new JsonArray())
                            )));
                });
    }

    @SuppressWarnings("unchecked")
    private void createPunishments(UserInfos user, JsonObject body, PunishmentCategory category, Handler<AsyncResult<JsonObject>> handler) {
        List<String> studentIds = body.getJsonArray(Field.STUDENT_IDS).getList();
        List<Punishment> punishments = initPunishmentsFromFields(category, body);
        JsonArray results = new JsonArray();
        FutureHelper.join(createPunishments(user, punishments, studentIds, category, results))
                .onFailure(error -> {
                    String message = String.format("[Incidents@%s::createPunishments] Fail to create punishments"
                            , this.getClass().getSimpleName());
                    LOGGER.error(String.format("%s %s", message, error.getMessage()));
                    handler.handle(Future.failedFuture(message));
                })
                .onSuccess(result -> {
                    StatisticsPresences.getInstance().postUsers(body.getString("structure_id"), studentIds);
                    handler.handle(Future.succeededFuture(new JsonObject().put("all", results)));
                });
    }

    @SuppressWarnings("unchecked")
    private List<Punishment> initPunishmentsFromFields(PunishmentCategory category, JsonObject body) {
        if (DetentionCategory.DETENTION.equals(category.getLabel())) {
            // Cast list as Map, because it's easier to manipulate than LinkedHashMap
            List<Map<String, Object>> fields = body.getJsonArray(Field.FIELDS, new JsonArray()).getList();
            String grouped_punishment_id = body.getString(Field.GROUPED_PUNISHMENT_ID, null);
            body.remove(Field.FIELDS);
            return fields.stream()
                    .map((mapField) -> {
                        Punishment punishment = new Punishment();
                        punishment.setFromJson(body);
                        JsonObject field = new JsonObject(mapField);
                        punishment.setId(field.getString(Field.ID));
                        PunishmentCategory.formatFromBody(category, field);
                        punishment.setFields(category.toJsonObject());
                        punishment.setGroupedPunishmentId(grouped_punishment_id);
                        return punishment;
                    }).collect(Collectors.toList());
        } else {
            Punishment punishment = new Punishment();
            punishment.setFromJson(body);
            PunishmentCategory.formatFromBody(category, punishment.getFields());
            punishment.setFields(category.toJsonObject());
            return Collections.singletonList(punishment);
        }
    }

    private List<Future<JsonObject>> createPunishments(UserInfos user, List<Punishment> punishments, List<String> studentIds,
                                                       PunishmentCategory category, JsonArray results) {
        List<Future<JsonObject>> futures = new ArrayList<>();

        for (String studentId : studentIds) {
            String groupedPunishmentId = DetentionCategory.DETENTION.equals(category.getLabel()) ?
                    UUID.randomUUID().toString() : null;
            for (Punishment punishment : punishments) {
                // re-init punishments, to keep reference of each punishments
                Punishment createPunishment = new Punishment();
                createPunishment.setFromJson(punishment.toJsonObject());
                createPunishment.setGroupedPunishmentId(groupedPunishmentId);
                createPunishment.setStudentId(studentId);
                futures.add(persistMongo(createPunishment, user, results));
            }
        }
        return futures;
    }

    private void createRelatedAbsences(String structureId, UserInfos user, JsonObject body, String categoryLabel, Handler<AsyncResult<JsonObject>> handler) {
        if (!PunishmentCategory.EXCLUDE.equals(categoryLabel) || !body.containsKey(Field.ABSENCE)) {
            handler.handle(Future.succeededFuture(new JsonObject()));
            return;
        }
        JsonObject fields = body.getJsonObject("fields", new JsonObject());
        Long reasonId = body.getJsonObject("absence").getLong("reason_id");
        Boolean followed = body.getJsonObject("absence").getBoolean("followed", false);
        String startAt = fields.getString("start_at").replace("/", "-");
        String endAt = fields.getString("end_at").replace("/", "-");

        getStudentsWithoutAbsences(body, startAt, endAt).setHandler(resultStudentIds -> {
            if (resultStudentIds.failed()) {
                handler.handle(Future.failedFuture(resultStudentIds.cause().getMessage()));
                return;
            }
            List<String> studentIds = resultStudentIds.result();
            if (studentIds == null || studentIds.isEmpty()) {
                handler.handle(Future.succeededFuture(new JsonObject()));
                return;
            }
            Presences.getInstance()
                    .createAbsences(structureId, studentIds, user.getUserId(), reasonId, startAt,
                            endAt, true, followed, handler);
        });
    }

    @SuppressWarnings("unchecked")
    private Future<List<String>> getStudentsWithoutAbsences(JsonObject body, String startAt, String endAt) {
        Future<List<String>> future = Future.future();
        if (startAt == null || endAt == null) {
            future.fail("[Incidents@DefaultPunishmentService::getStudentsWithoutAbsences] Missing period dates to retrieve absences");
            return future;
        }

        getAbsencesByStudentIds(body.getJsonArray("student_ids").getList(), startAt, endAt, absencesByStudentIds -> {
            if (absencesByStudentIds.failed()) {
                future.fail(absencesByStudentIds.cause().getMessage());
                return;
            }
            future.complete(
                    absencesByStudentIds.result()
                            .entrySet().stream()
                            .filter(absences -> absences.getValue().isEmpty())
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList())
            );
        });

        return future;
    }

    @Override
    public void update(UserInfos user, JsonObject body, Handler<AsyncResult<JsonArray>> handler) {
        String structureId = body.getString(Field.STRUCTURE_ID, "");
        Long typeId = body.getLong(Field.TYPEID);
        PunishmentCategory.getSpecifiedCategoryFromType(structureId, typeId)
                .onFailure(error -> {
                    String message = String.format("[Incidents@%s::createPunishments] Fail to get category label"
                            , this.getClass().getSimpleName());
                    LOGGER.error(String.format("%s %s", message, error));
                    handler.handle(Future.failedFuture(message));
                })
                .onSuccess(category -> {
                    Future<JsonObject> punishmentsFuture = updatePunishments(user, body, category);
                    Future<JsonObject> absencesFuture = updateRelatedAbsence(structureId, user, body, category.getLabel());

                    FutureHelper.all(Arrays.asList(punishmentsFuture, absencesFuture))
                            .onFailure(error -> handler.handle(Future.failedFuture(error.getMessage())))
                            .onSuccess(result -> handler.handle(Future.succeededFuture(
                                    punishmentsFuture.result().getJsonArray(Field.ALL, new JsonArray())
                            )));
                });
    }

    public Future<JsonObject> updatePunishments(UserInfos user, JsonObject body, PunishmentCategory category) {
        Promise<JsonObject> promise = Promise.promise();

        List<Punishment> punishments = initPunishmentsFromFields(category, body);
        JsonArray results = new JsonArray();
        FutureHelper.join(updatePunishments(user, punishments, category, results))
                .onFailure(error -> {
                    String message = String.format("[Incidents@%s::createPunishments] Fail to create punishments"
                            , this.getClass().getSimpleName());
                    LOGGER.error(String.format("%s %s", message, error.getMessage()));
                    promise.fail(message);
                })
                .onSuccess(result -> {
                    List<String> studentIds = punishments.stream().map(Punishment::getStudentId).collect(Collectors.toList());
                    StatisticsPresences.getInstance().postUsers(body.getString(Field.STRUCTURE_ID), studentIds);
                    promise.complete(new JsonObject().put(Field.ALL, results));
                });

        return promise.future();
    }

    public List<Future<JsonObject>> updatePunishments(UserInfos user, List<Punishment> updatePunishments, PunishmentCategory category, JsonArray results) {
        List<Future<JsonObject>> futures = new ArrayList<>();
        String groupedPunishmentId = PunishmentCategory.DETENTION.equals(category.getLabel()) ?
                UUID.randomUUID().toString() : null;
        for (Punishment punishment : updatePunishments) {
            // re-init punishments, to keep reference of each punishments
            Punishment updatePunishment = new Punishment();
            updatePunishment.setFromJson(punishment.toJsonObject());
            updatePunishment.setGroupedPunishmentId(punishment.getGroupedPunishmentId() == null ? groupedPunishmentId: punishment.getGroupedPunishmentId());
            futures.add(persistMongo(updatePunishment, user, results));
        }
        return futures;
    }

    private Future<JsonObject> persistMongo(Punishment punishment, UserInfos user, JsonArray results) {
        Promise<JsonObject> promise = Promise.promise();
        punishment.persistMongo(user)
                .onFailure(error -> {
                    String message = String.format("[Incidents@%s::persistMongo] Fail to persist punishment for student: %s"
                            , this.getClass().getSimpleName(), punishment.getStudentId());
                    LOGGER.error(String.format("%s %s", message, error.getMessage()));
                    promise.fail(message);
                })
                .onSuccess(result -> {
                    results.add(result);
                    promise.complete();
                });
        return promise.future();
    }

    private Future<JsonObject> updateRelatedAbsence(String structureId, UserInfos user, JsonObject body, String categoryLabel) {
        Promise<JsonObject> promise = Promise.promise();
        String punishmentId = body.getString(Field.ID);
        if (!body.containsKey(Field.ABSENCE) || !PunishmentCategory.EXCLUDE.equals(categoryLabel) || punishmentId == null) {
            promise.complete(new JsonObject());
            return promise.future();
        }

        Punishment oldPunishment = new Punishment();
        ExcludeCategory oldCategory = new ExcludeCategory();

        ExcludeCategory category = new ExcludeCategory();
        category.setFromJson(body.getJsonObject(Field.FIELDS));
        category.formatDates();

        get(user, punishmentId, null, structureId, null, null, null, null,
                null, null, false, null, false, null, null, null)
                .compose(oldPunishmentResult -> {
                    oldPunishment.setFromJson(oldPunishmentResult);

                    oldCategory.setFromJson(oldPunishment.getFields());
                    oldCategory.formatDates();

                    return getStudentAbsences(oldPunishment.getStudentId(), oldCategory.getStartAt(), oldCategory.getEndAt());
                })
                .onFailure(error -> {
                    String message = String.format("[Incidents@%s::updateRelatedAbsence] Fail to get update punishment related absence"
                            , this.getClass().getSimpleName());
                    LOGGER.error(String.format("%s %s", message, error));
                    promise.fail(message);
                })
                .onSuccess(absences -> {
                    Long reasonId = body.getJsonObject(Field.ABSENCE).getLong(Field.REASON_ID);

                    if (absences.isEmpty()) {
                        Presences.getInstance()
                                .createAbsences(structureId, Collections.singletonList(oldPunishment.getStudentId()),
                                        user.getUserId(), reasonId, category.getStartAt(), category.getEndAt(), true, true, promise);
                    } else if (absences.size() == 1 && areAbsenceDatesCorresponding(absences.get(0), oldCategory.getStartAt(), oldCategory.getEndAt())) {
                        Presences.getInstance()
                                .updateAbsence(structureId, absences.get(0).getLong(Field.ID),
                                        user.getUserId(), reasonId, oldPunishment.getStudentId(), category.getStartAt(), category.getEndAt(), true, promise);
                    } else {
                        promise.complete(new JsonObject());
                    }
                });
        return promise.future();
    }


    @SuppressWarnings("unchecked")
    private Future<List<JsonObject>> getStudentAbsences(String studentId, String startAt, String endAt) {
        Future<List<JsonObject>> future = Future.future();
        if (startAt == null || endAt == null) {
            future.fail("[Incidents@DefaultPunishmentService::getStudentAbsences] Missing period dates to retrieve absences");
            return future;
        }

        getAbsencesByStudentIds(Collections.singletonList(studentId), startAt, endAt, absencesByStudentIdsResult -> {
            if (absencesByStudentIdsResult.failed()) {
                future.fail(absencesByStudentIdsResult.cause().getMessage());
                return;
            }

            List<JsonObject> absences = absencesByStudentIdsResult.result().getOrDefault(studentId, new ArrayList<>());

            future.complete(absences);
        });

        return future;
    }

    private boolean areAbsenceDatesCorresponding(JsonObject absence, String startAt, String endAt) {
        return DateHelper.isDateEqual(absence.getString("start_date"), startAt) && DateHelper.isDateEqual(absence.getString("end_date"), endAt);
    }

    @Override
    public void get(UserInfos user, MultiMap body, boolean isStudent, Handler<AsyncResult<JsonObject>> handler) {
        String id = body.get(Field.ID);
        String structureId = body.get(Field.STRUCTURE_ID);
        String startAt = body.get(Field.START_AT);
        String endAt = body.get(Field.END_AT);
        List<String> studentIds = body.getAll(Field.STUDENT_ID);
        List<String> groupIds = body.getAll(Field.GROUP_ID);
        List<String> typeIds = body.getAll(Field.TYPE_ID);
        List<String> processStates = body.getAll(Field.PROCESS);
        String order = body.get(Field.ORDER) != null ?  body.get(Field.ORDER) : Field.DATE;
        boolean reverse = body.get(Field.REVERSE) != null && Boolean.parseBoolean(body.get(Field.REVERSE));
        String page = body.get(Field.PAGE);
        String limit = body.get(Field.LIMIT);
        String offset = body.get(Field.OFFSET);
        get(user, id, structureId, startAt, endAt, studentIds, groupIds, typeIds, processStates,
                isStudent, order, reverse, page, limit, offset, handler);
    }

    @Override
    public void get(UserInfos user, String id, String structureId, String startAt, String endAt, List<String> studentIds, List<String> groupIds,
                    List<String> typeIds, List<String> processStates, boolean isStudent, String order, boolean reverse, String pageString, String limitString, String offsetString,
                    Handler<AsyncResult<JsonObject>> handler) {
        get(user, id, null, structureId, startAt, endAt, studentIds, groupIds, typeIds, processStates, isStudent, order, reverse, pageString,
                limitString, offsetString)
                .onFailure(error -> handler.handle(Future.failedFuture(error)))
                .onSuccess(result -> handler.handle(Future.succeededFuture(result)));
    }

    @Override
    public Future<JsonObject> get(UserInfos user, String id, String groupedPunishmentId, String structureId,
                                  String startAt, String endAt, List<String> studentIds, List<String> groupIds,
                                  List<String> typeIds, List<String> processStates, boolean isStudent, String order, boolean reverse,
                                  String pageString, String limitString, String offsetString) {
        Promise<JsonObject> promise = Promise.promise();
        punishmentHelper.getQuery(user, id, groupedPunishmentId, structureId, startAt, endAt, studentIds, groupIds,
                typeIds, processStates, isStudent, queryResult -> {
            if (queryResult.failed()) {
                String message = String.format("[Incidents@%s::getPunishments] Failed to get query", this.getClass().getSimpleName());
                LOGGER.error(String.format("%s %s", message, queryResult.cause().getMessage()));
                promise.fail(message);
                return;
            }

            if (id != null && !id.equals("")) {
                punishmentHelper.getPunishment(punishment.getTable(), order, reverse, queryResult.result())
                        .onSuccess(promise::complete)
                        .onFailure(promise::fail);
            } else {
                Future<Long> countFuture = Future.future();
                Future<JsonArray> findFuture = Future.future();

                /* PAGINATE QUERY PUNISHMENTS */
                Integer limit, offset, page = null;
                int aPageNumber = Incidents.PAGE_SIZE;

                if (pageString != null && !pageString.equals("")) {
                    page = Integer.parseInt(pageString);
                    offset = page * aPageNumber;
                    limit = aPageNumber;
                } else {
                    offset = offsetString != null && !offsetString.equals("") ? Integer.parseInt(offsetString) : 0;
                    limit = limitString != null && !limitString.equals("") ? Integer.parseInt(limitString) : -1;
                }

                punishmentHelper.getPunishments(punishment.getTable(), queryResult.result(), order, reverse, limit, offset, result -> {
                    if (result.failed()) {
                        findFuture.fail(result.cause());
                        return;
                    }
                    findFuture.complete(result.result());
                });

                punishmentHelper.countPunishments(punishment.getTable(), queryResult.result(), result -> {
                    if (result.failed()) {
                        countFuture.fail(result.cause());
                        return;
                    }
                    countFuture.complete(result.result());
                });

                Integer finalPage = page;
                CompositeFuture.all(countFuture, findFuture).setHandler(resultFuture -> {
                    if (resultFuture.failed()) {
                        String message = String.format("[Incidents@%s::specifyCategoryFromType] fail to get punishments."
                                , this.getClass().getSimpleName());
                        LOGGER.error(String.format("%s %s", message, resultFuture.cause().getMessage()));
                        promise.fail(resultFuture.cause());
                    } else {
                        formatPunishmentsResult(countFuture.result(), findFuture.result(), finalPage, limit, offset, promise);
                    }
                });
            }
        });

        return promise.future();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void getPunishmentByStudents(String structure, String startAt, String endAt, List<String> students,
                                        List<Integer> typeIds, String eventType, Boolean processed, Boolean massmailed,
                                        Handler<Either<String, JsonArray>> handler) {
        punishmentType.get(structure, typesResult -> {
            if (typesResult.isLeft()) {
                LOGGER.info("[Incidents@DefaultPunishmentService::getPunishmentByStudents] Failed to " +
                        "fetch punishment types", typesResult.left().getValue());
                handler.handle(new Either.Left<>(typesResult.left().getValue()));
                return;
            }

            List<PunishmentType> punishmentsTypes = PunishmentTypeHelper.
                    getPunishmentTypeListFromJsonArray(typesResult.right().getValue());

            List<Integer> filteredTypeIds = typeIds;
            if (eventType != null) {
                punishmentsTypes = punishmentsTypes.stream().filter(pType -> pType.getType().equals(eventType))
                        .collect(Collectors.toList());

                List<Integer> punishmentsTypeIds = punishmentsTypes.stream()
                        .map(PunishmentType::getId)
                        .collect(Collectors.toList());

                filteredTypeIds = typeIds == null ? punishmentsTypeIds : typeIds.stream()
                        .filter(punishmentsTypeIds::contains)
                        .collect(Collectors.toList());
            }


            // for some reason, we still manage to find some "duplicate" data so we use mergeFunction (see collectors.toMap)
            Map<Integer, PunishmentType> punishmentTypeMap = punishmentsTypes
                    .stream()
                    .collect(Collectors.toMap(PunishmentType::getId, PunishmentType::clone, (punishmentType1, punishmentType2) -> punishmentType1));
            punishmentHelper.getPunishmentsCommand(punishment.getTable(), getPunishmentByStudentQueryPipeline(structure, startAt, endAt,
                    students, filteredTypeIds, processed, massmailed), punishmentsResult -> {
                if (punishmentsResult.failed()) {
                    LOGGER.info("[Incidents@DefaultPunishmentService::getPunishmentByStudents] Failed to " +
                            "fetch punishment by student: ", punishmentsResult.cause());
                    handler.handle(new Either.Left<>(punishmentsResult.cause().getMessage()));
                    return;
                }
                List<JsonObject> punishments = punishmentsResult.result().getList();

                List<String> ownerIds = new ArrayList<>();
                setOwnerIdsFromPunishment(punishments, ownerIds);
                userService.getUsers(ownerIds, userAsync -> {
                    if (userAsync.isLeft()) {
                        LOGGER.info("[Incidents@DefaultPunishmentService::getPunishmentByStudents] Failed to " +
                                "fetch owner info: ", userAsync.left().getValue());
                        handler.handle(new Either.Left<>(userAsync.left().getValue()));
                    } else {
                        setPunishmentInfo(punishments, punishmentTypeMap, userAsync);
                        handler.handle(new Either.Right<>(new JsonArray(punishments)));
                    }
                });

            });
        });
    }

    @SuppressWarnings("unchecked")
    private void setOwnerIdsFromPunishment(List<JsonObject> punishments, List<String> ownerIds) {
        punishments.forEach(punishmentStudent ->
                ((List<JsonObject>) punishmentStudent.getJsonArray("punishments").getList()).forEach(punishment -> {
                    if (!ownerIds.contains(punishment.getString("owner_id"))) {
                        ownerIds.add(punishment.getString("owner_id"));
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    private void setPunishmentInfo(List<JsonObject> punishments, Map<Integer, PunishmentType> punishmentTypeMap,
                                   Either<String, JsonArray> userAsync) {
        Map<String, JsonObject> ownerMap = ((List<JsonObject>) userAsync.right().getValue().getList())
                .stream()
                .collect(Collectors.toMap(user -> user.getString("id"), Function.identity(), (owner1, owner2) -> owner1));

        // remove _id
        punishments.forEach(punishmentStudent -> {
            punishmentStudent.remove("_id");
            punishmentStudent.put("type", determinePunishmentType(punishmentStudent, punishmentTypeMap));
            ((List<JsonObject>) punishmentStudent.getJsonArray("punishments").getList()).forEach(punishment -> {
                punishment.put("type", punishmentTypeMap.get(punishment.getInteger("type_id")).toJsonObject());
                punishment.put("owner", ownerMap.getOrDefault(punishment.getString("owner_id"), new JsonObject()));
            });
        });
    }

    /**
     * determinePunishmentType
     * <p>
     * We first manage to find punishment type ('PUNISHMENT'),
     * during the loop, as long as we find punishment , we will return 'PUNISHMENT' string (even if we find PUNISHMENT and SANCTION at same time)
     * If we haven't found one punishment, we can conclude on a sanction type ONLY
     *
     * @param punishmentStudent JsonObject each punishment
     * @param punishmentTypeMap Map with punishmentType id as key and Object punishment Type
     */
    @SuppressWarnings("unchecked")
    private String determinePunishmentType(JsonObject punishmentStudent, Map<Integer, PunishmentType> punishmentTypeMap) {
        boolean findPunishment = false;
        for (JsonObject punishment : ((List<JsonObject>) punishmentStudent.getJsonArray("punishments").getList())) {
            Integer punishmentType = punishment.getInteger("type_id");
            if (punishmentTypeMap.containsKey(punishmentType) && punishmentTypeMap.get(punishmentType).getType().equals(EventType.PUNISHMENT.toString())) {
                findPunishment = true;
            }
        }
        return findPunishment ? EventType.PUNISHMENT.name() : EventType.SANCTION.name();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void getPunishmentCountByStudent(String structure, String startAt, String endAt, List<String> students,
                                            List<Integer> typeIds, Boolean processed, Boolean massmailed,
                                            Handler<Either<String, JsonArray>> handler) {
        punishmentHelper.getPunishmentsCommand(punishment.getTable(), getPunishmentCountByStudentQueryPipeline(structure, startAt,
                endAt, students, typeIds, processed, massmailed), punishmentEvent -> {
            if (punishmentEvent.failed()) {
                LOGGER.info("[Incidents@DefaultPunishmentService::getPunishmentCountByStudent] Failed " +
                        "to count punishment by student: ", punishmentEvent.cause());
                handler.handle(new Either.Left<>(punishmentEvent.cause().getMessage()));
            } else {
                // remove _id
                ((List<JsonObject>) punishmentEvent.result().getList()).forEach(punishment -> punishment.remove("_id"));
                handler.handle(new Either.Right<>(punishmentEvent.result()));
            }
        });
    }

    private JsonArray getPunishmentByStudentQueryPipeline(String structure, String startAt, String endAt, List<String> students,
                                                          List<Integer> typeIds, Boolean processed, Boolean massmailed) {
        JsonObject $queryMatch = getQueryMatchPunishmentByStudent(structure, startAt, endAt, students, typeIds, processed, massmailed);
        return new JsonArray()
                .add(new JsonObject().put("$match", $queryMatch))
                .add(new JsonObject().put("$group",
                                new JsonObject()
                                        .put("_id", "$student_id")
                                        .put("punishments", new JsonObject()
                                                .put("$push", new JsonObject()
                                                        .put("id", "$_id")
                                                        .put("created_at", "$created_at")
                                                        .put("type_id", "$type_id")
                                                        .put("owner_id", "$owner_id")
                                                        .put("description", "$description")
                                                        .put(Field.GROUPED_PUNISHMENT_ID, "$grouped_punishment_id")
                                                        .put("fields", "$fields")
                                                )
                                        )
                        )
                )
                .add(new JsonObject().put("$project", new JsonObject()
                        .put("_id", 1)
                        .put("student_id", "$_id")
                        .put("punishments", 1))
                );
    }

    private JsonArray getPunishmentCountByStudentQueryPipeline(String structure, String startAt, String endAt, List<String> students,
                                                               List<Integer> typeIds, Boolean processed, Boolean massmailed) {
        JsonObject $queryMatch = getQueryMatchPunishmentByStudent(structure, startAt, endAt, students,
                typeIds, processed, massmailed);

        return new JsonArray()
                .add(new JsonObject().put("$match", $queryMatch))
                .add(addCountIdField())
                .add(new JsonObject().put(Field.$GROUP,
                                new JsonObject().put(Field._ID, new JsonObject()
                                        .put(Field.STUDENT_ID, "$student_id")
                                        .put(COUNTID, $COUNTID)
                                )
                        )
                )
                .add(new JsonObject().put("$group",
                                new JsonObject()
                                        .put("_id", String.format("%s.%s", Field.$_ID, Field.STUDENT_ID))
                                        .put("count", new JsonObject().put("$sum", 1))
                        )
                )
                .add(new JsonObject().put("$project", new JsonObject()
                        .put("_id", 1)
                        .put("student_id", "$_id")
                        .put("count", 1))
                );
    }

    private JsonObject getQueryMatchPunishmentByStudent(String structure, String startAt, String endAt, List<String> students,
                                                        List<Integer> typeIds, Boolean processed, Boolean massmailed) {
        JsonObject $queryMatch = new JsonObject()
                .put("structure_id", structure);

        JsonArray matchingDates = punishmentHelper.getPunishmentMatchingDate(startAt, endAt);
        if (matchingDates != null && !matchingDates.isEmpty()) $queryMatch.put("$or", matchingDates);

        if (students != null && !students.isEmpty()) {
            $queryMatch.put("student_id", new JsonObject().put("$in", new JsonArray(students)));
        }

        if (typeIds != null && !typeIds.isEmpty()) {
            $queryMatch.put("type_id", new JsonObject().put("$in", new JsonArray(typeIds)));
        }

        if (processed != null) {
            $queryMatch.put("processed", processed);
        }

        if (massmailed != null) {
            $queryMatch.put("massmailed", massmailed ? massmailed : new JsonObject().put("$in", new JsonArray().addNull().add(false)));
        }

        return $queryMatch;
    }

    private JsonObject addCountIdField() {
        JsonObject groupedPunishmentIdExistsQuery = new JsonObject().put(Field.$GT,
                new JsonArray().add(Field.$GROUPED_PUNISHMENT_ID).addNull()
        );

        JsonObject cond = new JsonObject()
                .put(Field.$COND, new JsonArray()
                        .add(groupedPunishmentIdExistsQuery)
                        .add(Field.$GROUPED_PUNISHMENT_ID)
                        .add(Field.$_ID)
                );

        return new JsonObject().put(Field.$ADDFIELDS, new JsonObject().put(COUNTID, cond));
    }

    private void formatPunishmentsResult(Long punishmentsNumber, JsonArray punishments,
                                         Integer page, Integer limit, Integer offset, Handler<AsyncResult<JsonObject>> handler) {
        JsonObject finalResult = new JsonObject();
        if (page != null) {
            Long pageCount = punishmentsNumber <= Incidents.PAGE_SIZE ?
                    0 : (long) Math.ceil(punishmentsNumber / (double) Incidents.PAGE_SIZE);
            finalResult
                    .put("page", page)
                    .put("page_count", pageCount);
        } else {
            finalResult
                    .put("limit", limit)
                    .put("offset", offset);
        }

        finalResult.put("all", punishments);
        handler.handle(Future.succeededFuture(finalResult));
    }

    @Override
    public void updatePunishmentMassmailing(List<String> punishmentsIds, Boolean isMassmailed, Handler<Either<String, JsonObject>> handler) {

        JsonObject selectIds = new JsonObject()
                .put("_id", new JsonObject().put("$in", new JsonArray(punishmentsIds)));

        JsonObject updatedPunishment = new JsonObject().put("$set", new JsonObject().put("massmailed", true));

        MongoDb.getInstance().update(punishment.getTable(), selectIds, updatedPunishment, MongoDbResult.validResultHandler(handler));
    }

    @Override
    public Future<JsonObject> delete(UserInfos user, String structureId, String punishmentId, String groupedPunishmentId) {
        Promise<JsonObject> promise = Promise.promise();
        get(user, punishmentId, groupedPunishmentId, structureId, null, null, null,
                null, null, null, false, null, false, null, null, null)
                .onFailure(error -> {
                    String message = String.format("[Incidents@%s::delete] Fail to delete punishments"
                            , this.getClass().getSimpleName());
                    LOGGER.error(String.format("%s %s", message, error.getMessage()));
                    promise.fail(message);
                })
                .onSuccess(punishmentResult -> {
                    List<Punishment> deletePunishments;
                    LOGGER.info("success");
                    if (punishmentId != null) {
                        Punishment deletePunishment = new Punishment();
                        deletePunishment.setFromJson(punishmentResult);
                        deletePunishments = Collections.singletonList(deletePunishment);
                    } else
                        deletePunishments = PunishmentHelper
                                .getCollectivePunishmentListFromJsonArray(punishmentResult.getJsonArray(Field.ALL, new JsonArray()));

                    delete(structureId, deletePunishments, promise);
                });
        return promise.future();
    }

    public void delete(String structureId, List<Punishment> deletePunishments, Handler<AsyncResult<JsonObject>> handler) {
        LOGGER.info("delete");
        List<String> studentIds = deletePunishments.stream()
                .map(Punishment::getStudentId)
                .collect(Collectors.toList());

        List<String> punishmentIds = deletePunishments.stream()
                .map(Punishment::getId)
                .collect(Collectors.toList());

        Future<JsonObject> punishmentFuture = deletePunishment(punishmentIds);
        Future<JsonObject> absenceFuture = deleteRelatedAbsence(deletePunishments, structureId);

        FutureHelper.all(Arrays.asList(punishmentFuture, absenceFuture)).setHandler(event -> {
            if (event.failed()) {
                handler.handle(Future.failedFuture(event.cause().toString()));
                return;
            }
            StatisticsPresences.getInstance().postUsers(structureId, studentIds);
            handler.handle(Future.succeededFuture(punishmentFuture.result()));
        });
    }

    private Future<JsonObject> deletePunishment(List<String> ids) {
        Promise<JsonObject> promise = Promise.promise();
        JsonObject query = new JsonObject()
                .put(Field._ID, new JsonObject().put(Field.$IN, ids));

        MongoDb.getInstance().delete(punishment.getTable(), query, MongoDbResult.validResultHandler(FutureHelper.handlerJsonObject(result -> {
            if (result.failed()) {
                String message = String.format("[Incidents@%s::deletePunishment] Fail to delete punishments"
                        , this.getClass().getSimpleName());
                LOGGER.error(String.format("%s %s", message, result.cause().getMessage()));
                promise.fail(message);
            } else {
                promise.complete(result.result());
            }
        })));
        return promise.future();
    }

    private Future<JsonObject> deleteRelatedAbsence(List<Punishment> deletePunishments, String structureId) {
        Promise<JsonObject> promise = Promise.promise();
        if (deletePunishments.isEmpty()) {
            promise.complete(new JsonObject());
            return promise.future();
        }
        Punishment deletePunishment = deletePunishments.get(0);

        PunishmentCategory.getSpecifiedCategoryFromType(structureId, deletePunishment.getTypeId())
                .onFailure(promise::fail)
                .onSuccess(categoryResult -> {
                    if (!PunishmentCategory.EXCLUDE.equals(categoryResult.getLabel())) {
                        promise.complete(new JsonObject());
                        return;
                    }

                    ExcludeCategory category = new ExcludeCategory();
                    category.setFromJson(deletePunishment.getFields());
                    category.formatDates();

                    getStudentAbsences(deletePunishment.getStudentId(), category.getStartAt(), category.getEndAt()).setHandler(absencesResult -> {
                        if (absencesResult.failed()) {
                            promise.fail(absencesResult.cause().getMessage());
                            return;
                        }

                        List<JsonObject> absences = absencesResult.result();
                        if (absences.size() == 1 && areAbsenceDatesCorresponding(absences.get(0), category.getStartAt(), category.getEndAt())) {
                            Presences.getInstance()
                                    .deleteAbsence(absences.get(0).getLong(Field.ID), promise);
                            return;
                        }
                        promise.complete(new JsonObject());
                    });
                });
        return promise.future();
    }


    @Override
    @SuppressWarnings("unchecked")
    public void getAbsencesByStudentIds(List<String> studentIds, String starDate, String endDate, Handler<AsyncResult<Map<String, List<JsonObject>>>> handler) {
        if (studentIds.isEmpty()) {
            handler.handle(Future.succeededFuture(new HashMap<>()));
            return;
        }
        Presences.getInstance().getAbsences(studentIds, starDate, endDate, result -> {
            if (result.failed()) {
                String message = "[Incidents@DefaultPunishmentService::getAbsencesByStudentIds] failed to retrieve absences";
                LOGGER.error(message, result.cause().getMessage());
                handler.handle(Future.failedFuture(message));
                return;
            }

            List<JsonObject> absences = result.result().getList();
            Map<String, List<JsonObject>> absencesByStudentIds = studentIds.stream().collect(Collectors.toMap(
                    studentId -> studentId,
                    studentId -> absences.stream().filter(absence -> absence.getString("student_id").equals(studentId)).collect(Collectors.toList())
            ));

            handler.handle(Future.succeededFuture(absencesByStudentIds));
        });
    }
}
