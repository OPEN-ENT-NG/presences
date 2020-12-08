package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.helper.PunishmentHelper;
import fr.openent.incidents.helper.PunishmentTypeHelper;
import fr.openent.incidents.model.Punishment;
import fr.openent.incidents.model.PunishmentType;
import fr.openent.incidents.service.PunishmentService;
import fr.openent.incidents.service.PunishmentTypeService;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultUserService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        punishmentHelper.getQuery(user, body, isStudent, queryResult -> {
            if (queryResult.failed()) {
                handler.handle(Future.failedFuture(queryResult.cause().getMessage()));
                return;
            }

            String id = body.get("id");

            if (id != null && !id.equals("")) {
                punishmentHelper.getPunishment(punishment.getTable(), queryResult.result(), handler);
            } else {
                Future<Long> countFuture = Future.future();
                Future<JsonArray> findFuture = Future.future();

                /* PAGINATE QUERY PUNISHMENTS */
                String pageString = body.get("page");
                String limitString = body.get("limit");
                String offsetString = body.get("offset");
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
    public void getPunishmentByStudents(String structure, String start_at, String end_at, List<String> students,
                                        List<Integer> type_id, Boolean processed, Boolean massmailed,
                                        Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> punishmentsFuture = Future.future();
        Future<JsonArray> punishmentsTypesFuture = Future.future();

        CompositeFuture.all(punishmentsFuture, punishmentsTypesFuture).setHandler(event -> {
            if (event.failed()) {
                LOGGER.info("[Incidents@DefaultPunishmentService::getPunishmentByStudents] Failed to fetch punishmentType or " +
                        " punishment by student: ", event.cause());
                handler.handle(new Either.Left<>(event.cause().getMessage()));
            } else {
                List<PunishmentType> punishmentsTypes = PunishmentTypeHelper.
                        getPunishmentTypeListFromJsonArray(punishmentsTypesFuture.result());

                // for some reason, we still manage to find some "duplicate" data so we use mergeFunction (see collectors.toMap)
                Map<Integer, PunishmentType> punishmentTypeMap = punishmentsTypes
                        .stream()
                        .collect(Collectors.toMap(PunishmentType::getId, PunishmentType::clone, (punishmentType1, punishmentType2) -> punishmentType1));

                List<String> ownerIds = new ArrayList<>();
                setOwnerIdsFromPunishment(punishmentsFuture, ownerIds);
                userService.getUsers(ownerIds, userAsync -> {
                    if (userAsync.isLeft()) {
                        LOGGER.info("[Incidents@DefaultPunishmentService::getPunishmentByStudents] Failed to " +
                                "fetch owner info: ", userAsync.left().getValue());
                        handler.handle(new Either.Left<>(userAsync.left().getValue()));
                    } else {
                        setPunishmentInfo(punishmentsFuture, punishmentTypeMap, userAsync);
                        handler.handle(new Either.Right<>(punishmentsFuture.result()));
                    }
                });
            }
        });

        punishmentHelper.getPunishmentsCommand(punishment.getTable(), getPunishmentByStudentQueryPipeline(structure, start_at, end_at,
                students, type_id, processed, massmailed), FutureHelper.handlerAsyncJsonArray(punishmentsFuture));
        punishmentType.get(structure, FutureHelper.handlerJsonArray(punishmentsTypesFuture));
    }

    @SuppressWarnings("unchecked")
    private void setOwnerIdsFromPunishment(Future<JsonArray> punishmentsFuture, List<String> ownerIds) {
        ((List<JsonObject>) punishmentsFuture.result().getList()).forEach(punishmentStudent ->
                ((List<JsonObject>) punishmentStudent.getJsonArray("punishments").getList()).forEach(punishment -> {
                    if (!ownerIds.contains(punishment.getString("owner_id"))) {
                        ownerIds.add(punishment.getString("owner_id"));
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    private void setPunishmentInfo(Future<JsonArray> punishmentsFuture, Map<Integer, PunishmentType> punishmentTypeMap,
                                   Either<String, JsonArray> userAsync) {
        Map<String, JsonObject> ownerMap = ((List<JsonObject>) userAsync.right().getValue().getList())
                .stream()
                .collect(Collectors.toMap(user -> user.getString("id"), Function.identity(), (owner1, owner2) -> owner1));

        // remove _id
        ((List<JsonObject>) punishmentsFuture.result().getList()).forEach(punishmentStudent -> {
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
    public void getPunishmentCountByStudent(String structure, String start_at, String end_at, List<String> students,
                                            List<Integer> type_id, Boolean processed, Boolean massmailed,
                                            Handler<Either<String, JsonArray>> handler) {
        punishmentHelper.getPunishmentsCommand(punishment.getTable(), getPunishmentCountByStudentQueryPipeline(structure, start_at,
                end_at, students, type_id, processed, massmailed), punishmentEvent -> {
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

    private JsonArray getPunishmentByStudentQueryPipeline(String structure, String start_at, String end_at, List<String> students,
                                                          List<Integer> type_id, Boolean processed, Boolean massmailed) {
        JsonObject $queryMatch = getQueryMatchPunishmentByStudent(structure, start_at, end_at, students, type_id, processed, massmailed);
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

    private JsonArray getPunishmentCountByStudentQueryPipeline(String structure, String start_at, String end_at, List<String> students,
                                                               List<Integer> type_id, Boolean processed, Boolean massmailed) {
        JsonObject $queryMatch = getQueryMatchPunishmentByStudent(structure, start_at, end_at, students,
                type_id, processed, massmailed);

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

    private JsonObject getQueryMatchPunishmentByStudent(String structure, String start_at, String end_at, List<String> students,
                                                        List<Integer> type_id, Boolean processed, Boolean massmailed) {
        JsonObject $queryMatch = new JsonObject()
                .put("structure_id", structure)
                .put("$or", punishmentHelper.getPunishmentMatchingDate(start_at, end_at).getJsonArray("$or"));

        if (students != null && !students.isEmpty()) {
            $queryMatch.put("student_id", new JsonObject().put("$in", new JsonArray(students)));
        }

        if (type_id != null && !type_id.isEmpty()) {
            $queryMatch.put("type_id", new JsonObject().put("$in", new JsonArray(type_id)));
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
}
