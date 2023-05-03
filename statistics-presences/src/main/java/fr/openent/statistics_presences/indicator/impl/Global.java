package fr.openent.statistics_presences.indicator.impl;

import fr.openent.presences.common.helper.*;
import fr.openent.presences.common.viescolaire.*;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.EventRecoveryDay;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.bean.User;
import fr.openent.statistics_presences.bean.global.GlobalSearch;
import fr.openent.statistics_presences.bean.global.GlobalValue;
import fr.openent.statistics_presences.indicator.Indicator;
import fr.openent.statistics_presences.indicator.IndicatorGeneric;
import fr.openent.statistics_presences.model.StatisticsFilter;
import fr.openent.statistics_presences.utils.*;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.neo4j.Neo4jResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
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
    public void search(StatisticsFilter filter, Handler<AsyncResult<JsonObject>> handler) {
        setSearchSettings(filter)
                .compose(this::searchProcess)
                .onComplete(handler);
    }

    @Override
    public void searchGraph(StatisticsFilter filter, Handler<AsyncResult<JsonObject>> handler) {
        throw new UnsupportedOperationException();
    }

    /**
     * set search and add settings to recover absences
     *
     * @param filter filter
     * @return search
     */
    private Future<GlobalSearch> setSearchSettings(StatisticsFilter filter) {
        Promise<GlobalSearch> promise = Promise.promise();
        GlobalSearch search = new GlobalSearch(filter);

        Future<JsonObject> settingsFuture = IndicatorGeneric.retrieveSettings(search.filter().structure());
        Future<JsonObject> slotsSettingsFuture = IndicatorGeneric.retrieveSlotsSettings(search.filter().structure());

        CompositeFuture.all(settingsFuture, slotsSettingsFuture)
                .compose(res -> {
                    search.setRecoveryMethod(settingsFuture.result().getString("event_recovery_method", EventRecoveryDay.HOUR.type()));
                    search.setHalfDay(slotsSettingsFuture.result().getString("end_of_half_day"));
                    return getNumberHalfDays(search);
                })
                .onSuccess(numberHalfDays -> {
                    search.setTotalHalfDays(numberHalfDays);
                    promise.complete(search);
                })
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Global::setSearchSettings] " +
                            "Indicator %s failed to retrieve settings. %s", Global.class.getName(), fail.getMessage()));
                    promise.fail(fail.getCause());
                });
        return promise.future();
    }

    private Future<JsonObject> searchProcess(GlobalSearch search) {
        Promise<JsonObject> promise = Promise.promise();

        Future<List<JsonObject>> valuesFuture = searchValues(search);
        Future<JsonObject> countFuture = countValues(search);
        Future<JsonObject> slotsFuture = countSlotsStatistics(search);
        Future<JsonObject> totalAbsFuture = globalAbsenceCount(search);
        CompositeFuture.all(valuesFuture, countFuture, totalAbsFuture, slotsFuture)
                .onSuccess(ar -> {
                    List<JsonObject> values = valuesFuture.result();
                    Number totalAbs = totalAbsFuture.result().getInteger(Field.COUNT, 0);
                    Number totalAbsSlots = totalAbsFuture.result().getInteger(Field.SLOTS, 0);
                    JsonObject slots = slotsFuture.result()
                            .put(Field.ABSENCE_TOTAL, totalAbsSlots);
                    JsonObject count = countFuture.result()
                            .put(Field.ABSENCE_TOTAL, totalAbs);

                    JsonObject response = new JsonObject()
                            .put(Field.DATA, values)
                            .put(Field.COUNT, count)
                            .put(Field.RATE, getAbsenceRates(count, search.totalHalfDays(), count.getInteger(Field.STUDENTS_CAPS)))
                            .put(Field.SLOTS, slots)
                            .put(Field.EVENT_RECOVERY_METHOD, search.recoveryMethod());

                    promise.complete(response);
                })
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Global::searchProcess] " +
                            "Indicator %s failed to complete search. %s", Global.class.getName(), fail.getMessage()));
                    promise.fail(fail.getCause());
                });
        return promise.future();
    }

    private JsonObject getAbsenceRates(JsonObject slots, Double totalHalfDays, int nbStudents) {
        List<String> absenceTypes = Arrays.asList(EventType.NO_REASON.name(), EventType.UNREGULARIZED.name(),
                EventType.REGULARIZED.name(), Field.ABSENCE_TOTAL);

        return new JsonObject(absenceTypes
                .stream()
                .collect(Collectors.toMap(type -> type, type -> getTotalComputedAbsenceRate(slots.getDouble(type, 0.0), totalHalfDays, nbStudents)))
        );
    }

    /**
     * Compute rules = (number of slots (absence type) / number of total half-day) * 100
     *
     * @param slotsAbsence  slots absences value
     * @param totalHalfDays total of half-day fetched
     * @return {@link double} result of absence type rate (e.g 2.456534 will be 2.46)
     */
    private double getComputedAbsenceRate(Double slotsAbsence, Double totalHalfDays) {
        double result = (slotsAbsence / totalHalfDays) * 100;
        return getValidRate(result);
    }

    private double getTotalComputedAbsenceRate(Double slotsAbsence, Double totalHalfDays, int nbStudents) {
        double result = ((slotsAbsence / totalHalfDays) * 100) / nbStudents;
        return getValidRate(result);
    }

    private double getValidRate(double rate) {
        if (Double.isInfinite(rate) || Double.isNaN(rate)) return 0.0;
        return BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_DOWN).doubleValue();
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
                                "Indicator %s failed to complete search values. %s", Global.class.getName(), ar.cause().getMessage()));
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
        mongoDb.command(request.toString(), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@Global::totalAbsenceStage] " +
                                "Indicator %s failed to execute mongodb total absence aggregation pipeline. %s", Global.class.getName(),
                        either.left().getValue()));
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
                            "Indicator %s failed to count values. %s", Global.class.getName(), fail.getMessage()));
                    promise.fail(fail.getCause());
                });

        return promise.future();
    }

    /**
     * Retrieve number of opened half days
     *
     * @param search search parameters
     * @return {@link Future} with the number as {@link Double}
     */
    private Future<Double> getNumberHalfDays(GlobalSearch search) {
        Promise<Double> promise = Promise.promise();
        Viescolaire.getInstance().getExclusionDays(search.filter().structure(), res -> {
            if (res.isLeft()) {
                promise.fail(res.left().getValue());
            } else {
                String startDate = DateHelper.getDateString(search.filter().start(), DateHelper.YEAR_MONTH_DAY);
                String endDate = DateHelper.getDateString(search.filter().end(), DateHelper.YEAR_MONTH_DAY);
                JsonArray excludedPeriods = res.right().getValue();
                List<LocalDate> dates = DateHelper.getDatesBetweenTwoDates(startDate, endDate);
                promise.complete(proceedIncrementHalfDays(excludedPeriods, dates, search.recoveryMethod()));
            }
        });

        return promise.future();
    }

    /**
     * With dates fetched, we count the number of opened half day
     * 'opened' meaning day when there are school active
     *
     * For example, a day will increment twice (1 morning, 1 afternoon).
     * A weekend day won't count but a wednesday will count only once
     * (we consider wednesday as a day when school only last the morning)
     *
     * @param excludedPeriods JsonArray with all excluded periods
     * @param dates           List of all dates fetched
     * @param recoveryMethod  recovery method ('HOUR' | 'HALF_DAY' | 'DAY')
     * @return {@link double} number of half day in total
     */
    private double proceedIncrementHalfDays(JsonArray excludedPeriods, List<LocalDate> dates, String recoveryMethod) {
        double numberOfHalfDays = 0.0;
        for (LocalDate date : dates) {
            // this condition checks if the date is either not a weekend day or not matching with excluded periods
            if (!date.getDayOfWeek().name().equals(DayOfWeek.SATURDAY.name()) &&
                    !date.getDayOfWeek().name().equals(DayOfWeek.SUNDAY.name()) && hasPeriodNoMatch(excludedPeriods, date)) {
                // if our date matches with a wednesday day, we won't count as half-day therefore we only increment once
                if (date.getDayOfWeek().name().equals(DayOfWeek.WEDNESDAY.name())) {
                    numberOfHalfDays++;
                } else {
                    numberOfHalfDays += 2;
                }
            }
        }
        if (EventRecoveryDay.DAY.type().equals(recoveryMethod)) {
            numberOfHalfDays = numberOfHalfDays / 2;
        }
        return BigDecimal.valueOf(numberOfHalfDays).setScale(2, RoundingMode.HALF_DOWN).doubleValue();
    }

    /**
     * Check with there is no match with a date and all excluded periods
     *
     * @param excludedPeriods JsonArray with all excluded periods
     * @param date            date to match
     * @return {@link boolean} {@code true} if there are no match
     * otherwise {@code false} if there are some match with excluded period
     */
    @SuppressWarnings("unchecked")
    private boolean hasPeriodNoMatch(JsonArray excludedPeriods, LocalDate date) {
        return ((List<JsonObject>) excludedPeriods.getList()).stream().noneMatch(period ->
                DateHelper.isDateBetween(date + " " + DateHelper.DEFAULT_END_TIME,
                        period.getString(Field.START_DATE), period.getString(Field.END_DATE))
        );
    }

    private Future<JsonObject> globalAbsenceCount(GlobalSearch search) {
        Promise<JsonObject> promise = Promise.promise();
        JsonObject request = commandObject(search.totalAbsenceGlobalPipeline());
        mongoDb.command(request.toString(), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@Global::globalAbsenceCount] " +
                                "Indicator %s failed to execute mongodb global absence count aggregation pipeline. %s", Global.class.getName(),
                        either.left().getValue()));
                promise.fail(either.left().getValue());
                return;
            }

            JsonArray result = either.right().getValue().getJsonObject(Field.CURSOR, new JsonObject())
                    .getJsonArray(Field.FIRSTBATCH, new JsonArray());
            if (result.isEmpty()) {
                promise.complete(new JsonObject());
                return;
            }
            promise.complete(result.getJsonObject(0));
        }));

        return promise.future();
    }

    private boolean isEmptyPrefetch(GlobalSearch search) {
        return (search.filter().from() != null || search.filter().to() != null) && search.filter().users().isEmpty();
    }

    private Future<Number> studentCount(GlobalSearch search) {
        if (search.filter().to() != null || search.filter().from() != null) return countUsersWithStatistics(search);
        if (search.filter().audiences().isEmpty()) return countUserInClass(search, search.filter().users());
        return countUsersFromProvidedAudiences(search);
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
                            "Indicator %s failed to retrieve statistics count. %s", Global.class.getName(), fail.getMessage()));
                    promise.fail(fail.getCause());
                });

        return promise.future();
    }

    @SuppressWarnings("unchecked")
    private Future<JsonObject> countSlotsStatistics(GlobalSearch search) {
        Promise<JsonObject> promise = Promise.promise();

        retrieveStatistics(search.countAbsencesPipeline())
                .onSuccess(res -> {
                    JsonObject result = new JsonObject();

                    ((List<JsonObject>) res.getList())
                            .forEach(statistic -> result.put(statistic.getString("type"), statistic.getInteger("slots")));

                    promise.complete(result);
                })
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Global::countSlotsStatistics] " +
                            "Indicator %s failed to retrieve statistics slots count. %s", Global.class.getName(), fail.getMessage()));
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
        if (search.filter().to() != null || search.filter().from() != null) return prefetchUsers(search);
        if (search.filter().audiences().isEmpty() && search.filter().users() != null) {
            return searchUserInClass(search, search.filter().users());
        }
        return getUsersFromProvidedAudiences(search);
    }

    private Future<GlobalSearch> getUsersFromProvidedAudiences(GlobalSearch search) {
        Promise<GlobalSearch> promise = Promise.promise();
        JsonObject params = new JsonObject();
        String query = matchUsersFromProvidedAudiencesQuery(search, params);
        query += "RETURN (u.lastName + ' ' + u.firstName) as name, g.name as audience, u.id as id " +
                "ORDER BY audience ASC, name ASC ";

        if (search.filter().page() != null)
            query += "SKIP " + (search.filter().page() * PAGE_SIZE) + " LIMIT " + PAGE_SIZE;

        neo4j.execute(query, params, searchUserHandler(search, promise));

        return promise.future();
    }

    private Future<Number> countUsersFromProvidedAudiences(GlobalSearch search) {
        Promise<Number> promise = Promise.promise();
        JsonObject params = new JsonObject();
        String query = matchUsersFromProvidedAudiencesQuery(search, params);
        query += "RETURN count(DISTINCT u.id) as count";
        neo4j.execute(query, params, countUserHandler(promise));
        return promise.future();
    }

    private String matchUsersFromProvidedAudiencesQuery(GlobalSearch search, JsonObject params) {
        String query = "MATCH (s:Structure {id:{structure}})<-[:BELONGS|:DEPENDS]-(g)<-[:IN|DEPENDS*1..2]-(u:User {profiles: ['Student']}) " +
                "WHERE (g:Class OR g:FunctionalGroup) " +
                "AND g.id IN {audiences} ";

        params.put(Field.STRUCTURE, search.filter().structure())
                .put(Field.AUDIENCES, search.filter().audiences());

        return query;
    }

    /**
     * Execute prefetch user if filter contains from or to filter. Finally set users and unset page.
     *
     * @param search Search object containing filter and values
     * @return Future completing stage
     */
    @SuppressWarnings("unchecked")
    private Future<GlobalSearch> prefetchUsers(GlobalSearch search) {
        Future<GlobalSearch> future = Future.future();
        JsonObject request = commandObject(search.prefetchUserPipeline());
        mongoDb.command(request.toString(), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@Global::prefetchUsers] " +
                                "Indicator %s failed to execute prefetch user mongodb aggregation pipeline. %s", Global.class.getName(),
                        either.left().getValue()));
                future.fail(either.left().getValue());
                return;
            }

            JsonArray result = either.right().getValue().getJsonObject(Field.CURSOR).getJsonArray(Field.FIRSTBATCH, new JsonArray());
            List<String> users = ((List<JsonObject>) result.getList()).stream().map(user -> user.getString(Field._ID)).collect(Collectors.toList());
            search.filter().setUsers(users)
                    .setPage(null);
            future.complete(search);
        }));

        return future;
    }

    private Future<Number> countUsersWithStatistics(GlobalSearch search) {
        Promise<Number> promise = Promise.promise();
        JsonObject request = commandObject(search.countUsersWithStatisticsPipeline());
        mongoDb.command(request.toString(), MongoDbResult.validResultHandler(FutureHelper.handlerJsonObject(res -> {
            if (res.failed()) {
                log.error(String.format("[StatisticsPresences@Global::prefetchUsers] " +
                                "Indicator %s failed to execute prefetch user mongodb aggregation pipeline. %s", Global.class.getName(),
                        res.cause().getMessage()));
                promise.fail(res.cause());
            }
            else {
                JsonArray countArray = res.result().getJsonObject(Field.CURSOR).getJsonArray(Field.FIRSTBATCH, new JsonArray());
                if (countArray.isEmpty()) {
                    promise.complete(0);
                } else {
                    promise.complete(countArray.getJsonObject(0).getInteger(Field.COUNT, 0));
                }
            }
        })));

        return promise.future();
    }

    /**
     * Execute mongodb aggregation pipeline to retrieve user statistics based on search filter object.
     *
     * @param search Search object containing filter and values
     * @return Future completing stage
     */
    @SuppressWarnings("unchecked")
    private Future<GlobalSearch> statisticStage(GlobalSearch search) {
        if (isEmptyPrefetch(search)) {
            return Future.succeededFuture(search);
        }

        Promise<GlobalSearch> promise = Promise.promise();

        Future<JsonArray> basicEventTypedStatisticsFuture = retrieveStatistics(search.searchBasicEventTypedPipeline());
        Future<JsonArray> absencesStatisticsFuture = retrieveStatistics(search.searchAbsencesPipeline());

        CompositeFuture.all(basicEventTypedStatisticsFuture, absencesStatisticsFuture)
                .onSuccess(res -> {

                    // we fetch all absences types stats result, and we add the "rate" object
                    JsonArray absencesAddRateStats = new JsonArray(((List<JsonObject>) absencesStatisticsFuture.result().getList())
                            .stream()
                            .map(a -> a.put(Field.RATE, getComputedAbsenceRate(a.getDouble(Field.COUNT, 0.0), search.totalHalfDays())))
                            .collect(Collectors.toList()));

                    Map<String, List<JsonObject>> basicEventTypedStatistics = mapPipelineResultByUser(basicEventTypedStatisticsFuture.result());
                    Map<String, List<JsonObject>> absencesStatistics = mapPipelineResultByUser(absencesAddRateStats);

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
                            "Indicator %s failed to retrieve statistics. %s", Global.class.getName(), fail.getMessage()));
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
        mongoDb.command(request.toString(), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@Global::retrieveStatistics] " +
                                "Indicator %s failed to execute mongodb aggregation pipeline. %s", Global.class.getName(),
                        either.left().getValue()));
                promise.fail(either.left().getValue());
                return;
            }
            JsonObject result = either.right().getValue();
            if (result.getJsonObject("cursor") == null) {
                String message = either.right().getValue().getString(Field.ERRMSG, "");
                log.error(String.format("[StatisticsPresences@Global::retrieveStatistics] Indicator %s failed to execute " +
                        "mongodb aggregation pipeline. %s %s", Global.class.getName(), message, request));
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
        return searchUserInClass(search, users);
    }

    /**
     * Merge students and statistics (count, rate...) based on retrieved values during previous stage.
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
                    String type = stat.getString(Field.TYPE);
                    userStat.setValue(type, stat);
                });

                if (search.containsAbsence()) {
                    Number userTotalAbs = search.totalAbs(user.id());
                    if (userTotalAbs != null) {
                        JsonObject stat = new JsonObject()
                                .put(Field.COUNT, userTotalAbs)
                                .put(Field.RATE, getComputedAbsenceRate(userTotalAbs.doubleValue(), search.totalHalfDays()));
                        userStat.setValue(Field.ABSENCE_TOTAL, stat);
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

    private String matchUserInClassQuery(GlobalSearch search, List<String> users, JsonObject params) {
        String query = "MATCH (u:User {profiles: ['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class)-[:BELONGS]->(s:Structure {id: {structure}}) " +
                (users != null && !users.isEmpty() ? "WHERE u.id IN {users} " : "");

        params.put(Field.STRUCTURE, search.filter().structure());
        if (users != null && !users.isEmpty()) params.put(Field.USERS, users);

        return query;
    }

    /**
     * Search user in class. If users parameter is neither null nor empty, the request filter on users list
     *
     * @param search Search object containing filter and values
     * @param users  User list. It contains users identifiers. Used as a filter if not null and not empty
     * @return Future completing current stage
     */
    private Future<GlobalSearch> searchUserInClass(GlobalSearch search, List<String> users) {
        // If search.filter().audiences().isEmpty() method returns true then search for users
        // Warning: search.filter().users() could be fulfilled
        Promise<GlobalSearch> promise = Promise.promise();
        JsonObject params = new JsonObject();
        String query = matchUserInClassQuery(search, users, params);
        query += "RETURN (u.lastName + ' ' + u.firstName) as name, c.name as audience, u.id as id " +
                "ORDER BY audience ASC, name ASC ";

        if (search.filter().page() != null)
            query += "SKIP " + (search.filter().page() * PAGE_SIZE) + " LIMIT " + PAGE_SIZE;

        neo4j.execute(query, params, searchUserHandler(search, promise));

        return promise.future();
    }

    private Future<Number> countUserInClass(GlobalSearch search, List<String> users) {
        Promise<Number> promise = Promise.promise();
        // If search.filter().audiences().isEmpty() method returns true then search for users
        // Warning: search.filter().users() could be fulfilled
        JsonObject params = new JsonObject();
        String query = matchUserInClassQuery(search, users, params);
        query += "RETURN count(DISTINCT u.id) as count";
        neo4j.execute(query, params, countUserHandler(promise));
        return promise.future();
    }


    /**
     * Global user handler. It parse the response, cast objects as User object and store the new list into the search object.
     *
     * @param search  Search object containing filter and values
     * @param promise Future completing current stage
     * @return Handler storing users in search object
     */
    private Handler<Message<JsonObject>> searchUserHandler(GlobalSearch search, Promise<GlobalSearch> promise) {
        return Neo4jResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@Global::searchUserHandler] " +
                        "Indicator %s failed to retrieve users. %s", Global.class.getName(), either.left().getValue()));
                promise.fail(either.left().getValue());
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
            promise.complete(search);
        });
    }

    private Handler<Message<JsonObject>> countUserHandler(Promise<Number> promise) {
        return Neo4jResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(res -> {
            if (res.failed()) promise.fail(res.cause().getMessage());
            else promise.complete(res.result().getInteger(Field.COUNT));
        }));
    }

    private JsonObject commandObject(JsonArray pipeline) {
        return new JsonObject()
                .put("aggregate", StatisticsPresences.COLLECTION)
                .put("allowDiskUse", true)
                .put("cursor", new JsonObject().put("batchSize", 2147483647))
                .put("pipeline", pipeline);
    }
}
