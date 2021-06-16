package fr.openent.statistics_presences.indicator.impl;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.bean.Audience;
import fr.openent.statistics_presences.bean.monthly.*;
import fr.openent.statistics_presences.filter.Filter;
import fr.openent.statistics_presences.helper.MonthlyHelper;
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
    private Future<MonthlySearch> setSearchSettings(Filter filter) {
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


    private boolean isEmptyPrefetch(MonthlySearch search) {
        return (search.filter().from() != null || search.filter().to() != null) && search.filter().users().isEmpty();
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
                    Map<String, List<Month>> basicEventTypedStatistics = mapPipelineResultByKeyAndMonth(
                            audienceBasicEventTypedStatisticsFuture.result());

                    Map<String, List<Month>> absencesStatistics = mapPipelineResultByKeyAndMonth(
                            audienceAbsencesStatisticsFuture.result());

                    Map<String, List<Student>> studentBasicEventTypedStatistics = mapPipelineResultByAudienceAndStudent(
                            studentBasicEventTypedStatisticsFuture.result());

                    Map<String, List<Student>> studentAbsencesStatistics = mapPipelineResultByAudienceAndStudent(
                            studentAbsencesStatisticsFuture.result());


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
                                    this::concatStudents
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

    public Future<JsonArray> retrieveStatistics(JsonArray command) {
        Promise<JsonArray> promise = Promise.promise();
        if (command == null || command.isEmpty()) {
            promise.complete(new JsonArray());
            return promise.future();
        }

        JsonObject request = commandObject(command);
        MongoDb.getInstance().command(request.toString(), MongoDbResult.validResultHandler(either -> {
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
                    List<Student> currentStudents = students.get(audience.name());
                    List<Month> months = MonthlyHelper.concatMonths(
                            MonthlyHelper.initMonths(startAt, numOfMonthsBetween),
                            statistics.getOrDefault(audience.name(), new ArrayList<>()))
                            .stream()
                            .sorted(Comparator.comparing(Month::key))
                            .collect(Collectors.toList());
                    setMaxValue(months);
                    if (currentStudents != null) {
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

    private void setMaxValue(List<Month> months) {
        months.stream()
                .max(Comparator.comparing(monthA -> monthA.statistic().count()))
                .ifPresent(month -> month.statistic().setMax(true));
    }

    /**
     * Loop through aggregation pipeline result and store values in a Map. The Map contains audience name
     * identifier as key, containing a map of month for value. This map of month contains the mont (String) as key
     * and JsonObject as value. Those JsonObjects are statistics objects extracted
     * by the aggregation pipeline.
     *
     * @param result Aggregation pipeline result
     * @return Map containing user identifier as key and an List of statistics as value
     * {@link Map} of {@link String} as key and {@link List<Month>} as value
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<Month>> mapPipelineResultByKeyAndMonth(JsonArray result) {
        return ((List<JsonObject>) result.getList()).stream()
                .collect(Collectors.toMap(
                        stat -> stat.getString("class_name"),
                        this::mapObjectToMonthsList,
                        MonthlyHelper::concatMonths
                ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<Student>> mapPipelineResultByAudienceAndStudent(JsonArray result) {
        return ((List<JsonObject>) result.getList()).stream()
                .collect(Collectors.toMap(
                        stat -> stat.getString("class_name"),
                        this::mapObjectToStudentList,
                        this::concatStudents
                ));
    }

    private List<Student> concatStudents(List<Student> students1, List<Student> students2) {
        return new ArrayList<>(
                Stream.of(students1, students2)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toMap(
                                Student::id,
                                student -> student,
                                (stud1, stud2) -> new Student(stud1.name(), stud1.id(), MonthlyHelper.concatMonths(stud1.months(), stud2.months())
                                        .stream()
                                        .sorted(Comparator.comparing(Month::key))
                                        .collect(Collectors.toList()))
                        ))
                        .values()
        );
    }

    private List<Student> mapObjectToStudentList(JsonObject stat) {
        return Collections.singletonList(new Student(stat.getString("name"), stat.getString("user"), mapObjectToMonthsList(stat)));
    }

    private List<Month> mapObjectToMonthsList(JsonObject stat) {
        return Collections.singletonList(new Month(stat.getString("month"), setStatisticValues2(stat)));
    }

    private Statistic setStatisticValues2(JsonObject stat) {
        return new Statistic(
                stat.getInteger("count", 0),
                stat.getInteger("slots", 0)
        );
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


        Neo4j.getInstance().execute(query, params, searchClassHandler(search, promise));
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
