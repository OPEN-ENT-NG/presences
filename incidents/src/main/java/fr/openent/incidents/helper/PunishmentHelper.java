package fr.openent.incidents.helper;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.enums.PunishmentsProcessState;
import fr.openent.incidents.enums.WorkflowActions;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.service.impl.DefaultUserService;
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

public class PunishmentHelper {
    private GroupService groupService;
    private UserService userService;

    private static final Logger log = LoggerFactory.getLogger(PunishmentHelper.class);

    public PunishmentHelper(EventBus eb) {
        this.groupService = new DefaultGroupService(eb);
        this.userService = new DefaultUserService();
    }

    /* QUERY CONSTRUCTION PART */

    public void getQuery(UserInfos user, MultiMap body, boolean isStudent, Handler<AsyncResult<JsonObject>> handler) {
        String id = body.get("id");
        JsonObject query = new JsonObject().put("structure_id", body.get("structure_id"));

        if (!isStudent && (!WorkflowHelper.hasRight(user, WorkflowActions.PUNISHMENTS_VIEW.toString()) || !WorkflowHelper.hasRight(user, WorkflowActions.SANCTIONS_VIEW.toString()))) {
            query.put("owner_id", user.getUserId());
        }

        if (isStudent) query.put("student_id", user.getUserId());

        if (id != null && !id.equals("")) {
            query.put("_id", id);
            handler.handle(Future.succeededFuture(query));
        } else {
            String start_at = body.get("start_at");
            String end_at = body.get("end_at");
            List<String> student_ids = body.getAll("student_id");
            List<String> group_ids = body.getAll("group_id");
            List<String> type_ids = body.getAll("type_id");
            List<String> process_states = body.getAll("process");

            getManyPunishmentsQuery(query, start_at, end_at, student_ids, group_ids, type_ids, process_states, isStudent, handler);
        }
    }

    private void getManyPunishmentsQuery(JsonObject query, String start_at, String end_at, List<String> student_ids, List<String> group_ids,
                                         List<String> type_ids, List<String> process_states, boolean isStudent,
                                         Handler<AsyncResult<JsonObject>> handler) {
        this.groupService.getGroupStudents(group_ids, groupResult -> {
            if (groupResult.isLeft()) {
                handler.handle(Future.failedFuture("[Incidents@Punishment::getQuery] Failed to retrieve students from groups"));
                return;
            }

            if (start_at != null && end_at != null) {

                //Check date for detentions and exclusions
                JsonArray containDateQueries = new JsonArray(Arrays.asList(
                        new JsonObject().put("fields.start_at", new JsonObject().put("$lte", end_at)),
                        new JsonObject().put("fields.end_at", new JsonObject().put("$gte", start_at))
                ));
                JsonObject containDateQuery = new JsonObject().put("$and", containDateQueries);

                //Check date for duties
                JsonArray containDateDutyQueries = new JsonArray(Arrays.asList(
                        new JsonObject().put("fields.delay_at", new JsonObject().put("$lte", end_at)),
                        new JsonObject().put("fields.delay_at", new JsonObject().put("$gte", start_at))
                ));
                JsonObject containDutyDateQuery = new JsonObject().put("$and", containDateDutyQueries);

                //Check creation date
                JsonObject nullDateQuery = new JsonObject().put("created_at", new JsonObject().put("$gte", start_at).put("$lte", end_at));

                //Check for formatted format : YYYY/MM/DD instead of YYYY-MM-DD
                JsonObject nullDateQuery2 = new JsonObject().put("created_at", new JsonObject()
                        .put("$gte", DateHelper.getDateString(start_at, DateHelper.YEAR_MONTH_DAY_HOUR_MINUTES_SECONDS))
                        .put("$lte", DateHelper.getDateString(end_at, DateHelper.YEAR_MONTH_DAY_HOUR_MINUTES_SECONDS)));

                query.put("$or", new JsonArray(Arrays.asList(containDateQuery, nullDateQuery, nullDateQuery2, containDutyDateQuery)));
            }

            if (!isStudent) {
                groupResult.right().getValue().forEach(oStudent -> {
                    JsonObject student = (JsonObject) oStudent;
                    student_ids.add(student.getString("id"));
                });

                if (student_ids != null && student_ids.size() > 0)
                    query.put("student_id", new JsonObject().put("$in", new JsonArray(student_ids)));
            }

            if (type_ids != null && type_ids.size() > 0) {
                List<Long> listTypeIds = new ArrayList<>();
                type_ids.forEach(type_id -> listTypeIds.add(Long.parseLong(type_id)));
                query.put("type_id", new JsonObject().put("$in", new JsonArray(listTypeIds)));
            }


            if (process_states != null && process_states.size() > 0) {
                Boolean isProcessed = process_states.contains(PunishmentsProcessState.PROCESSED.toString());
                Boolean isNotProcessed = process_states.contains(PunishmentsProcessState.NOT_PROCESSED.toString());
                boolean isAllSelected = isProcessed && isNotProcessed;

                if (!isAllSelected) {
                    if (isProcessed) {
                        query.put("processed", true);
                    } else if (isNotProcessed) {
                        query.put("processed", false);
                    }
                }
            }

            handler.handle(Future.succeededFuture(query));
        });
    }

    /* REQUEST MONGODB PART */

    public void getPunishment(String tableName, JsonObject query, Handler<AsyncResult<JsonObject>> handler) {
        MongoDb.getInstance().findOne(tableName, query, message -> {
            Either<String, JsonObject> messageCheck = MongoDbResult.validResult(message);
            if (messageCheck.isLeft()) {
                handler.handle(Future.failedFuture("[Incidents@PunishmentHelper::getPunishment] Failed to get punishment by id."));
            } else {
                JsonObject result = messageCheck.right().getValue();
                mapGetterResults(new JsonArray().add(result), mapResult -> {
                    if (mapResult.failed()) {
                        handler.handle(Future.failedFuture(mapResult.cause()));
                    } else {
                        handler.handle(Future.succeededFuture(mapResult.result().getJsonObject(0)));
                    }
                });
            }
        });
    }

    public void getPunishments(String tableName, JsonObject query, Integer limit, Integer offset, Handler<AsyncResult<JsonArray>> handler) {
        JsonObject command = new JsonObject()
                .put("aggregate", tableName)
                .put("allowDiskUse", true)
                .put("cursor", getCursor())
                .put("pipeline", getPipeline(query, limit, offset));

        MongoDb.getInstance().command(command.toString(), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error("[Incidents@PunishmentHelper::getPunishments] Failed to get punishments.", either.left().getValue());
                handler.handle(Future.failedFuture(either.left().getValue()));
                return;
            }
            JsonArray result = either.right().getValue().getJsonObject("cursor", new JsonObject()).getJsonArray("firstBatch", new JsonArray());
            mapGetterResults(result, mapResult -> {
                if (mapResult.failed()) {
                    log.error("[Incidents@PunishmentHelper::getPunishments] Failed to map punishments.", mapResult.cause());
                    handler.handle(Future.failedFuture(mapResult.cause()));
                } else {
                    handler.handle(Future.succeededFuture(mapResult.result()));
                }
            });
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

    private JsonArray getPipeline(JsonObject query, Integer limit, Integer offset) {
        JsonArray pipeline = new JsonArray()
                .add(new JsonObject().put("$addFields",
                        addSortField()
                ))
                .add(new JsonObject().put("$match", query))
                .add(new JsonObject().put("$sort", new JsonObject().put("sortField", -1)))
                .add(new JsonObject().put("$project", addProject()));

        pipeline.add(new JsonObject().put("$skip", offset));
        pipeline.add(new JsonObject().put("$limit", limit));

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
                                        new JsonObject().put("$gt", new JsonArray(Arrays.asList("$fields.end_at", null))),
                                        getDate("$fields.end_at")
                                ),
                                getCase(
                                        new JsonObject().put("$gt", new JsonArray(Arrays.asList("$fields.start_at", null))),
                                        getDate("$fields.start_at")
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
                .put("_id", 1)
                .put("description", 1)
                .put("processed", 1)
                .put("incident_id", 1)
                .put("type_id", 1)
                .put("owner_id", 1)
                .put("structure_id", 1)
                .put("student_id", 1)
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

    private void mapGetterResults(JsonArray result, Handler<AsyncResult<JsonArray>> handler) {
        if (result.size() == 0) {
            handler.handle(Future.succeededFuture(result));
            return;
        }

        List<String> ownerIds = ((List<JsonObject>) result.getList())
                .stream()
                .map(res -> res.getString("owner_id"))
                .collect(Collectors.toList());

        List<String> studentIds = ((List<JsonObject>) result.getList())
                .stream()
                .map(res -> res.getString("student_id"))
                .collect(Collectors.toList());

        List<Long> typeIds = ((List<JsonObject>) result.getList())
                .stream()
                .map(res -> res.getLong("type_id"))
                .collect(Collectors.toList());

        Future<JsonArray> ownersFuture = Future.future();
        Future<JsonArray> studentsFuture = Future.future();
        Future<JsonArray> typesFuture = Future.future();

        userService.getUsers(ownerIds, resUsers -> {
            if (resUsers.isLeft()) {
                ownersFuture.fail("[Incidents@Punishment::mapGetterResults] Failed to get owners");
            } else {
                ownersFuture.complete(resUsers.right().getValue());
            }
        });

        userService.getStudents(studentIds, resUsers -> {
            if (resUsers.isLeft()) {
                studentsFuture.fail("[Incidents@Punishment::mapGetterResults] Failed to get students");
            } else {
                studentsFuture.complete(resUsers.right().getValue());
            }
        });

        String query = "SELECT * FROM " + Incidents.dbSchema + ".punishment_type WHERE id IN " + Sql.listPrepared(typeIds);
        JsonArray params = new JsonArray().addAll(new JsonArray(typeIds));

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(resTypes -> {
            if (resTypes.isLeft()) {
                typesFuture.fail("[Incidents@Punishment::mapGetterResults] Failed to get types");
            } else {
                typesFuture.complete(resTypes.right().getValue());
            }
        }));

        CompositeFuture.all(ownersFuture, studentsFuture, typesFuture).setHandler(resultFuture -> {
            if (resultFuture.failed()) {
                handler.handle(Future.failedFuture(resultFuture.cause()));
            } else {
                Map<String, JsonObject> ownerMap = new HashMap<>();
                ownersFuture.result().forEach(oOwner -> {
                    JsonObject owner = (JsonObject) oOwner;
                    ownerMap.put(owner.getString("id"), owner);
                });

                Map<String, JsonObject> studentMap = new HashMap<>();
                studentsFuture.result().forEach(oStudent -> {
                    JsonObject student = (JsonObject) oStudent;
                    studentMap.put(student.getString("id"), student);
                });

                Map<Long, JsonObject> typeMap = new HashMap<>();
                typesFuture.result().forEach(oType -> {
                    JsonObject type = (JsonObject) oType;
                    typeMap.put(type.getLong("id"), type);
                });

                result.forEach(oRes -> {
                    JsonObject res = (JsonObject) oRes;
                    res.put("owner", ownerMap.get(res.getString("owner_id")));
                    res.put("student", studentMap.get(res.getString("student_id")));
                    res.put("type", typeMap.get(res.getLong("type_id")));

                    res.put("id", res.getString("_id"));
                    res.remove("_id");
                    res.remove("owner_id");
                    res.remove("student_id");
                    res.remove("type_id");
                });
                handler.handle(Future.succeededFuture(result));
            }
        });

    }
}

