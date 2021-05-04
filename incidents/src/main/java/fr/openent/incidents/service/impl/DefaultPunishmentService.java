package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.helper.PunishmentHelper;
import fr.openent.incidents.helper.PunishmentTypeHelper;
import fr.openent.incidents.model.Punishment;
import fr.openent.incidents.model.PunishmentType;
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
    private final PunishmentHelper punishmentHelper;
    private final Punishment punishment = new Punishment();
    private final PunishmentTypeService punishmentType = new DefaultPunishmentTypeService();
    private final UserService userService = new DefaultUserService();


    public DefaultPunishmentService(EventBus eb) {
        this.punishmentHelper = new PunishmentHelper(eb);
    }

    @Override
    public void create(UserInfos user, JsonObject body, Handler<AsyncResult<JsonArray>> handler) {
        Punishment createPunishment = new Punishment();
        createPunishment.setFromJson(body);

        Future<JsonObject> punishmentsFuture = Future.future();
        Future<JsonObject> absencesFuture = Future.future();
        createPunishments(user, body, punishmentsFuture);
        createRelatedAbsences(user, body, createPunishment, absencesFuture);

        FutureHelper.all(Arrays.asList(punishmentsFuture, absencesFuture)).setHandler(event -> {
            if (event.failed()) {
                handler.handle(Future.failedFuture(event.cause().toString()));
                return;
            }

            handler.handle(Future.succeededFuture(punishmentsFuture.result().getJsonArray("all", new JsonArray())));
        });
    }

    @SuppressWarnings("unchecked")
    private void createPunishments(UserInfos user, JsonObject body, Handler<AsyncResult<JsonObject>> handler) {
        List<String> studentIds = body.getJsonArray("student_ids").getList();
        JsonArray results = new JsonArray();
        List<Future> futures = new ArrayList<>();

        for (String studentId : studentIds) {
            Future future = Future.future();
            futures.add(future);
            Punishment createPunishment = new Punishment();
            createPunishment.setFromJson(body);
            createPunishment.setStudentId(studentId);

            createPunishment.persistMongo(user, result -> {
                if (result.failed()) {
                    future.fail(result.cause().getMessage());
                } else {
                    results.add(result.result());
                    future.complete();
                }
            });
        }

        CompositeFuture.join(futures).setHandler(event -> {
            if (event.failed()) {
                handler.handle(Future.failedFuture(event.cause().toString()));
                return;
            }

            StatisticsPresences.getInstance().postUsers(body.getString("structure_id"), studentIds, Future.future());
            handler.handle(Future.succeededFuture(new JsonObject().put("all", results)));
        });
    }

    private void createRelatedAbsences(UserInfos user, JsonObject body, Punishment punishment, Handler<AsyncResult<JsonObject>> handler) {
        PunishmentCategory.getCategoryLabelFromType(punishment.getTypeId(), punishment.getStructureId(), label -> {
            if (!label.result().equals(PunishmentCategory.EXCLUDE) || !body.containsKey("absence")) {
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
                        .createAbsences(punishment.getStructureId(), studentIds, user.getUserId(), reasonId, startAt,
                                endAt, true, followed, handler);
            });
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
    public void update(UserInfos user, JsonObject body, Handler<AsyncResult<JsonObject>> handler) {
        Punishment updatePunishment = new Punishment();
        updatePunishment.setFromJson(body);
        get(user, updatePunishment.getId(), updatePunishment.getStructureId(), null, null,
                null, null, null, null, false, null, null, null, punishmentResult -> {
                    if (punishmentResult.failed()) {
                        handler.handle(Future.failedFuture(punishmentResult.cause().getMessage()));
                    }

                    String studentId = punishmentResult.result().getJsonObject("student", new JsonObject()).getString("id");
                    Punishment oldPunishment = new Punishment();
                    oldPunishment.setFromJson(punishmentResult.result());

                    update(user, body, updatePunishment, oldPunishment, studentId, handler);
                });
    }

    public void update(UserInfos user, JsonObject body, Punishment updatePunishment, Punishment oldPunishment, String studentId,
                       Handler<AsyncResult<JsonObject>> handler) {
        Future<JsonObject> punishmentFuture = Future.future();
        Future<JsonObject> absencesFuture = Future.future();

        updatePunishment(user, updatePunishment, punishmentFuture);
        updateRelatedAbsence(user, body, studentId, oldPunishment, updatePunishment, absencesFuture);

        FutureHelper.all(Arrays.asList(punishmentFuture, absencesFuture)).setHandler(event -> {
            if (event.failed()) {
                handler.handle(Future.failedFuture(event.cause().toString()));
                return;
            }
            StatisticsPresences.getInstance().postUsers(updatePunishment.getStructureId(), Collections.singletonList(studentId), Future.future());
            handler.handle(Future.succeededFuture(punishmentFuture.result()));
        });
    }

    private void updatePunishment(UserInfos user, Punishment updatePunishment, Handler<AsyncResult<JsonObject>> handler) {
        updatePunishment.persistMongo(user, result -> {
            if (result.failed()) {
                handler.handle(Future.failedFuture(result.cause().getMessage()));
                return;
            }
            handler.handle(Future.succeededFuture(result.result()));
        });
    }

    private void updateRelatedAbsence(UserInfos user, JsonObject body, String studentId, Punishment oldPunishment,
                                      Punishment updatePunishment, Handler<AsyncResult<JsonObject>> handler) {
        if (!body.containsKey("absence")) {
            handler.handle(Future.succeededFuture(new JsonObject()));
            return;
        }

        getCategoryLabel(updatePunishment, labelResult -> {
            if (labelResult.failed()) {
                handler.handle(Future.failedFuture(labelResult.cause().getMessage()));
                return;
            }

            String label = labelResult.result().getString("label", "");
            if (!label.equals(PunishmentCategory.EXCLUDE)) {
                handler.handle(Future.succeededFuture(new JsonObject()));
                return;
            }

            ExcludeCategory oldCategory = new ExcludeCategory();
            oldCategory.setFromJson(oldPunishment.getFields());
            oldCategory.formatDates();

            ExcludeCategory category = new ExcludeCategory();
            category.setFromJson(updatePunishment.getFields());
            category.formatDates();

            Long reasonId = body.getJsonObject("absence").getLong("reason_id");

            getStudentAbsences(studentId, oldCategory.getStartAt(), oldCategory.getEndAt()).setHandler(absencesResult -> {
                if (absencesResult.failed()) {
                    handler.handle(Future.failedFuture(absencesResult.cause().getMessage()));
                    return;
                }

                List<JsonObject> absences = absencesResult.result();
                if (absences.isEmpty()) {
                    Presences.getInstance()
                            .createAbsences(updatePunishment.getStructureId(), Collections.singletonList(studentId),
                                    user.getUserId(), reasonId, category.getStartAt(), category.getEndAt(), true, true, handler);
                } else if (absences.size() == 1 && areAbsenceDatesCorresponding(absences.get(0), oldCategory.getStartAt(), oldCategory.getEndAt())) {
                    Presences.getInstance()
                            .updateAbsence(updatePunishment.getStructureId(), absences.get(0).getLong("id"),
                                    user.getUserId(), reasonId, studentId, category.getStartAt(), category.getEndAt(), true, handler);
                } else {
                    handler.handle(Future.succeededFuture(new JsonObject()));
                }
            });

        });
    }

    private void getCategoryLabel(Punishment punishment, Handler<AsyncResult<JsonObject>> handler) {
        PunishmentCategory.getCategoryLabelFromType(punishment.getTypeId(), punishment.getStructureId(), label -> {
            if (label.failed()) {
                handler.handle(Future.failedFuture(label.cause().getMessage()));
                return;
            }
            handler.handle(Future.succeededFuture(new JsonObject().put("label", label.result())));
        });
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
        String id = body.get("id");
        String structureId = body.get("structure_id");
        String startAt = body.get("start_at");
        String endAt = body.get("end_at");
        List<String> studentIds = body.getAll("student_id");
        List<String> groupIds = body.getAll("group_id");
        List<String> typeIds = body.getAll("type_id");
        List<String> processStates = body.getAll("process");
        String page = body.get("page");
        String limit = body.get("limit");
        String offset = body.get("offset");
        get(user, id, structureId, startAt, endAt, studentIds, groupIds, typeIds, processStates, isStudent, page, limit, offset, handler);
    }

    @Override
    public void get(UserInfos user, String id, String structureId, String startAt, String endAt, List<String> studentIds, List<String> groupIds,
                    List<String> typeIds, List<String> processStates, boolean isStudent, String pageString, String limitString, String offsetString,
                    Handler<AsyncResult<JsonObject>> handler) {
        punishmentHelper.getQuery(user, id, structureId, startAt, endAt, studentIds, groupIds, typeIds, processStates, isStudent, queryResult -> {
            if (queryResult.failed()) {
                handler.handle(Future.failedFuture(queryResult.cause().getMessage()));
                return;
            }

            if (id != null && !id.equals("")) {
                punishmentHelper.getPunishment(punishment.getTable(), queryResult.result(), handler);
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

                punishmentHelper.getPunishments(punishment.getTable(), queryResult.result(), limit, offset, result -> {
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
                        handler.handle(Future.failedFuture(resultFuture.cause()));
                    } else {
                        formatPunishmentsResult(countFuture.result(), findFuture.result(), finalPage, limit, offset, handler);
                    }
                });
            }
        });
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
                .add(new JsonObject().put("$group",
                        new JsonObject()
                                .put("_id", "$student_id")
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
    public void count(UserInfos user, MultiMap body, boolean isStudent, Handler<AsyncResult<Long>> handler) {
        punishmentHelper.getQuery(user, body, isStudent, result -> {
            if (result.failed()) {
                handler.handle(Future.failedFuture(result.cause().getMessage()));
                return;
            }
            punishmentHelper.countPunishments(punishment.getTable(), result.result(), handler);
        });
    }

    @Override
    public void delete(UserInfos user, MultiMap body, Handler<AsyncResult<JsonObject>> handler) {
        String structureId = body.get("structureId");
        get(user, body.get("id"), structureId, null, null,
                null, null, null, null, false, null, null, null, punishmentResult -> {
                    if (punishmentResult.failed()) {
                        handler.handle(Future.failedFuture(punishmentResult.cause().getMessage()));
                    }

                    Punishment deletePunishment = new Punishment();
                    deletePunishment.setFromJson(punishmentResult.result());
                    String studentId = punishmentResult.result().getJsonObject("student", new JsonObject()).getString("id");
                    delete(body, deletePunishment, studentId, result -> {
                        StatisticsPresences.getInstance().postUsers(structureId, Collections.singletonList(studentId), Future.future());
                        handler.handle(result);
                    });
                });
    }

    public void delete(MultiMap body, Punishment deletePunishment, String studentId, Handler<AsyncResult<JsonObject>> handler) {
        Future<JsonObject> punishmentFuture = Future.future();
        Future<JsonObject> absenceFuture = Future.future();

        deletePunishment(body, punishmentFuture);
        deleteRelatedAbsence(deletePunishment, studentId, absenceFuture);
        FutureHelper.all(Arrays.asList(punishmentFuture, absenceFuture)).setHandler(event -> {
            if (event.failed()) {
                handler.handle(Future.failedFuture(event.cause().toString()));
                return;
            }
            handler.handle(Future.succeededFuture(punishmentFuture.result()));
        });
    }

    private void deletePunishment(MultiMap body, Handler<AsyncResult<JsonObject>> handler) {
        JsonObject query = new JsonObject()
                .put("_id", body.get("id"));
        MongoDb.getInstance().delete(punishment.getTable(), query, message -> {
            Either<String, JsonObject> messageCheck = MongoDbResult.validResult(message);
            if (messageCheck.isLeft()) {
                handler.handle(Future.failedFuture("[Incidents@Punishment::deletePunishment] Failed to delete punishment"));
            } else {
                JsonObject result = messageCheck.right().getValue();
                handler.handle(Future.succeededFuture(result));
            }
        });
    }

    private void deleteRelatedAbsence(Punishment deletePunishment, String studentId, Handler<AsyncResult<JsonObject>> handler) {
        getCategoryLabel(deletePunishment, labelResult -> {
            if (labelResult.failed()) {
                handler.handle(Future.failedFuture(labelResult.cause().getMessage()));
                return;
            }

            String label = labelResult.result().getString("label", "");
            if (!label.equals(PunishmentCategory.EXCLUDE)) {
                handler.handle(Future.succeededFuture(new JsonObject()));
                return;
            }

            ExcludeCategory category = new ExcludeCategory();
            category.setFromJson(deletePunishment.getFields());
            category.formatDates();

            getStudentAbsences(studentId, category.getStartAt(), category.getEndAt()).setHandler(absencesResult -> {
                if (absencesResult.failed()) {
                    handler.handle(Future.failedFuture(absencesResult.cause().getMessage()));
                    return;
                }

                List<JsonObject> absences = absencesResult.result();
                if (absences.size() == 1 && areAbsenceDatesCorresponding(absences.get(0), category.getStartAt(), category.getEndAt())) {
                    Presences.getInstance()
                            .deleteAbsence(absences.get(0).getLong("id"), handler);
                    return;
                }
                handler.handle(Future.succeededFuture(new JsonObject()));
            });

        });
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
