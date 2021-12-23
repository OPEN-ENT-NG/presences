package fr.openent.statistics_presences.indicator.impl;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.bean.Audience;
import fr.openent.statistics_presences.bean.monthly.*;
import fr.openent.statistics_presences.helper.MonthlyHelper;
import fr.openent.statistics_presences.indicator.Indicator;
import fr.openent.statistics_presences.indicator.IndicatorGeneric;
import fr.openent.statistics_presences.model.StatisticsFilter;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.neo4j.Neo4jResult;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Monthly extends Indicator {
    public static final Integer PAGE_SIZE = 35;
    private final Logger log = LoggerFactory.getLogger(Monthly.class);

    public Monthly(Vertx vertx, String name) {
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
        setSearchSettings(filter)
                .compose(res -> {
                    MonthlyGraphSearch graphSearch = new MonthlyGraphSearch(res);
                    return searchGraphProcess(graphSearch);
                })
                .onComplete(handler);
    }

    /**
     * set search and add settings to recover absences
     *
     * @param filter filter
     * @return search
     */
    private Future<MonthlySearch> setSearchSettings(StatisticsFilter filter) {
        Promise<MonthlySearch> promise = Promise.promise();
        MonthlySearch search = new MonthlySearch(filter);

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
                            "Indicator %s failed to retrieve settings", Monthly.class.getName()), fail.getCause());
                    promise.fail(fail.getCause());
                });
        return promise.future();
    }

    /**
     * Search Monthly process
     *
     * @param search Search object containing filter and values {@link MonthlySearch}
     * @return Future {@link Future<JsonObject> } completing search values
     */
    private Future<JsonObject> searchProcess(MonthlySearch search) {
        Promise<JsonObject> promise = Promise.promise();
        searchValues(search)
                .onSuccess(result -> {
                    JsonObject response = new JsonObject()
                            .put("data", result);

                    promise.complete(response);
                })
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Monthly::searchProcess] " +
                            "Indicator %s failed to complete search", Monthly.class.getName()), fail.getCause());
                    promise.fail(fail.getCause());
                });
        return promise.future();
    }

    /**
     * Search Graph Monthly process
     *
     * @param search Search object containing filter and values {@link MonthlyGraphSearch}
     * @return Future {@link Future<JsonObject> } completing search values
     */
    private Future<JsonObject> searchGraphProcess(MonthlyGraphSearch search) {
        Promise<JsonObject> promise = Promise.promise();
        searchGraphValues(search)
                .onSuccess(promise::complete)
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Monthly::searchGraphProcess] " +
                            "Indicator %s failed to complete search", Monthly.class.getName()), fail.getCause());
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
    private Future<List<JsonObject>> searchValues(MonthlySearch search) {
        Promise<List<JsonObject>> promise = Promise.promise();
        preStatisticsStage(search)
                .compose(this::statisticStage)
                .compose(this::mergeStage)
                .onSuccess(ar -> promise.handle(
                        Future.succeededFuture(search.audienceResult().stream()
                                .map(AudienceMap::toJson)
                                .collect(Collectors.toList())))
                )
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Monthly::searchValues] " +
                            "Indicator %s failed to complete search values ", Monthly.class.getName()), fail.getCause().getMessage());
                    promise.handle(Future.failedFuture(fail.getCause()));
                });

        return promise.future();
    }

    /**
     * Search for graph statistic values
     *
     * @param search Search object containing filter and values {@link MonthlyGraphSearch}
     * @return Future {@link Future} of {@link <List<JsonObject>} completing search values
     */
    private Future<JsonObject> searchGraphValues(MonthlyGraphSearch search) {
        Promise<JsonObject> promise = Promise.promise();
        statisticGraphStage(search)
                .compose(this::mergeGraphStage)
                .onSuccess(ar -> promise.handle(Future.succeededFuture(ar)))
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Monthly::searchValues] " +
                            "Indicator %s failed to complete search values ", Monthly.class.getName()), fail.getCause().getMessage());
                    promise.handle(Future.failedFuture(fail.getCause()));
                });

        return promise.future();
    }

    private boolean isEmptyPrefetch(MonthlySearch search) {
        return (search.filter().from() != null || search.filter().to() != null) && search.filter().users().isEmpty();
    }

    private boolean isGraphEmptyPrefetch(MonthlyGraphSearch search) {
        return (search.filter().from() != null || search.filter().to() != null) &&
                (search.filter().users().isEmpty() || search.filter().audiences().isEmpty());
    }


    /**
     * If filter contains to filter or from filter, skip this stage. First we need to retrieve statistics.
     * Otherwise fetch students based on GlobalSearch object.
     *
     * @param search Search object containing filter and values
     * @return Future completing stage
     */
    private Future<MonthlySearch> preStatisticsStage(MonthlySearch search) {
        Promise<MonthlySearch> promise = Promise.promise();
        searchClasses(search, search.filter().audiences(), promise);
        return promise.future();
    }

    /**
     * Execute mongodb aggregation pipeline to retrieve user statistics based on search filter object.
     *
     * @param search Search object containing filter and values
     * @return Future {@link Future<MonthlySearch>} completing stage
     */
    private Future<MonthlySearch> statisticStage(MonthlySearch search) {
        if (isEmptyPrefetch(search)) {
            return Future.succeededFuture(search);
        }

        Promise<MonthlySearch> promise = Promise.promise();

        Future<JsonArray> audienceBasicEventTypedStatisticsFuture = retrieveStatistics(search.searchBasicEventTypedByAudiencePipeline());
        Future<JsonArray> audienceAbsencesStatisticsFuture = retrieveStatistics(search.searchAbsencesByAudiencePipeline());
        Future<JsonArray> studentBasicEventTypedStatisticsFuture = retrieveStatistics(search.searchBasicEventTypedByStudentPipeline());
        Future<JsonArray> studentAbsencesStatisticsFuture = retrieveStatistics(search.searchAbsencesByStudentPipeline());

        CompositeFuture.all(audienceBasicEventTypedStatisticsFuture, audienceAbsencesStatisticsFuture,
                studentBasicEventTypedStatisticsFuture, studentAbsencesStatisticsFuture)
                .onSuccess(res -> {

                    Map<String, List<Month>> basicEventTypedStatistics = duplicateMonthsStatisticsDataMultipleClass(
                            MonthlyHelper.mapPipelineResultByKeyAndMonth(audienceBasicEventTypedStatisticsFuture.result())
                    );

                    Map<String, List<Month>> absencesStatistics = duplicateMonthsStatisticsDataMultipleClass(
                            MonthlyHelper.mapPipelineResultByKeyAndMonth(audienceAbsencesStatisticsFuture.result())
                    );

                    Map<String, List<Student>> studentBasicEventTypedStatistics = duplicateStudentsStatisticsDataMultipleClass(
                            MonthlyHelper.mapPipelineResultByAudienceAndStudent(studentBasicEventTypedStatisticsFuture.result())
                    );

                    Map<String, List<Student>> studentAbsencesStatistics = duplicateStudentsStatisticsDataMultipleClass(
                            MonthlyHelper.mapPipelineResultByAudienceAndStudent(studentAbsencesStatisticsFuture.result())
                    );

                    Map<String, List<Month>> statistics = Stream.of(basicEventTypedStatistics, absencesStatistics)
                            .flatMap(map -> map.entrySet().stream())
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    MonthlyHelper::concatMonths
                            ));

                    Map<String, List<Student>> students = Stream.of(studentBasicEventTypedStatistics, studentAbsencesStatistics)
                            .flatMap(map -> map.entrySet().stream())
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    MonthlyHelper::concatStudents
                            ));

                    search.setStatistics(statistics);
                    search.setStudents(students);
                    promise.complete(search);
                })
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Monthly::statisticStage] " +
                            "Indicator %s failed to retrieve statistics", Monthly.class.getName()), fail.getCause());
                    promise.fail(fail.getCause());
                });

        return promise.future();
    }

    /**
     * Adding a {@link Map} in parameter will find class_name's field multiple argument (comma separator)
     * and create a new independant class map (and potentially CONCAT VALUE {@link Month}
     * if it encounters the same class map by its key)
     * <p>
     * (e.g  Map of {"3EME1", "3EME2,3EME1"} will become {"3EME1", "3EME2", "3EME2,3EME1" (still keeping its unused reference)}
     *
     * @param statistics statistics month where we will append data
     * @return {@link Map} of {@link String} {@link List<Month>}
     */
    private Map<String, List<Month>> duplicateMonthsStatisticsDataMultipleClass(Map<String, List<Month>> statistics) {
        Map<String, List<Month>> duplicateClassesStatistics = new HashMap<>();

        for (Map.Entry<String, List<Month>> entry : statistics.entrySet()) {
            String[] classesKey = entry.getKey().split(",");
            if (classesKey.length > 1) {
                List<Month> classesStatistics = statistics.get(entry.getKey());
                for (String classkey : classesKey) {
                    duplicateClassesStatistics.put(classkey, classesStatistics);
                }
            }
        }

        if (duplicateClassesStatistics.isEmpty()) return statistics;

        return Stream.of(statistics, duplicateClassesStatistics)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        MonthlyHelper::concatMonths
                ));
    }

    /**
     * Adding a {@link Map} in parameter will find class_name's field multiple argument (comma separator)
     * and create a new independant class map (and potentially CONCAT VALUE {@link Student}
     * if it encounters the same class map by its key)
     * <p>
     * (e.g  Map of {"3EME1", "3EME2,3EME1"} will become {"3EME1", "3EME2", "3EME2,3EME1" (still keeping its unused reference)}
     *
     * @param statistics statistics month where we will append data
     * @return {@link Map} of {@link String} {@link List<Student>}
     */
    private Map<String, List<Student>> duplicateStudentsStatisticsDataMultipleClass(Map<String, List<Student>> statistics) {
        Map<String, List<Student>> duplicateClassesStatistics = new HashMap<>();

        for (Map.Entry<String, List<Student>> entry : statistics.entrySet()) {
            String[] classesKey = entry.getKey().split(",");
            if (classesKey.length > 1) {
                List<Student> classesStatistics = statistics.get(entry.getKey());
                for (String classkey : classesKey) {
                    duplicateClassesStatistics.put(classkey, classesStatistics);
                }
            }
        }

        if (duplicateClassesStatistics.isEmpty()) return statistics;

        return Stream.of(statistics, duplicateClassesStatistics)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        MonthlyHelper::concatStudents
                ));
    }

    /**
     * Execute mongodb aggregation pipeline to retrieve graph data
     *
     * @param search Search object containing filter and values {@link MonthlyGraphSearch}
     * @return Future {@link Future<MonthlyGraphSearch>} completing stage
     */
    private Future<MonthlyGraphSearch> statisticGraphStage(MonthlyGraphSearch search) {
        if (isGraphEmptyPrefetch(search)) {
            return Future.succeededFuture(search);
        }

        Promise<MonthlyGraphSearch> promise = Promise.promise();

        Future<JsonArray> groupedBasicEventTypeStatisticsFuture = retrieveStatistics(search.searchGroupedBasicEvenTypePipeline());
        Future<JsonArray> groupedAbsencesStatisticsFuture = retrieveStatistics(search.searchGroupedAbsencesPipeline());

        CompositeFuture.all(groupedBasicEventTypeStatisticsFuture, groupedAbsencesStatisticsFuture)
                .onSuccess(res -> {
                    Map<String, List<Month>> basicEventTypedStatistics = MonthlyHelper.mapPipelineResultEventByGroup(
                            groupedBasicEventTypeStatisticsFuture.result(), "type");

                    Map<String, List<Month>> absencesStatistics = MonthlyHelper.mapPipelineResultEventByGroup(
                            groupedAbsencesStatisticsFuture.result(), "type");

                    Map<String, List<Month>> statistics = Stream.of(basicEventTypedStatistics, absencesStatistics)
                            .flatMap(map -> map.entrySet().stream())
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    MonthlyHelper::concatMonths
                            ));
                    search.setStatistics(statistics);
                    promise.complete(search);
                })
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Monthly::statisticGraphStage] " +
                            "Indicator %s failed to retrieve statistics", Monthly.class.getName()), fail.getCause());
                    promise.fail(fail.getCause());
                });

        return promise.future();
    }

    private Future<JsonArray> retrieveStatistics(JsonArray command) {
        Promise<JsonArray> promise = Promise.promise();
        if (command == null || command.isEmpty()) {
            promise.complete(new JsonArray());
            return promise.future();
        }

        JsonObject request = commandObject(command);
        mongoDb.command(request.toString(), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@Monthly::retrieveStatistics] " +
                                "Indicator %s failed to execute mongodb aggregation pipeline", Monthly.class.getName()),
                        either.left().getValue());
                promise.fail(either.left().getValue());
                return;
            }
            JsonObject result = either.right().getValue();
            if (result.getJsonObject("cursor") == null) {
                String message = either.right().getValue().getString("errmsg");
                log.error(String.format("[StatisticsPresences@Monthly::retrieveStatistics] Indicator %s failed to execute " +
                        "mongodb aggregation pipeline. ", Monthly.class.getName()), message);
                promise.fail(message);
                return;
            }


            promise.complete(result.getJsonObject("cursor").getJsonArray("firstBatch", new JsonArray()));
        }));

        return promise.future();
    }

    /**
     * Merge students and statistics based on retrieved values during previous stage.
     *
     * @param search Search object containing filter and values
     * @return {@link Future<MonthlySearch>} completing stage
     */
    private Future<MonthlySearch> mergeStage(MonthlySearch search) {
        Map<String, List<Month>> statistics = search.statistics();
        Map<String, List<Student>> students = search.students();
        LocalDate startAt = LocalDate.parse(DateHelper.getDateString(search.filter().start(), DateHelper.YEAR_MONTH_DAY));

        // adding 1 to include date itself (e.g filtering start(2021/01) and end(2021/01)
        // will return 1 instead of 0 in order to still "count";
        // then start(2021/01) and end(2021/02) will return 2, start(2021/01) and end(2021/03) will return 3 etc...
        long numOfMonthsBetween = IntStream
                .range(0, (int) DateHelper.distinctMonthsNumberSeparating(search.filter().start(), search.filter().end()) + 1)
                .toArray().length;

        List<AudienceMap> audiences = search.audiences().stream()
                .map(audience -> {
                    List<Student> currentStudents = findCurrentStudents(students, audience);
                    List<Month> months = MonthlyHelper.concatMonths(
                            MonthlyHelper.initMonths(startAt, numOfMonthsBetween),
                            findStatistics(statistics, audience))
                            .stream()
                            .sorted(Comparator.comparing(Month::key))
                            .collect(Collectors.toList());
                    setMaxValue(months);
                    if (currentStudents != null && !currentStudents.isEmpty()) {
                        currentStudents.forEach(student -> {
                            setMaxValue(student.months());
                            Number totalCountStudent = student.months().stream().map(m -> m.statistic().count()).mapToInt(Integer::intValue).sum();
                            student.setTotal(totalCountStudent);
                        });
                    }
                    Number totalCountInMonth = months.stream().map(m -> m.statistic().count()).mapToInt(Integer::intValue).sum();
                    return new AudienceMap(audience.name(), months, currentStudents, totalCountInMonth);
                })
                .collect(Collectors.toList());

        search.setAudienceResult(audiences);
        return Future.succeededFuture(search);
    }

    /**
     * this method will attempt to find the key of {@link List<Student>} with audience {@link Audience}
     * the map can contain a string with a comma separator (e.g "3EME1, 3EME2") and our audience can only have 1 name
     * in order to make this string "match", we loop through its entryKey until we find the correct map
     *
     * @param students the map we will attempt to access at its value {@link Map} of {@link List<Student>} as {@link String} as key
     * @param audience the object with its name field we want to make it match {@link Audience}
     * @return result of a value  {@link List<Student>}
     */
    private List<Student> findCurrentStudents(Map<String, List<Student>> students, Audience audience) {
        for (Map.Entry<String, List<Student>> entry : students.entrySet()) {
            for (String classkey : entry.getKey().split(",")) {
                if (classkey.equals(audience.name())) {
                    return students.get(entry.getKey());
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * this method will attempt to find the key of {@link List<Month>} with audience {@link Audience}
     * the map can contain a string with a comma separator (e.g "3EME1, 3EME2") and our audience can only have 1 name
     * in order to make this string "match", we loop through its entryKey until we find the correct map
     *
     * @param statistics the map we will attempt to access at its value {@link Map} of {@link List<Month>} as {@link String} as key
     * @param audience   the object with its name field we want to make it match {@link Audience}
     * @return result of a value  {@link List<Student>}
     */
    private List<Month> findStatistics(Map<String, List<Month>> statistics, Audience audience) {
        for (Map.Entry<String, List<Month>> entry : statistics.entrySet()) {
            for (String classkey : entry.getKey().split(",")) {
                if (classkey.equals(audience.name())) {
                    return statistics.get(entry.getKey());
                }
            }
        }
        return new ArrayList<>();
    }


    /**
     * Merge students and statistics based on retrieved values during previous stage.
     *
     * @param search Search object containing filter and values {@link MonthlyGraphSearch}
     * @return Future {@link Future<MonthlyGraphSearch>} completing stage
     */
    private Future<JsonObject> mergeGraphStage(MonthlyGraphSearch search) {
        Promise<JsonObject> promise = Promise.promise();

        Map<String, List<Month>> statistics = search.statistics();
        LocalDate startAt = LocalDate.parse(DateHelper.getDateString(search.filter().start(), DateHelper.YEAR_MONTH_DAY));

        // adding 1 to include date itself (e.g filtering start(2021/01) and end(2021/01)
        // will return 1 instead of 0 in order to still "count";
        // then start(2021/01) and end(2021/02) will return 2, start(2021/01) and end(2021/03) will return 3 etc...
        long numOfMonthsBetween = IntStream
                .range(0, (int) DateHelper.distinctMonthsNumberSeparating(search.filter().start(), search.filter().end()) + 1)
                .toArray().length;
        List<Month> listMonthsDefault = MonthlyHelper.initMonths(startAt, numOfMonthsBetween);
        JsonObject formatGraphStatistics = new JsonObject().put("data", new JsonObject());

        statistics.forEach((key, value) -> {
            List<Month> months = MonthlyHelper.concatMonths(listMonthsDefault, value)
                    .stream()
                    .sorted(Comparator.comparing(Month::key))
                    .collect(Collectors.toList());
            formatGraphStatistics.getJsonObject("data").put(key, months.stream().map(Month::toJson).collect(Collectors.toList()));
        });

        formatGraphStatistics.put("months", listMonthsDefault.stream().map(Month::key).collect(Collectors.toList()));


        promise.complete(formatGraphStatistics);

        return promise.future();
    }

    private void setMaxValue(List<Month> months) {
        months.stream()
                .max(Comparator.comparing(monthA -> monthA.statistic().count()))
                .ifPresent(month -> month.statistic().setMax(true));
    }

    /**
     * Search class.
     *
     * @param search  Search object containing filter and values
     * @param promise Future completing current stage
     */
    private void searchClasses(MonthlySearch search, List<String> audienceIds, Promise<MonthlySearch> promise) {
        String query = "MATCH (c:Class)-[:BELONGS]->(s:Structure {id: {structure}}) " +
                "WHERE (c:Class OR c:FunctionalGroup) ";
        JsonObject params = new JsonObject()
                .put("structure", search.filter().structure());

        if (audienceIds != null && !audienceIds.isEmpty()) {
            query += "AND c.id IN {audiences} ";
            params.put("audiences", audienceIds);
        }

        query += "RETURN c.name as audience_name ORDER BY audience_name ASC ";

        if (search.filter().page() != null)
            query += "SKIP " + (search.filter().page() * PAGE_SIZE) + " LIMIT " + PAGE_SIZE;


        neo4j.execute(query, params, searchClassHandler(search, promise));
    }

    /**
     * Global audience handler. It parse the response, cast objects as Audiences object and store the new list into the search object.
     *
     * @param search  Search object containing filter and values
     * @param promise Future completing current stage
     * @return Handler storing classes in search object
     */
    @SuppressWarnings("unchecked")
    private Handler<Message<JsonObject>> searchClassHandler(MonthlySearch search, Promise<MonthlySearch> promise) {
        return Neo4jResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@Monthly::searchClassHandler] " +
                        "Indicator %s failed to retrieve classes", Monthly.class.getName()), either.left().getValue());
                promise.fail(either.left().getValue());
                return;
            }

            List<JsonObject> result = either.right().getValue().getList();
            List<Audience> audiences = result.stream()
                    .map(audience -> new Audience(audience.getString("audience_name")))
                    .collect(Collectors.toList());
            search.setAudiences(audiences);
            promise.complete(search);
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
