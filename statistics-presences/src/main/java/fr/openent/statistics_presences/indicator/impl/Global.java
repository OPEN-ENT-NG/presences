package fr.openent.statistics_presences.indicator.impl;

import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.bean.User;
import fr.openent.statistics_presences.bean.global.GlobalSearch;
import fr.openent.statistics_presences.bean.global.GlobalValue;
import fr.openent.statistics_presences.filter.Filter;
import fr.openent.statistics_presences.indicator.Indicator;
import fr.openent.statistics_presences.indicator.IndicatorGeneric;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Global extends Indicator {
    public static final Integer PAGE_SIZE = 35;
    private final Logger log = LoggerFactory.getLogger(Global.class);

    public Global(Vertx vertx, String name) {
        super(vertx, name);
    }

    @Override
    public void search(Filter filter, Handler<AsyncResult<JsonObject>> handler) {
        setSearchSettings(filter)
                .compose(this::searchProcess)
                .onComplete(handler);
    }

    /**
     * set search and add settings to recover absences
     *
     * @param filter filter
     * @return search
     */
    private Future<GlobalSearch> setSearchSettings(Filter filter) {
        Promise<GlobalSearch> promise = Promise.promise();
        GlobalSearch search = new GlobalSearch(filter);

        Future<JsonObject> settingsFuture = IndicatorGeneric.retrieveSettings(search.filter().structure());
        Future<JsonObject> slotsSettingsFuture = IndicatorGeneric.retrieveSlotsSettings(search.filter().structure());

        CompositeFuture.all(settingsFuture, slotsSettingsFuture)
                .onSuccess(success -> {
                    search.setRecoveryMethod(settingsFuture.result().getString("event_recovery_method", "HOUR"));
                    search.setHalfDay(slotsSettingsFuture.result().getString("end_of_half_day"));
                    promise.complete(search);
                })
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Global::setSearchSettings] " +
                            "Indicator %s failed to retrieve settings", Global.class.getName()), fail.getCause());
                    promise.fail(fail.getCause());
                });
        return promise.future();
    }

    private Future<JsonObject> searchProcess(GlobalSearch search) {
        Promise<JsonObject> promise = Promise.promise();

        Future<List<JsonObject>> valuesFuture = searchValues(search);
        Future<JsonObject> countFuture = countValues(search);
        Future<Number> totalAbsFuture = globalAbsenceCount(search);
        CompositeFuture.all(valuesFuture, countFuture, totalAbsFuture)
                .onSuccess(ar -> {
                    List<JsonObject> values = valuesFuture.result();
                    JsonObject count = countFuture.result()
                            .put("ABSENCE_TOTAL", totalAbsFuture.result());
                    JsonObject response = new JsonObject()
                            .put("data", values)
                            .put("count", count);

                    promise.complete(response);
                })
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Global::searchProcess] " +
                            "Indicator %s failed to complete search", Global.class.getName()), fail.getCause());
                    promise.fail(fail.getCause());
                });
        return promise.future();
    }

    /**
     * Search for statistic values
     *
     * @param search Search object containing filter and values
     * @return Future completing search values
     */
    private Future<List<JsonObject>> searchValues(GlobalSearch search) {
        Future<List<JsonObject>> future = Future.future();
        preStatisticsStage(search)
                .compose(this::statisticStage)
                .compose(this::totalAbsenceStage)
                .compose(this::postStatisticStage)
                .compose(this::mergeStage)
                .setHandler(ar -> {
                    if (ar.failed()) {
                        log.error(String.format("[StatisticsPresences@Global::searchValues] " +
                                "Indicator %s failed to complete search values", Global.class.getName()), ar.cause());
                        future.handle(Future.failedFuture(ar.cause()));
                    } else {
                        GlobalSearch completedSearch = ar.result();
                        future.handle(Future.succeededFuture(completedSearch.users().stream().map(User::toJSON).collect(Collectors.toList())));
                    }
                });

        return future;
    }

    private Future<GlobalSearch> totalAbsenceStage(GlobalSearch search) {
        if (Boolean.FALSE.equals(search.containsAbsence()) || isEmptyPrefetch(search)) {
            return Future.succeededFuture(search);
        }

        Future<GlobalSearch> future = Future.future();
        JsonObject request = commandObject(search.totalAbsenceUserPipeline());
        MongoDb.getInstance().command(request.toString(), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@Global::totalAbsenceStage] " +
                                "Indicator %s failed to execute mongodb total absence aggregation pipeline", Global.class.getName()),
                        either.left().getValue());
                future.fail(either.left().getValue());
                return;
            }

            JsonArray result = either.right().getValue().getJsonObject("cursor").getJsonArray("firstBatch", new JsonArray());
            Map<String, Number> totalAbsMap = new HashMap<>();
            for (int i = 0; i < result.size(); i++) {
                JsonObject abs = result.getJsonObject(i);
                totalAbsMap.put(abs.getString("user"), abs.getInteger("count"));
            }

            search.setTotalAbsMap(totalAbsMap);
            future.complete(search);
        }));

        return future;
    }

    /**
     * Count statistic values
     *
     * @param search Search object containing filter and values
     * @return Future completing count
     */
    private Future<JsonObject> countValues(GlobalSearch search) {
        Promise<JsonObject> promise = Promise.promise();
        Future<Number> studentFuture = studentCount(search);
        Future<JsonObject> countFuture = countStatistics(search);
        CompositeFuture.all(studentFuture, countFuture)
                .onSuccess(ar -> {
                    JsonObject result = countFuture.result();
                    result.put("STUDENTS", studentFuture.result());
                    promise.complete(result);
                })
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Global::countValues] " +
                            "Indicator %s failed to count values", Global.class.getName()), fail.getCause());
                    promise.fail(fail.getCause());
                });

        return promise.future();
    }

    private Future<Number> globalAbsenceCount(GlobalSearch search) {
        Promise<Number> promise = Promise.promise();
        JsonObject request = commandObject(search.totalAbsenceGlobalPipeline());
        MongoDb.getInstance().command(request.toString(), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@Global::globalAbsenceCount] " +
                                "Indicator %s failed to execute mongodb global absence count aggregation pipeline", Global.class.getName()),
                        either.left().getValue());
                promise.fail(either.left().getValue());
                return;
            }

            JsonArray result = either.right().getValue().getJsonObject("cursor").getJsonArray("firstBatch", new JsonArray());
            if (result.isEmpty()) {
                promise.complete(0);
                return;
            }

            JsonObject stat = result.getJsonObject(0);
            promise.complete(stat.getInteger("count", 0));
        }));

        return promise.future();
    }

    private boolean isEmptyPrefetch(GlobalSearch search) {
        return (search.filter().from() != null || search.filter().to() != null) && search.filter().users().isEmpty();
    }

    private Future<Number> studentCount(GlobalSearch search) {
        Future<Number> future = Future.future();
        String query = "MATCH (u:User {profiles: ['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class)-[:BELONGS]->(s:Structure {id: {structure}}) " +
                "RETURN count(u) as count";
        JsonObject params = new JsonObject()
                .put("structure", search.filter().structure());
        Neo4j.getInstance().execute(query, params, Neo4jResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) {
                future.fail(either.left().getValue());
            } else {
                JsonObject result = either.right().getValue();
                future.complete(result.getInteger("count"));
            }
        }));

        return future;
    }

    @SuppressWarnings("unchecked")
    private Future<JsonObject> countStatistics(GlobalSearch search) {
        Promise<JsonObject> promise = Promise.promise();

        Future<JsonArray> basicEventTypedStatisticsFuture = retrieveStatistics(search.countBasicEventTypedPipeline());
        Future<JsonArray> absencesStatisticsFuture = retrieveStatistics(search.countAbsencesPipeline());

        CompositeFuture.all(basicEventTypedStatisticsFuture, absencesStatisticsFuture)
                .onSuccess(ar -> {
                    JsonObject result = new JsonObject();

                    ((List<JsonObject>) basicEventTypedStatisticsFuture.result().getList())
                            .forEach(statistic -> result.put(statistic.getString("type"), statistic.getInteger("count")));

                    ((List<JsonObject>) absencesStatisticsFuture.result().getList())
                            .forEach(statistic -> result.put(statistic.getString("type"), statistic.getInteger("count")));

                    promise.complete(result);
                })
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Global::countStatistics] " +
                            "Indicator %s failed to retrieve statistics count", Global.class.getName()), fail.getCause());
                    promise.fail(fail.getCause());
                });

        return promise.future();
    }


    /**
     * If filter contains to filter or from filter, skip this stage. First we need to retrieve statistics.
     * Otherwise fetch students based on GlobalSearch object.
     *
     * @param search Search object containing filter and values
     * @return Future completing stage
     */
    private Future<GlobalSearch> preStatisticsStage(GlobalSearch search) {
        Future<GlobalSearch> future = Future.future();
        if (search.filter().to() != null || search.filter().from() != null) {
            return prefetchUsers(search);
        }

        if (search.filter().audiences().isEmpty()) {
            searchUserInClass(search, search.filter().users(), future);
        } else {
            JsonObject params = new JsonObject()
                    .put("structure", search.filter().structure());

            // If search.filter().audiences() method does not return an empty list then search users based on provided audiences
            String query = "MATCH (s:Structure {id:{structure}})<-[:BELONGS|:DEPENDS]-(g)<-[:IN|DEPENDS*1..2]-(u:User {profiles: ['Student']}) " +
                    "WHERE (g:Class OR g:FunctionalGroup) " +
                    "AND g.id IN {audiences} " +
                    "RETURN (u.lastName + ' ' + u.firstName) as name, g.name as audience, u.id as id " +
                    "ORDER BY audience ASC, name ASC ";
            if (search.filter().page() != null) {
                query += "SKIP " + (search.filter().page() * PAGE_SIZE) + " LIMIT " + PAGE_SIZE;
            }

            params.put("audiences", search.filter().audiences());

            Neo4j.getInstance().execute(query, params, searchUserHandler(search, future));
        }

        return future;
    }

    /**
     * Execute prefetch user if filter contains from or to filter. Finally set users and unset page.
     *
     * @param search Search object containing filter and values
     * @return Future completing stage
     */
    private Future<GlobalSearch> prefetchUsers(GlobalSearch search) {
        Future<GlobalSearch> future = Future.future();
        JsonObject request = commandObject(search.prefetchUserPipeline());
        MongoDb.getInstance().command(request.toString(), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@Global::prefetchUsers] " +
                                "Indicator %s failed to execute prefetch user mongodb aggregation pipeline", Global.class.getName()),
                        either.left().getValue());
                future.fail(either.left().getValue());
                return;
            }

            JsonArray result = either.right().getValue().getJsonObject("cursor").getJsonArray("firstBatch", new JsonArray());
            List<String> users = ((List<JsonObject>) result.getList()).stream().map(user -> user.getString("_id")).collect(Collectors.toList());
            search.filter().setUsers(users)
                    .setPage(null);
            future.complete(search);
        }));

        return future;
    }

    /**
     * Execute mongodb aggregation pipeline to retrieve user statistics based on search filter object.
     *
     * @param search Search object containing filter and values
     * @return Future completing stage
     */
    private Future<GlobalSearch> statisticStage(GlobalSearch search) {
        if (isEmptyPrefetch(search)) {
            return Future.succeededFuture(search);
        }

        Promise<GlobalSearch> promise = Promise.promise();

        Future<JsonArray> basicEventTypedStatisticsFuture = retrieveStatistics(search.searchBasicEventTypedPipeline());
        Future<JsonArray> absencesStatisticsFuture = retrieveStatistics(search.searchAbsencesPipeline());

        CompositeFuture.all(basicEventTypedStatisticsFuture, absencesStatisticsFuture)
                .onSuccess(res -> {
                    Map<String, List<JsonObject>> basicEventTypedStatistics = mapPipelineResultByUser(basicEventTypedStatisticsFuture.result());
                    Map<String, List<JsonObject>> absencesStatistics = mapPipelineResultByUser(absencesStatisticsFuture.result());

                    Map<String, List<JsonObject>> statistics = Stream.of(basicEventTypedStatistics, absencesStatistics)
                            .flatMap(map -> map.entrySet().stream())
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (list1, list2) -> Stream.concat(list1.stream(), list2.stream()).collect(Collectors.toList())));
                    search.setStatistics(statistics);
                    promise.complete(search);
                })
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Global::statisticStage] " +
                            "Indicator %s failed to retrieve statistics", Global.class.getName()), fail.getCause());
                    promise.fail(fail.getCause());
                });

        return promise.future();
    }

    public Future<JsonArray> retrieveStatistics(JsonArray command) {
        Promise<JsonArray> promise = Promise.promise();
        if (command == null || command.isEmpty()) {
            promise.complete(new JsonArray());
            return promise.future();
        }

        JsonObject request = commandObject(command);
        MongoDb.getInstance().command(request.toString(), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@Global::retrieveStatistics] " +
                                "Indicator %s failed to execute mongodb aggregation pipeline", Global.class.getName()),
                        either.left().getValue());
                promise.fail(either.left().getValue());
                return;
            }
            JsonObject result = either.right().getValue();
            if (result.getJsonObject("cursor") == null) {
                String message = either.right().getValue().getString("errmsg");
                log.error(String.format("[StatisticsPresences@Global::retrieveStatistics] Indicator %s failed to execute " +
                        "mongodb aggregation pipeline. ", Global.class.getName()), message);
                promise.fail(message);
                return;
            }


            promise.complete(result.getJsonObject("cursor").getJsonArray("firstBatch", new JsonArray()));
        }));

        return promise.future();
    }


    /**
     * If filter does not contains to filter or from filter skip this step. preStatisticsStage retrieved students.
     * Otherwise based on statistics users fetch students information.
     *
     * @param search Search object containing filter and values
     * @return Future completing stage
     */
    private Future<GlobalSearch> postStatisticStage(GlobalSearch search) {
        Future<GlobalSearch> future = Future.future();
        if (search.filter().from() == null && search.filter().to() == null) {
            return Future.succeededFuture(search);
        }

        Map<String, List<JsonObject>> statistics = search.statistics();
        if (statistics == null) {
            search.setUsers(new ArrayList<>());
            return Future.succeededFuture(search);
        }

        List<String> users = new ArrayList<>(statistics.keySet());
        if (users.isEmpty()) return Future.succeededFuture(search);
        searchUserInClass(search, users, future);

        return future;
    }

    /**
     * Merge students and statistics based on retrieved values during previous stage.
     *
     * @param search Search object containing filter and values
     * @return Future completing stage
     */
    private Future<GlobalSearch> mergeStage(GlobalSearch search) {
        Map<String, List<JsonObject>> statistics = search.statistics();
        search.users().forEach(user -> {
            if (statistics.containsKey(user.id())) {
                GlobalValue userStat = new GlobalValue();
                statistics.get(user.id()).forEach(stat -> {
                    String type = stat.getString("type");
                    userStat.setValue(type, stat);
                });

                if (search.containsAbsence()) {
                    Number userTotalAbs = search.totalAbs(user.id());
                    if (userTotalAbs != null) {
                        JsonObject stat = new JsonObject()
                                .put("count", userTotalAbs);

                        userStat.setValue("ABSENCE_TOTAL", stat);
                    }
                }

                setMaxValue(userStat);
                user.setValue(userStat);
            }
        });

        return Future.succeededFuture(search);
    }

    private void setMaxValue(GlobalValue userStat) {
        Set<String> keys = userStat.keys();
        String maxValue = keys.iterator().next();
        if (maxValue != null) {

            for (String key : keys) {
                if ("ABSENCE_TOTAL".equals(key)) continue;

                if (userStat.value(key).getInteger("count") > userStat.value(maxValue).getInteger("count") || "ABSENCE_TOTAL".equals(maxValue)) {
                    maxValue = key;
                }
            }

            userStat.value(maxValue).put("max", true);
        }
    }

    /**
     * Loop through aggregation pipeline result and store values in a Map. The Map contains user
     * identifier as key and a List a JsonObject as value. Those JsonObjects are statistics objects extracted
     * by the aggregation pipeline.
     *
     * @param result Aggregation pipeline result
     * @return Map containing user identifier as key and an List of statistics as value
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<JsonObject>> mapPipelineResultByUser(JsonArray result) {
        return ((List<JsonObject>) result.getList()).stream()
                .collect(Collectors.groupingBy(stat -> stat.getString("user")));
    }

    /**
     * Search user in class. If users parameter is neither null nor empty, the request filter on users list
     *
     * @param search Search object containing filter and values
     * @param users  User list. It contains users identifiers. Used as a filter if not null and not empty
     * @param future Future completing current stage
     */
    private void searchUserInClass(GlobalSearch search, List<String> users, Future<GlobalSearch> future) {
        // If search.filter().audiences().isEmpty() method returns true then search for users
        // Warning: search.filter().users() could be fulfilled
        String query = "MATCH (u:User {profiles: ['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class)-[:BELONGS]->(s:Structure {id: {structure}}) " +
                (users != null && !users.isEmpty() ? "WHERE u.id IN {users} " : "") +
                "RETURN (u.lastName + ' ' + u.firstName) as name, c.name as audience, u.id as id " +
                "ORDER BY audience ASC, name ASC ";

        if (search.filter().page() != null) {
            query += "SKIP " + (search.filter().page() * PAGE_SIZE) + " LIMIT " + PAGE_SIZE;
        }

        JsonObject params = new JsonObject()
                .put("structure", search.filter().structure());
        if (users != null && !users.isEmpty()) params.put("users", users);

        Neo4j.getInstance().execute(query, params, searchUserHandler(search, future));
    }

    /**
     * Global user handler. It parse the response, cast objects as User object and store the new list into the search object.
     *
     * @param search Search object containing filter and values
     * @param future Future completing current stage
     * @return Handler storing users in search object
     */
    private Handler<Message<JsonObject>> searchUserHandler(GlobalSearch search, Future<GlobalSearch> future) {
        return Neo4jResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@Global::searchUserHandler] " +
                        "Indicator %s failed to retrieve users", Global.class.getName()), either.left().getValue());
                future.fail(either.left().getValue());
                return;
            }

            // Parse response and store it in search object as a list of user
            List<User> users = new LinkedList<>();
            JsonArray result = either.right().getValue();
            for (int i = 0; i < result.size(); i++) {
                JsonObject user = result.getJsonObject(i);
                users.add(new User(user.getString("id"), user.getString("name"), user.getString("audience")));
            }

            search.setUsers(users);
            future.complete(search);
        });
    }

    private JsonObject commandObject(JsonArray pipeline) {
        return new JsonObject()
                .put("aggregate", StatisticsPresences.COLLECTION)
                .put("allowDiskUse", true)
                .put("cursor", new JsonObject().put("batchSize", 2147483647))
                .put("pipeline", pipeline);
    }
}
