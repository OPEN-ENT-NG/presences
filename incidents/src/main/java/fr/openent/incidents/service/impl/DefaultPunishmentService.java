package fr.openent.incidents.service.impl;

import com.mongodb.BasicDBObject;
import com.sun.org.apache.xpath.internal.operations.Bool;
import fr.openent.incidents.Incidents;
import fr.openent.incidents.enums.PunishmentsProcessState;
import fr.openent.incidents.enums.WorkflowActions;
import fr.openent.incidents.model.Punishment;
import fr.openent.incidents.service.PunishmentService;
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

public class DefaultPunishmentService implements PunishmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPunishmentService.class);
    private GroupService groupService;
    private UserService userService;
    private Punishment punishment = new Punishment();


    public DefaultPunishmentService(EventBus eb) {
        this.groupService = new DefaultGroupService(eb);
        this.userService = new DefaultUserService();
    }

    @Override
    public void create(UserInfos user, JsonObject body, Handler<AsyncResult<JsonArray>> handler) {

        JsonArray student_ids = body.getJsonArray("student_ids");
        JsonArray results = new JsonArray();
        List<Future> futures = new ArrayList<>();

        for (Object oStudent_id : student_ids) {
            Future future = Future.future();
            futures.add(future);
            String student_id = (String) oStudent_id;
            Punishment createPunishment = new Punishment();
            createPunishment.setFromJson(body);
            createPunishment.setStudentId(student_id);

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
            handler.handle(Future.succeededFuture(results));
        });
    }

    @Override
    public void update(UserInfos user, JsonObject body, Handler<AsyncResult<JsonObject>> handler) {
        Punishment updatePunishment = new Punishment();
        updatePunishment.setFromJson(body);
        updatePunishment.persistMongo(user, result -> {
            if (result.failed()) {
                handler.handle(Future.failedFuture(result.cause().getMessage()));
                return;
            }
            handler.handle(Future.succeededFuture(result.result()));
        });
    }

    @Override
    public void get(UserInfos user, MultiMap body, boolean isStudent, Handler<AsyncResult<JsonObject>> handler) {
        getQuery(user, body, isStudent, queryResult -> {
            if(queryResult.failed()) {
                handler.handle(Future.failedFuture(queryResult.cause().getMessage()));
                return;
            }

            String id = body.get("id");

            if (id != null && !id.equals("")) {
                MongoDb.getInstance().findOne(punishment.getTable(), JsonObject.mapFrom(queryResult.result()), message -> {
                    Either<String, JsonObject> messageCheck = MongoDbResult.validResult(message);
                    if (messageCheck.isLeft()) {
                        handler.handle(Future.failedFuture("[Incidents@Punishment::persistMongo] Failed to get punishment by id."));
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
            } else {
                Integer limit, offset, page = null;
                String pageString = body.get("page");
                String limitString = body.get("limit");
                String offsetString = body.get("offset");

                int aPageNumber = Incidents.PAGE_SIZE;

                Future<Long> countFuture = Future.future();
                Future<JsonArray> findFuture = Future.future();

                JsonObject sort = new JsonObject()
                        .put("fields.end_at", -1)
                        .put("fields.start_at", -1)
                        .put("created_at", -1);

                if (pageString != null && !pageString.equals("")) {
                    page = Integer.parseInt(pageString);
                    offset = page * aPageNumber;
                    limit = aPageNumber;
                } else {
                    offset = offsetString != null && !offsetString.equals("") ? Integer.parseInt(offsetString) : 0;
                    limit = limitString != null && !limitString.equals("") ? Integer.parseInt(limitString) : -1;
                }

                countFromQuery(queryResult.result(), countResult -> {
                    if (countResult.failed()) {
                        countFuture.fail(countResult.cause());
                        return;
                    }
                    countFuture.complete(countResult.result());
                });

                MongoDb.getInstance().find(punishment.getTable(),
                        JsonObject.mapFrom(queryResult.result()),
                        sort,
                        null,
                        offset,
                        limit,
                        limit,
                        null,
                        message -> {
                            Either<String, JsonObject> messageCheck = MongoDbResult.validResult(message);
                            if (messageCheck.isLeft()) {
                                findFuture.fail("[Incidents@Punishment::persistMongo] Failed to get punishments.");
                            } else {
                                JsonArray result = message.body().getJsonArray("results");

                                mapGetterResults(result, mapResult -> {
                                    if (mapResult.failed()) {
                                        findFuture.fail(mapResult.cause());
                                    } else {
                                        findFuture.complete(mapResult.result());
                                    }
                                });
                            }
                        });

                Integer finalPage = page;
                CompositeFuture.all(countFuture, findFuture).setHandler(resultFuture -> {
                    if (resultFuture.failed()) {
                        handler.handle(Future.failedFuture(resultFuture.cause()));
                    } else {
                        JsonObject finalResult = new JsonObject();
                        if (finalPage != null) {
                            Long pageCount = countFuture.result() / Incidents.PAGE_SIZE;
                            finalResult
                                    .put("page", finalPage)
                                    .put("page_count", pageCount);
                        } else {
                            finalResult
                                    .put("limit", limit)
                                    .put("offset", offset);
                        }

                        finalResult.put("all", findFuture.result());
                        handler.handle(Future.succeededFuture(finalResult));
                    }
                });
            }
        });
    }

    @Override
    public void count(UserInfos user, MultiMap body, boolean isStudent, Handler<AsyncResult<Long>> handler) {
        getQuery(user, body, isStudent, result -> {
            if(result.failed()) {
                handler.handle(Future.failedFuture(result.cause().getMessage()));
                return;
            }
            countFromQuery(result.result(), handler);
        });
    }

    private void countFromQuery(BasicDBObject query, Handler<AsyncResult<Long>> handler) {
        MongoDb.getInstance().count(punishment.getTable(), JsonObject.mapFrom(query), message -> {
            Either<String, JsonObject> messageCheck = MongoDbResult.validResult(message);
            if (messageCheck.isLeft()) {
                handler.handle(Future.failedFuture("[Incidents@Punishment::countFromQuery] Failed to get count values."));
            } else {
                handler.handle(Future.succeededFuture(message.body().getLong("count")));
            }
        });
    }

    @Override
    public void delete(UserInfos user, MultiMap body, Handler<AsyncResult<JsonObject>> handler) {
        JsonObject query = new JsonObject()
                .put("_id", body.get("id"));
        MongoDb.getInstance().delete(punishment.getTable(), query, message -> {
            Either<String, JsonObject> messageCheck = MongoDbResult.validResult(message);
            if (messageCheck.isLeft()) {
                handler.handle(Future.failedFuture("[Incidents@Punishment::persistMongo] Failed to delete punishment"));
            } else {
                JsonObject result = messageCheck.right().getValue();
                handler.handle(Future.succeededFuture(result));
            }
        });
    }

    private void getQuery(UserInfos user, MultiMap body, boolean isStudent, Handler<AsyncResult<BasicDBObject>> handler) {
        String id = body.get("id");
        List<BasicDBObject> queries = new ArrayList<>();
        BasicDBObject query = new BasicDBObject();

        if (!isStudent && (!WorkflowHelper.hasRight(user, WorkflowActions.PUNISHMENTS_VIEW.toString()) || !WorkflowHelper.hasRight(user, WorkflowActions.SANCTIONS_VIEW.toString()))) {
            queries.add(new BasicDBObject("owner_id", user.getUserId()));
        }

        queries.add(new BasicDBObject("structure_id", body.get("structure_id")));

        if (id != null && !id.equals("")) {

            queries.add(new BasicDBObject("_id", id));
            if (isStudent) queries.add(new BasicDBObject("student_id", user.getUserId()));
            query.put("$and", queries);
            handler.handle(Future.succeededFuture(query));
        } else {
            String start_at = body.get("start_at");
            String end_at = body.get("end_at");
            List<String> student_ids = body.getAll("student_id");
            List<String> group_ids = body.getAll("group_id");
            List<String> type_ids = body.getAll("type_id");
            List<String> process_states = body.getAll("process");

            this.groupService.getGroupStudents(group_ids, groupResult -> {
                if (groupResult.isLeft()) {
                    handler.handle(Future.failedFuture("[Incidents@Punishment::getQuery] Failed to retrieve students from groups"));
                    return;
                }

                if (start_at != null && end_at != null) {

                    List<BasicDBObject> containDateQueries = new ArrayList<>();
                    containDateQueries.add(new BasicDBObject("fields.start_at", new BasicDBObject("$lt", end_at)));
                    containDateQueries.add(new BasicDBObject("fields.end_at", new BasicDBObject("$gt", start_at)));
                    BasicDBObject containDateQuery = new BasicDBObject("$and", containDateQueries);
                    BasicDBObject nullDateQuery = new BasicDBObject("created_at", new BasicDBObject("$gt", start_at).append("$lt", end_at));

                    queries.add(new BasicDBObject("$or", Arrays.asList(containDateQuery, nullDateQuery)));
                }

                if (isStudent) queries.add(new BasicDBObject("student_id", user.getUserId()));
                else {
                    groupResult.right().getValue().forEach(oStudent -> {
                        JsonObject student = (JsonObject) oStudent;
                        student_ids.add(student.getString("id"));
                    });

                    if (student_ids != null && student_ids.size() > 0) {
                        queries.add(new BasicDBObject("student_id", new BasicDBObject("$in", student_ids)));
                    }
                }

                if (type_ids != null && type_ids.size() > 0) {
                    List<Long> listTypeIds = new ArrayList<>();
                    type_ids.forEach(type_id -> listTypeIds.add(Long.parseLong(type_id)));
                    queries.add(new BasicDBObject("type_id", new BasicDBObject("$in", listTypeIds)));
                }


                if (process_states != null && process_states.size() > 0) {
                    Boolean isProcessed = process_states.contains(PunishmentsProcessState.PROCESSED.toString());
                    Boolean isNotProcessed = process_states.contains(PunishmentsProcessState.NOT_PROCESSED.toString());
                    Boolean isAllSelected = isProcessed && isNotProcessed;

                    if (!isAllSelected) {
                        if (isProcessed) {
                            queries.add(new BasicDBObject("processed", true));
                        } else if (isNotProcessed) {
                            queries.add(new BasicDBObject("processed", false));
                        }
                    }
                }

                query.put("$and", queries);

                handler.handle(Future.succeededFuture(query));
            });
        }
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
