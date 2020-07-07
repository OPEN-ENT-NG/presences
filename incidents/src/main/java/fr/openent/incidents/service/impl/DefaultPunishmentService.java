package fr.openent.incidents.service.impl;

import com.mongodb.BasicDBObject;
import fr.openent.incidents.Incidents;
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
            Punishment punishment = new Punishment();
            punishment.setFromJson(body);
            punishment.setStudentId(student_id);

            punishment.persistMongo(user, result -> {
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
        Punishment punishment = new Punishment();
        punishment.setFromJson(body);
        punishment.persistMongo(user, result -> {
            if (result.failed()) {
                Future.failedFuture(result.cause().getMessage());
                return;
            }
            handler.handle(Future.succeededFuture(result.result()));
        });
    }

    @Override
    public void get(UserInfos user, MultiMap body, Handler<AsyncResult<JsonObject>> handler) {
        int aPageNumber = Incidents.PAGE_SIZE;
        Punishment punishment = new Punishment();
        String id = body.get("id");
        List<BasicDBObject> queries = new ArrayList<>();
        BasicDBObject query = new BasicDBObject();

        if (!WorkflowHelper.hasRight(user, WorkflowActions.PUNISHMENTS_VIEW.toString()) || !WorkflowHelper.hasRight(user, WorkflowActions.SANCTIONS_VIEW.toString())) {
            queries.add(new BasicDBObject("owner_id", user.getUserId()));
        }

        queries.add(new BasicDBObject("structure_id", body.get("structure_id")));
        if (id != null && !id.equals("")) {
            queries.add(new BasicDBObject("_id", id));
            query.put("$and", queries);

            MongoDb.getInstance().findOne(punishment.getTable(), JsonObject.mapFrom(query), message -> {
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

            String start_at = body.get("start_at");
            String end_at = body.get("end_at");
            List<String> student_ids = body.getAll("student_id");
            List<String> group_ids = body.getAll("group_id");
            List<String> type_ids = body.getAll("type_id");
            String pageString = body.get("page");

            this.groupService.getGroupStudents(group_ids, groupResult -> {
                if (groupResult.isLeft()) {
                    handler.handle(Future.failedFuture("[Incidents@Punishment::persistMongo] Failed to retrieve students from groups"));
                    return;
                }

                groupResult.right().getValue().forEach(oStudent -> {
                    JsonObject student = (JsonObject) oStudent;
                    student_ids.add(student.getString("id"));
                });

                int page = pageString != null && !pageString.equals("") ? Integer.parseInt(pageString) : 0;

                if (start_at != null && end_at != null) {

                    List<BasicDBObject> containDateQueries = new ArrayList<>();
                    containDateQueries.add(new BasicDBObject("fields.start_at", new BasicDBObject("$lt", end_at)));
                    containDateQueries.add(new BasicDBObject("fields.end_at", new BasicDBObject("$gt", start_at)));
                    BasicDBObject containDateQuery = new BasicDBObject("$and", containDateQueries);
                    BasicDBObject nullDateQuery = new BasicDBObject("created_at", new BasicDBObject("$gt", start_at).append("$lt", end_at));

                    queries.add(new BasicDBObject("$or", Arrays.asList(containDateQuery, nullDateQuery)));
                }

                if (student_ids != null && student_ids.size() > 0) {
                    queries.add(new BasicDBObject("student_id", new BasicDBObject("$in", student_ids)));
                }

                if (type_ids != null && type_ids.size() > 0) {
                    List<Long> listTypeIds = new ArrayList<>();
                    type_ids.forEach(type_id -> listTypeIds.add(Long.parseLong(type_id)));
                    queries.add(new BasicDBObject("type_id", new BasicDBObject("$in", listTypeIds)));
                }

                JsonObject sort = new JsonObject().put("created_at", -1);

                query.put("$and", queries);

                Future<Long> countFuture = Future.future();
                Future<JsonArray> findFuture = Future.future();


                MongoDb.getInstance().count(punishment.getTable(), JsonObject.mapFrom(query), message -> {
                    Either<String, JsonObject> messageCheck = MongoDbResult.validResult(message);
                    if (messageCheck.isLeft()) {
                        countFuture.fail("[Incidents@Punishment::persistMongo] Failed to get count values.");
                    } else {
                        countFuture.complete(message.body().getLong("count"));
                    }
                });

                MongoDb.getInstance().find(punishment.getTable(),
                        JsonObject.mapFrom(query),
                        sort,
                        null,
                        (page * aPageNumber),
                        aPageNumber,
                        aPageNumber,
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

                CompositeFuture.all(countFuture, findFuture).setHandler(resultFuture -> {
                    if (resultFuture.failed()) {
                        handler.handle(Future.failedFuture(resultFuture.cause()));
                    } else {
                        Long pageCount = countFuture.result() / Incidents.PAGE_SIZE;
                        JsonObject finalResult = new JsonObject()
                                .put("page", page)
                                .put("page_count", pageCount)
                                .put("all", findFuture.result());
                        handler.handle(Future.succeededFuture(finalResult));
                    }
                });
            });
        }
    }

    @Override
    public void delete(UserInfos user, MultiMap body, Handler<AsyncResult<JsonObject>> handler) {
        Punishment punishment = new Punishment();
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
