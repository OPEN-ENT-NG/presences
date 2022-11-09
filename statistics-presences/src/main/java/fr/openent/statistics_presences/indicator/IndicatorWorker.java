package fr.openent.statistics_presences.indicator;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.bean.Failure;
import fr.openent.statistics_presences.bean.Report;
import fr.openent.statistics_presences.bean.Stat;
import fr.openent.statistics_presences.bean.StatProcessSettings;
import fr.openent.statistics_presences.bean.timeslot.Timeslot;
import fr.openent.statistics_presences.utils.EventType;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static fr.openent.statistics_presences.StatisticsPresences.STATISTICS_PRESENCES_CLASS;

public abstract class IndicatorWorker extends AbstractVerticle {
    protected final Logger log = LoggerFactory.getLogger(IndicatorWorker.class);
    protected final Map<String, JsonObject> settings = new HashMap<>();
    protected Context indicatorContext;
    protected JsonObject config;
    protected Report report;

    @Override
    public void start() throws Exception {
        // for some reason we might lose our verticle's context and might also pick its parent's context to keep it "alive"
        // in order to avoid this behavior, we assign manually its own context to indicatorContext
        indicatorContext = vertx.getOrCreateContext();
        config = new JsonObject(config().toString());
        log.info(String.format("[StatisticsPresences@IndicatorWorker::start] Launching worker %s, deploy verticle %s",
                this.indicatorName(), indicatorContext.deploymentID()));
        this.report = new Report(this.indicatorName()).start();
        JsonObject structures = config.getJsonObject(Field.STRUCTURES);
        Future<Void> current = Future.succeededFuture();

        for (String structure : structures.fieldNames()) {
            current = current.compose(res -> {
                Promise<Void> promise = Promise.promise();
                processStructure(structure, structures.getJsonArray(structure)).onComplete(r -> promise.complete());
                return promise.future();
            });
        }

        current.onComplete(this::sendSigTerm);
    }

    /**
     * Process computing statistics for each student's inside structure within config given (payload)
     * (JsonObject is a map with structure id as key and array of student id and its endpoint (indicator name)
     *
     * @param payload config with endpoint (indicatorName) and structures (Map within id key structure and array as student id)
     * @return Future ending process
     */
    protected Future<JsonObject> manualStart(JsonObject payload) {
        Promise<JsonObject> promise = Promise.promise();
        config = new JsonObject(payload.toString());
        this.report = new Report(this.indicatorName()).start();
        JsonObject structures = config.getJsonObject(Field.STRUCTURES);
        List<Future<Void>> futures = new ArrayList<>();
        for (String structure : structures.fieldNames()) {
            futures.add(processStructure(structure, structures.getJsonArray(structure)));
        }
        FutureHelper.join(futures)
                .onSuccess(res -> promise.complete(new JsonObject().put(Field.MESSAGE, Field.OK)))
                .onFailure(err -> {
                    String message = String.format("[Presences@%s::manualStart] An error has occurred when updating student(s)'s stats : %s, " +
                            "returning empty list", this.getClass().getSimpleName(), err.getMessage());
                    log.error(message);
                    promise.fail(err.getMessage());
                });
        return promise.future();
    }

    protected JsonArray reasonIds(String structureId) {
        return settings.get(structureId).getJsonArray("reasonIds", new JsonArray());
    }

    protected String indicatorName() {
        return config.getString(Field.ENDPOINT);
    }

    /**
     * No filter date
     *
     * @deprecated Replaced by {@link #save(String, JsonArray, List, String, String, Handler)}
     */
    @Deprecated
    protected void save(String id, JsonArray students, List<JsonObject> values, Handler<AsyncResult<Void>> handler) {
        this.save(id, students, values, null, null, handler);
    }

    protected Future<List<JsonObject>> overrideStatisticsStudent(String structureId, String studentId, List<JsonObject> values, String startDate,
                                             String endDate) {
        Promise<List<JsonObject>> promise = Promise.promise();

        Future<List<JsonObject>> future = deleteOldValuesForStudent(structureId, studentId, values, startDate, endDate);

        if (values.isEmpty()) {
            future.onComplete(promise);
        } else {
            future.compose(this::storeValues)
                    .onSuccess(res -> promise.complete(values))
                    .onFailure(promise::fail);
        }

        return promise.future();
    }

    protected void save(String id, JsonArray students, List<JsonObject> values, String startDate, String endDate,
                        Handler<AsyncResult<Void>> handler) {
        if (values.isEmpty()) {
            deleteOldValues(id, students, values, startDate, endDate).onComplete(event -> {
                if (event.failed()) {
                    handler.handle(Future.failedFuture(event.cause()));
                } else {
                    handler.handle(Future.succeededFuture());
                }
            });
            return;
        }

        deleteOldValues(id, students, values, startDate, endDate)
                .compose(this::storeValues)
                .onComplete(handler);
    }

    /**
     * No filter date
     *
     * @deprecated Replaced by {@link #deleteOldValues(String, JsonArray, List, String, String)}
     */
    @Deprecated
    private Future<List<JsonObject>> deleteOldValues(String id, JsonArray students, List<JsonObject> values) {
        return deleteOldValues(id, students, values, null, null);
    }

    private Future<List<JsonObject>> deleteOldValues(String id, JsonArray students, List<JsonObject> values, String startDate, String endDate) {
        Future<List<JsonObject>> future = Future.future();
        JsonObject $in = new JsonObject()
                .put(Field.$IN, students);
        JsonObject selector = new JsonObject()
                .put(Field.INDICATOR, this.indicatorName())
                .put(Field.STRUCTURE, id)
                .put(Field.USER, $in);
        if (startDate != null && endDate != null) {
            JsonObject $gte = new JsonObject()
                    .put(Field.$GTE, startDate);
            JsonObject $lte = new JsonObject()
                    .put(Field.$LTE, endDate);
            selector.put(Field.START_DATE, $gte)
                    .put(Field.END_DATE, $lte);
        }

        MongoDb.getInstance().delete(StatisticsPresences.COLLECTION, selector, MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@IndicatorWorker::deleteOldValues] " +
                                "Failed to remove old statistics for indicator %s. %s",
                        this.indicatorName(),
                        either.left().getValue()
                ));
                future.fail(either.left().getValue());
            } else {
                future.complete(values);
            }
        }));

        return future;
    }

    private Future<List<JsonObject>> deleteOldValuesForStudent(String id, String studentId, List<JsonObject> values, String startDate, String endDate) {
        Future<List<JsonObject>> future = Future.future();
        JsonObject selector = new JsonObject()
                .put(Field.INDICATOR, this.indicatorName())
                .put(Field.STRUCTURE, id)
                .put(Field.USER, studentId);
        if (startDate != null && endDate != null) {
            JsonObject $gte = new JsonObject()
                    .put(Field.$GTE, startDate);
            JsonObject $lte = new JsonObject()
                    .put(Field.$LTE, endDate);
            selector.put(Field.START_DATE, $gte)
                    .put(Field.END_DATE, $lte);
        }

        MongoDb.getInstance().delete(StatisticsPresences.COLLECTION, selector, MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@IndicatorWorker::deleteOldValuesForStudent] " +
                                "Failed to remove old statistics for student %s for indicator %s. %s",
                        studentId, this.indicatorName(), either.left().getValue()
                ));
                future.fail(either.left().getValue());
            } else {
                future.complete(values);
            }
        }));

        return future;
    }

    private Future<Void> storeValues(List<JsonObject> values) {
        Future<Void> future = Future.future();
        MongoDb.getInstance().insert(StatisticsPresences.COLLECTION, new JsonArray(values), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@IndicatorWorker::storeValues] " +
                                "%s indicator failed to store new values. %s",
                        this.indicatorName(),
                        either.left().getValue()
                ));
                future.fail(either.left().getValue());
            } else {
                future.complete();
            }
        }));

        return future;
    }

    private void sendSigTerm(AsyncResult<Void> ar) {
        this.report.end();
        if (ar.failed()) {
            log.error(String.format("[StatisticsPresences@IndicatorWorker::sendSigTerm] Some structure failed to process. %s", ar.cause().getMessage()));
        }

        log.info(String.format("[StatisticsPresences@IndicatorWorker::sendSigTerm] Sending term signal by %s indicator", this.getClass().getName()));

        DeliveryOptions deliveryOptions = new DeliveryOptions().setCodecName(StatisticsPresences.codec.name());
        vertx.eventBus().send(config.getString(Field.ENDPOINT), report, deliveryOptions);
        if (isParentVerticle()) {
            log.info(String.format("[StatisticsPresences@IndicatorWorker::sendSigTerm] Tried to undeploy verticle %s but" +
                    " turns out it is the Parent Verticle...", indicatorContext.deploymentID()));
        } else {
            log.info(String.format("[StatisticsPresences@IndicatorWorker::sendSigTerm] Undeploy verticle %s",
                    indicatorContext.deploymentID()));
            vertx.undeploy(indicatorContext.deploymentID());
        }
    }

    /**
     * check if our verticle's context is the parent (Statistics Presences Verticle)
     *
     * @return boolean  true if verticle parent, false if own vertx's context
     */
    private boolean isParentVerticle() {
        return config.containsKey("main") &&
                config.getString("main").equals(STATISTICS_PRESENCES_CLASS);
    }

    @SuppressWarnings("unchecked")
    protected Future<Void> processStructure(String structureId, JsonArray students) {
        Promise<Void> promise = Promise.promise();

        Future<JsonObject> settingsFuture = IndicatorGeneric.retrieveSettings(structureId);
        Future<JsonArray> reasonFuture = IndicatorGeneric.retrieveReasons(structureId);
        Future<JsonObject> schoolYearFuture = Viescolaire.getInstance().getSchoolYear(structureId);

        CompositeFuture.all(Arrays.asList(settingsFuture, reasonFuture, schoolYearFuture))
                .onSuccess(res -> {
                    List<Integer> reasonIds = ((List<JsonObject>) reasonFuture.result().getList()).stream()
                            .map(reason -> reason.getInteger("id"))
                            .collect(Collectors.toList());
                    JsonObject schoolYear = schoolYearFuture.result();

                    JsonObject structureSettings = settingsFuture.result()
                            .put("reasonIds", reasonIds);
                    settings.put(structureId, structureSettings);

                    Promise<Void> init = Promise.promise();
                    Future<Void> current = init.future();
                    String startDate = schoolYear.getString(Field.START_DATE, null);
                    String endDate = schoolYear.getString(Field.END_DATE, null);
                    Function<Integer, Future<List<JsonObject>>> nextProcessFunction = index -> {
                        log.debug(String.format("[StatisticsPresences@IndicatorWorker::processStructure] " +
                                "Processing student %s for structure %s", students.getString(index), structureId));
                        return processStudent(structureId, students.getString(index), startDate, endDate);
                    };

                    for (int i = 0; i < students.size(); i++) {
                        int indice = i;

                        current = current.compose(result -> {
                            Promise<Void> otherPromise = Promise.promise();
                            nextProcessFunction.apply(indice)
                                    .onComplete(ar -> {
                                        if (ar.failed()) {
                                            log.error(String.format("[StatisticsPresences@IndicatorWorker::processStructure] " +
                                                    "Processing fail student %s for structure %s %s", students.getString(indice), structureId, ar.cause()));
                                            report.fail(new Failure(students.getString(indice), structureId, ar.cause()));
                                        }
                                        otherPromise.complete();
                                    });

                            return otherPromise.future();
                        });
                    }

                    current.onComplete(ar -> promise.complete());
                    init.complete();
                })
                .onFailure(fail -> {
                    log.debug(String.format("[StatisticsPresences@IndicatorWorker::processStructure] " +
                            "Failed to retrieve settings from eventBus for structure %s", structureId), fail.getCause());
                    promise.fail(fail.getCause());
                });
        return promise.future();
    }

    /**
     * No filter date
     *
     * @deprecated Replaced by {@link #processStudent(String, String, String, String)}
     */
    @Deprecated
    private Future<List<JsonObject>> processStudent(String structureId, String studentId) {
        return processStudent(structureId, studentId, null, null);
    }

    /*
        For each student retrieve :
        - absence count (no reason + unregularized + regularized)
        - no reason absence count
        - unregularized absence count
        - regularized absence count
        - lateness count
        - departure count
        - sanction/punishment count
        - incident count
     */
    private Future<List<JsonObject>> processStudent(String structureId, String studentId, String startDate, String endDate) {
        Promise<List<JsonObject>> promise = Promise.promise();
        EventType[] eventTypes = EventType.values();
        Map<EventType, Future<List<Stat>>> statsByEventTypes = new HashMap<>();

        StatProcessSettings statProcessSettings = new StatProcessSettings();

        Future<JsonArray> audienceFuture = IndicatorGeneric.retrieveAudiences(structureId, studentId);
        Future<JsonObject> studentFuture = IndicatorGeneric.retrieveUser(structureId, studentId);

        CompositeFuture.all(audienceFuture, studentFuture)
                .compose(settingsRes -> {
                    statProcessSettings.setStudentInfo(studentFuture.result());
                    statProcessSettings.setAudienceIds(audienceFuture.result());

                    String classId = !statProcessSettings.getStudentClassIds().isEmpty() ?
                            statProcessSettings.getStudentClassIds().get(0) : null;

                    return Viescolaire.getInstance().getAudienceTimeslots(structureId, Collections.singletonList(classId));
                })
                .compose(timeslots -> {
                    if (timeslots == null || timeslots.isEmpty()) {
                        String message = String.format("[StatisticsPresences@%s::processStudent] " +
                                        "%s error: timeslot not found in structure %s",
                                this.getClass().getSimpleName(), indicatorName(), structureId);
                        log.error(message);
                        return Future.failedFuture(message);
                    }

                    statProcessSettings.setTimeslot(timeslots.getJsonObject(0));

                    for (EventType eventType : eventTypes) {
                        statsByEventTypes.put(eventType, fetchEvent(eventType, structureId, studentId, statProcessSettings.getTimeslot(), startDate, endDate));
                    }

                    return CompositeFuture.all(new ArrayList<>(statsByEventTypes.values()));
                })
                .compose(ar -> {
                    log.debug(String.format("[StatisticsPresences@IndicatorWorker::processStudent] Student %s proceed", studentId));
                    List<JsonObject> userStats = collectUserStats(statsByEventTypes, studentFuture.result(), studentId, structureId, audienceFuture.result());
                    return overrideStatisticsStudent(structureId, studentId, userStats, startDate, endDate);
                })
                .onSuccess(promise::complete)
                .onFailure(ar -> {
                    log.error(String.format("[StatisticsPresences@IndicatorWorker::processStudent] " +
                                    "Failed to process student %s in structure %s for indicator %s. %s",
                            studentId,
                            structureId,
                            indicatorName(),
                            ar.getMessage()
                    ));
                    promise.fail(ar.getMessage());
                    ar.printStackTrace();
                });

        return promise.future();
    }

    @SuppressWarnings("unchecked")
    private List<JsonObject> collectUserStats(Map<EventType, Future<List<Stat>>> statsByEventTypes, JsonObject student, String studentId, String structureId, JsonArray audiencesList) {
        if (!student.containsKey(Field.NAME)) {
            return new ArrayList<>();
        }

        return statsByEventTypes.entrySet().stream()
                .flatMap(statsByEventType -> statsByEventType.getValue().result().stream()
                        .map(stat -> {
                            stat.setUser(studentId)
                                    .setIndicator(indicatorName())
                                    .setName(student.getString(Field.NAME))
                                    .setClassName(String.join(",", student.getJsonArray(Field.CLASSNAME).getList()))
                                    .setType(statsByEventType.getKey())
                                    .setStructure(structureId)
                                    .setAudiences(audiencesList);
                            return stat.toJSON();
                        }))
                .collect(Collectors.toList());
    }

    /**
     * No filter date
     *
     * @deprecated Replaced by {@link #fetchEvent(EventType, String, String, Timeslot, String, String)}
     */
    @Deprecated
    protected Future<List<Stat>> fetchEvent(EventType type, String structureId, String studentId, Timeslot timeslot) {
        return this.fetchEvent(type, structureId, studentId, timeslot, null, null);
    };

    protected abstract Future<List<Stat>> fetchEvent(EventType type, String structureId, String studentId, Timeslot timeslot, String startDate, String endDate);
}
