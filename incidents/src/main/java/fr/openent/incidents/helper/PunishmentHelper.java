package fr.openent.incidents.helper;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.enums.PunishmentsProcessState;
import fr.openent.incidents.enums.WorkflowActions;
import fr.openent.incidents.model.Punishment;
import fr.openent.presences.common.helper.*;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.core.constants.Field;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PunishmentHelper {
    private final GroupService groupService;
    private final UserService userService;

    private static final Logger log = LoggerFactory.getLogger(PunishmentHelper.class);

    public PunishmentHelper(EventBus eb) {
        this.groupService = new DefaultGroupService(eb);
        this.userService = new DefaultUserService();
    }

    @SuppressWarnings("unchecked")
    public static List<Punishment> getCollectivePunishmentListFromJsonArray(JsonArray punishments) {
        return ((List<JsonObject>) punishments.getList()).stream()
                .map(oPunishment -> {
                    Punishment punishment = new Punishment();
                    punishment.setFromJson(oPunishment);
                    return punishment;
                })
                .collect(Collectors.toList());
    }

    public void getQuery(UserInfos user, String id, String groupedPunishmentId, String structureId, String startAt, String endAt, List<String> studentIds,
                         List<String> groupIds, List<String> typeIds, List<String> processStates, boolean isStudent,
                         Handler<AsyncResult<JsonObject>> handler) {
        JsonObject query = new JsonObject().put("structure_id", structureId);

        if (!isStudent && user != null && (!WorkflowHelper.hasRight(user, WorkflowActions.PUNISHMENTS_VIEW.toString()) || !WorkflowHelper.hasRight(user, WorkflowActions.SANCTIONS_VIEW.toString()))) {
            query.put("owner_id", user.getUserId());
        }

        if (isStudent) query.put("student_id", user.getUserId());

        if (id != null && !id.equals("")) {
            query.put("_id", id);
            handler.handle(Future.succeededFuture(query));
        } else if (groupedPunishmentId != null && !groupedPunishmentId.equals("")) {
            query.put(Field.GROUPED_PUNISHMENT_ID, groupedPunishmentId);
            handler.handle(Future.succeededFuture(query));
        } else {
            getManyPunishmentsQuery(query, startAt, endAt, studentIds, groupIds, typeIds, processStates, isStudent, handler);
        }
    }

    private void getManyPunishmentsQuery(JsonObject query, String startAt, String endAt, List<String> studentIds, List<String> groupIds,
                                         List<String> typeIds, List<String> processStates, boolean isStudent,
                                         Handler<AsyncResult<JsonObject>> handler) {
        this.groupService.getGroupStudents(groupIds, groupResult -> {
            if (groupResult.isLeft()) {
                handler.handle(Future.failedFuture("[Incidents@Punishment::getQuery] Failed to retrieve students from groups"));
                return;
            }

            if (startAt != null && endAt != null) {

                //Check date for detentions and exclusions
                JsonArray containDateQueries = new JsonArray(Arrays.asList(
                        new JsonObject().put("fields.start_at", new JsonObject().put("$lte", endAt)),
                        new JsonObject().put("fields.end_at", new JsonObject().put("$gte", startAt))
                ));
                JsonObject containDateQuery = new JsonObject().put("$and", containDateQueries);

                //Check date for duties
                JsonArray containDateDutyQueries = new JsonArray(Arrays.asList(
                        new JsonObject().put("fields.delay_at", new JsonObject().put("$lte", endAt)),
                        new JsonObject().put("fields.delay_at", new JsonObject().put("$gte", startAt))
                ));
                JsonObject containDutyDateQuery = new JsonObject().put("$and", containDateDutyQueries);

                //Check creation date
                JsonObject nullDateQuery = new JsonObject().put("created_at", new JsonObject().put("$gte", startAt).put("$lte", endAt));

                query.put("$or", new JsonArray(Arrays.asList(containDateQuery, nullDateQuery, containDutyDateQuery)));
            }

            if (!isStudent) {
                groupResult.right().getValue().forEach(oStudent -> {
                    JsonObject student = (JsonObject) oStudent;
                    studentIds.add(student.getString("id"));
                });

                if (studentIds != null && !studentIds.isEmpty())
                    query.put("student_id", new JsonObject().put("$in", new JsonArray(studentIds)));
            }

            if (typeIds != null && !typeIds.isEmpty()) {
                List<Long> listTypeIds = new ArrayList<>();
                typeIds.forEach(typeId -> listTypeIds.add(Long.parseLong(typeId)));
                query.put("type_id", new JsonObject().put("$in", new JsonArray(listTypeIds)));
            }


            if (processStates != null && !processStates.isEmpty()) {
                Boolean isProcessed = processStates.contains(PunishmentsProcessState.PROCESSED.toString());
                Boolean isNotProcessed = processStates.contains(PunishmentsProcessState.NOT_PROCESSED.toString());
                boolean isAllSelected = isProcessed && isNotProcessed;

                if (!isAllSelected) {
                    if (Boolean.TRUE.equals(isProcessed)) {
                        query.put("processed", true);
                    } else if (Boolean.TRUE.equals(isNotProcessed)) {
                        query.put("processed", false);
                    }
                }
            }

            handler.handle(Future.succeededFuture(query));
        });
    }

    /**
     * get JsonArray that will query the matching date
     *
     * @param startAt start date string
     * @param endAt   end date string
     */
    public JsonArray getPunishmentMatchingDate(String startAt, String endAt) {
        JsonObject dateChecks = new JsonObject();
        JsonArray startAndEndDateChecks = new JsonArray();
        if (startAt != null) {
            dateChecks.put("$gte", startAt);
            startAndEndDateChecks.add(new JsonObject().put("fields.end_at", new JsonObject().put("$gte", startAt)));
        }
        if (endAt != null) {
            dateChecks.put("$lte", endAt);
            startAndEndDateChecks.add(new JsonObject().put("fields.start_at", new JsonObject().put("$lte", endAt)));
        }

        // Check date for detentions and exclusions
        JsonObject containDateQuery = !startAndEndDateChecks.isEmpty() ? new JsonObject().put("$and", startAndEndDateChecks) : null;

        JsonObject containDutyDateQuery = null;
        JsonObject nullDateQuery = null;
        if (!dateChecks.isEmpty()) {
            // Check date for duties
            containDutyDateQuery = new JsonObject().put("fields.delay_at", dateChecks);
            // Check creation date
            nullDateQuery = new JsonObject().put("created_at", dateChecks);
        }
        List<JsonObject> listMatchingDates = Stream.of(containDateQuery, nullDateQuery, containDutyDateQuery)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new JsonArray(listMatchingDates);
    }

    /* REQUEST MONGODB PART */

    public Future<JsonObject> getPunishment(String tableName, String order, boolean reverse, JsonObject query) {
        Promise<JsonObject> promise = Promise.promise();
        MongoDb.getInstance().findOne(tableName, query, message -> {
            Either<String, JsonObject> messageCheck = MongoDbResult.validResult(message);
            if (messageCheck.isLeft()) {
                String messageError = String.format("[Incidents@%s::getPunishment] Failed to get punishment by id."
                        , this.getClass().getSimpleName());
                log.error(String.format("%s %s", messageError, messageCheck.left().getValue()));
                promise.fail(messageError);
            } else {
                JsonObject result = messageCheck.right().getValue();
                mapGetterResults(new JsonArray().add(result), order, reverse, mapResult -> {
                    if (mapResult.failed()) {
                        String messageError = String.format("[Incidents@%s::getPunishment] Failed to map punishment."
                                , this.getClass().getSimpleName());
                        log.error(String.format("%s %s", messageError, mapResult.cause().getMessage()));
                        promise.fail(messageError);
                    } else {
                        promise.complete(mapResult.result().getJsonObject(0));
                    }
                });
            }
        });
        return promise.future();
    }

    public void getPunishments(String tableName, JsonObject query, String order, boolean reverse, Integer limit, Integer offset, Handler<AsyncResult<JsonArray>> handler) {
        JsonObject command = new JsonObject()
                .put("aggregate", tableName)
                .put("allowDiskUse", true)
                .put("cursor", getCursor())
                .put("pipeline", getPipeline(query, order, reverse, limit, offset));

        MongoDb.getInstance().command(command.toString(), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error("[Incidents@PunishmentHelper::getPunishments] Failed to get punishments.", either.left().getValue());
                handler.handle(Future.failedFuture(either.left().getValue()));
                return;
            }
            JsonArray result = either.right().getValue().getJsonObject("cursor", new JsonObject()).getJsonArray("firstBatch", new JsonArray());
            mapGetterResults(result, order, reverse, mapResult -> {
                if (mapResult.failed()) {
                    log.error("[Incidents@PunishmentHelper::getPunishments] Failed to map punishments.", mapResult.cause());
                    handler.handle(Future.failedFuture(mapResult.cause()));
                } else {
                    handler.handle(Future.succeededFuture(mapResult.result()));
                }
            });
        }));
    }

    public void getPunishmentsCommand(String tableName, JsonArray query, Handler<AsyncResult<JsonArray>> handler) {
        JsonObject command = new JsonObject()
                .put("aggregate", tableName)
                .put("allowDiskUse", true)
                .put("cursor", getCursor())
                .put("pipeline", query);

        MongoDb.getInstance().command(command.toString(), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error("[Incidents@PunishmentHelper::getPunishmentsCommand] An error has occured while running command.", either.left().getValue());
                handler.handle(Future.failedFuture(either.left().getValue()));
                return;
            }
            JsonArray result = either.right().getValue().getJsonObject("cursor", new JsonObject()).getJsonArray("firstBatch", new JsonArray());
            handler.handle(Future.succeededFuture(result));
        }));
    }

    public void countPunishments(String tableName, JsonObject query, Handler<AsyncResult<Long>> handler) {
        MongoDb.getInstance().count(tableName, JsonObject.mapFrom(query), message -> {
            Either<String, JsonObject> messageCheck = MongoDbResult.validResult(message);
            if (messageCheck.isLeft()) {
                handler.handle(Future.failedFuture("[Incidents@PunishmentHelper::countPunishments] Failed to get count values."));
            } else {
                handler.handle(Future.succeededFuture(message.body().getLong("count")));
            }
        });
    }

    private JsonArray getPipeline(JsonObject query, String order, boolean reverse, Integer limit, Integer offset) {
        JsonArray pipeline = new JsonArray()
                .add(new JsonObject().put("$addFields",
                        addSortField()
                ))
                .add(new JsonObject().put("$match", query))
                .add(new JsonObject().put("$sort",
                        new JsonObject().put("sortField", (Objects.equals(order, Field.DATE) && reverse) ? 1 : -1)))
                .add(new JsonObject().put("$project", addProject()));

        pipeline.add(new JsonObject().put("$skip", offset));
        if (limit > 0) {
            pipeline.add(new JsonObject().put("$limit", limit));
        }

        return pipeline;
    }

    private JsonObject addSortField() {
        return new JsonObject()
                .put("sortField", new JsonObject().put(
                        "$switch", new JsonObject().put("branches", new JsonArray(Arrays.asList(
                                        getCase(
                                                new JsonObject().put("$gt", new JsonArray(Arrays.asList("$fields.delay_at", null))),
                                                getDate("$fields.delay_at")
                                        ),
                                        getCase(
                                                new JsonObject().put("$gt", new JsonArray(Arrays.asList("$fields.start_at", null))),
                                                getDate("$fields.start_at")
                                        ),
                                        getCase(
                                                new JsonObject().put("$gt", new JsonArray(Arrays.asList("$fields.end_at", null))),
                                                getDate("$fields.end_at")
                                        )
                                )))
                                .put("default", getDate("$created_at"))
                ));
    }

    private JsonObject getCase(JsonObject usingCase, JsonObject then) {
        return new JsonObject().put("case", usingCase).put("then", then);
    }

    private JsonObject getDate(String varDate) {
        return getDate(varDate, null);
    }

    private JsonObject getDate(String varDate, String format) {
        JsonObject result = new JsonObject().put("$dateFromString", new JsonObject().put("dateString", varDate));
        if (format != null) {
            result = new JsonObject().put("$dateToString", new JsonObject().put("format", format).put("date", result));
        }
        return result;
    }

    private JsonObject cond(JsonObject expression, JsonObject trueCase, String falseCase) {
        return new JsonObject().put("$cond", new JsonArray(Arrays.asList(
                expression,
                trueCase,
                falseCase
        )));
    }

    private JsonObject cond(JsonObject expression, String trueCase, String falseCase) {
        return new JsonObject().put("$cond", new JsonArray(Arrays.asList(
                expression,
                trueCase,
                falseCase
        )));
    }

    private JsonObject fieldIsNotNull(String field) {
        return new JsonObject().put("$gt", new JsonArray(Arrays.asList(
                field,
                null
        )));
    }

    private JsonObject addProject() {
        return new JsonObject()
                .put("id", "$_id")
                .put("description", 1)
                .put("processed", 1)
                .put("incident_id", 1)
                .put("type_id", 1)
                .put("owner_id", 1)
                .put("structure_id", 1)
                .put("student_id", 1)
                .put("grouped_punishment_id", 1)
                .put("fields.place", cond(fieldIsNotNull("$fields.place"), "$fields.place", "$$REMOVE"))
                .put("fields.instruction", cond(fieldIsNotNull("$fields.instruction"), "$fields.instruction", "$$REMOVE"))
                .put("fields.delay_at",
                        cond(
                                fieldIsNotNull("$fields.delay_at"),
                                getDate("$fields.delay_at", DateHelper.MONGO_FORMAT_TO_STRING_YMD_HMINS),
                                "$$REMOVE")
                )
                .put("fields.start_at",
                        cond(
                                fieldIsNotNull("$fields.start_at"),
                                getDate("$fields.start_at", DateHelper.MONGO_FORMAT_TO_STRING_YMD_HMINS),
                                "$$REMOVE")
                )
                .put("fields.end_at",
                        cond(
                                fieldIsNotNull("$fields.end_at"),
                                getDate("$fields.end_at", DateHelper.MONGO_FORMAT_TO_STRING_YMD_HMINS),
                                "$$REMOVE")
                )
                .put("created_at", getDate("$created_at", DateHelper.MONGO_FORMAT_TO_STRING_YMD_HMINS))
                .put("updated_at", getDate("$updated_at", DateHelper.MONGO_FORMAT_TO_STRING_YMD_HMINS));
    }

    private JsonObject getCursor() {
        return new JsonObject().put("batchSize", 2147483647);
    }

    @SuppressWarnings("unchecked")
    private void mapGetterResults(JsonArray result, String order, boolean reverse, Handler<AsyncResult<JsonArray>> handler) {
        if (result.size() == 0) {
            handler.handle(Future.succeededFuture(result));
            return;
        }

        List<String> ownerIds = ((List<JsonObject>) result.getList())
                .stream().map(res -> res.getString(Field.OWNER_ID)).collect(Collectors.toList());

        List<String> studentIds = ((List<JsonObject>) result.getList())
                .stream().map(res -> res.getString(Field.STUDENT_ID)).collect(Collectors.toList());

        List<Long> typeIds = ((List<JsonObject>) result.getList())
                .stream().map(res -> res.getLong(Field.TYPE_ID)).collect(Collectors.toList());

        Promise<JsonArray> ownersPromise = Promise.promise();
        Promise<JsonArray> studentsPromise = Promise.promise();
        Promise<JsonArray> typesPromise = Promise.promise();

        userService.getUsers(ownerIds, resUsers -> {
            if (resUsers.isLeft()) {
                String message = String.format("[Incidents@%s::mapGetterResults] Failed to get owners: %s",
                        this.getClass().getSimpleName(), resUsers.left().getValue());
                log.error(message);
                ownersPromise.fail(message);
            } else {
                ownersPromise.complete(resUsers.right().getValue());
            }
        });

        userService.getStudents(studentIds, resUsers -> {
            if (resUsers.isLeft()) {
                String message = String.format("[Incidents@%s::mapGetterResults] Failed to get students: %s",
                        this.getClass().getSimpleName(), resUsers.left().getValue());
                log.error(message);
                studentsPromise.fail(message);
            } else {
                studentsPromise.complete(resUsers.right().getValue());
            }
        });

        String query = "SELECT * FROM " + Incidents.dbSchema + ".punishment_type WHERE id IN " + Sql.listPrepared(typeIds);
        JsonArray params = new JsonArray().addAll(new JsonArray(typeIds));

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(resTypes -> {
            if (resTypes.isLeft()) {
                String message = String.format("[Incidents@%s::mapGetterResults] Failed to get types: %s",
                        this.getClass().getSimpleName(), resTypes.left().getValue());
                log.error(message);
                typesPromise.fail(message);
            } else {
                typesPromise.complete(resTypes.right().getValue());
            }
        }));

        CompositeFuture.all(ownersPromise.future(), studentsPromise.future(), typesPromise.future())
                .onFailure(fail -> handler.handle(Future.failedFuture(fail.getCause().getMessage())))
                .onSuccess(resultFuture -> {
                    Map<String, JsonObject> ownerMap = new HashMap<>();
                    ownersPromise.future().result().forEach(oOwner -> {
                        JsonObject owner = (JsonObject) oOwner;
                        ownerMap.put(owner.getString(Field.ID), owner);
                    });

                    Map<String, JsonObject> studentMap = new HashMap<>();
                    studentsPromise.future().result().forEach(oStudent -> {
                        JsonObject student = (JsonObject) oStudent;
                        studentMap.put(student.getString(Field.ID), student);
                    });

                    Map<Long, JsonObject> typeMap = new HashMap<>();
                    typesPromise.future().result().forEach(oType -> {
                        JsonObject type = (JsonObject) oType;
                        typeMap.put(type.getLong(Field.ID), type);
                    });

                    result.forEach(oRes -> {
                        JsonObject res = (JsonObject) oRes;
                        res.put(Field.OWNER, ownerMap.get(res.getString(Field.OWNER_ID)));
                        res.put(Field.STUDENT, studentMap.get(res.getString(Field.STUDENT_ID)));
                        res.put(Field.TYPE, typeMap.get(res.getLong(Field.TYPE_ID)));
                        res.put(Field.ID, res.getString(Field._ID));
                    });


                    handler.handle(Future.succeededFuture(sortPunishmentByField(result, order, reverse)));
                });
    }

    @SuppressWarnings("unchecked")
    private JsonArray sortPunishmentByField(JsonArray punishments, String order, boolean reverse) {
        List<JsonObject> list = punishments.getList();
        if (order == null) {
            return new JsonArray(list);
        }
        switch (order) {
            case Field.DISPLAYNAME:
                Collections.sort(list, (o1, o2) -> reverse ?
                        o1.getJsonObject(Field.STUDENT).getString(Field.NAME)
                                .compareToIgnoreCase(o2.getJsonObject(Field.STUDENT).getString(Field.NAME)) :
                        o2.getJsonObject(Field.STUDENT).getString(Field.NAME)
                                .compareToIgnoreCase(o1.getJsonObject(Field.STUDENT).getString(Field.NAME)));
                break;
            case Field.CLASSNAME:
                Collections.sort(list, (o1, o2) -> reverse ?
                        o1.getJsonObject(Field.STUDENT).getString(Field.CLASSNAME)
                                .compareToIgnoreCase(o2.getJsonObject(Field.STUDENT).getString(Field.CLASSNAME)) :
                        o2.getJsonObject(Field.STUDENT).getString(Field.CLASSNAME)
                                .compareToIgnoreCase(o1.getJsonObject(Field.STUDENT).getString(Field.CLASSNAME)));
                break;
            case Field.OWNER:
                Collections.sort(list, (o1, o2) -> reverse ?
                        o1.getJsonObject(Field.OWNER).getString(Field.DISPLAYNAME)
                                .compareToIgnoreCase(o2.getJsonObject(Field.OWNER).getString(Field.DISPLAYNAME)) :
                        o2.getJsonObject(Field.OWNER).getString(Field.DISPLAYNAME)
                                .compareToIgnoreCase(o1.getJsonObject(Field.OWNER).getString(Field.DISPLAYNAME)));
                break;

            case Field.TYPE:
                Collections.sort(list, (o1, o2) -> reverse ?
                        o1.getJsonObject(Field.TYPE).getString(Field.LABEL)
                                .compareToIgnoreCase(o2.getJsonObject(Field.TYPE).getString(Field.LABEL)) :
                        o2.getJsonObject(Field.TYPE).getString(Field.LABEL)
                                .compareToIgnoreCase(o1.getJsonObject(Field.TYPE).getString(Field.LABEL)));
                break;
            default:
                break;
        }

        return new JsonArray(list);
    }
}

