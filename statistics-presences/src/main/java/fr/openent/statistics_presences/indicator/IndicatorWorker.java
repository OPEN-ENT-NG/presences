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
        List<Future<Void>> futures = new ArrayList<>();
        for (String structure : structures.fieldNames()) {
            futures.add(processStructure(structure, structures.getJsonArray(structure)));
        }
        FutureHelper.join(futures).onComplete(this::sendSigTerm);
    }

    /**
     * Launch indicators process. The process compute values for each user and store it in the database.
     *
     * @param structures structure map. Contains in key structure identifier and in value an array containing each structure
     *                   student to proceed
     * @return Future handling result
     */
    private Future<Void> processManualIndicators(JsonObject structures) {
        Promise<Void> promise = Promise.promise();
        List<Future<JsonObject>> indicatorFutures = new ArrayList<>();
        for (Indicator indicator : StatisticsPresences.indicatorMap.values()) {
            indicatorFutures.add(indicator.manualProcess(structures));
        }
        FutureHelper.join(indicatorFutures)
                .onSuccess(success -> promise.complete())
                .onFailure(error -> {
                    log.error(String.format("[StatisticsPresences@ProcessingScheduledManual::processIndicators] Some indicator failed during processing. %s", error.getMessage()));
                    promise.fail(error.getMessage());
                });

        return promise.future();
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

    protected void save(String id, JsonArray students, List<JsonObject> values, Handler<AsyncResult<Void>> handler) {
        if (values.isEmpty()) {
            deleteOldValues(id, students, values).setHandler(event -> {
                if (event.failed()) {
                    handler.handle(Future.failedFuture(event.cause()));
                } else {
                    handler.handle(Future.succeededFuture());
                }
            });
            return;
        }

        deleteOldValues(id, students, values)
                .compose(this::storeValues)
                .setHandler(handler);
    }

    private Future<List<JsonObject>> deleteOldValues(String id, JsonArray students, List<JsonObject> values) {
        Future<List<JsonObject>> future = Future.future();
        JsonObject $in = new JsonObject()
                .put("$in", students);
        JsonObject selector = new JsonObject()
                .put("indicator", this.indicatorName())
                .put("structure", id)
                .put("user", $in);
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

    private void sendSigTerm(AsyncResult<CompositeFuture> ar) {
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
    protected Future<Void> processStructure(String id, JsonArray students) {
        Promise<Void> promise = Promise.promise();

        Future<JsonObject> settingsFuture = IndicatorGeneric.retrieveSettings(id);
        Future<JsonArray> reasonFuture = IndicatorGeneric.retrieveReasons(id);

        CompositeFuture.all(Arrays.asList(settingsFuture, reasonFuture))
                .onSuccess(res -> {
                    List<Integer> reasonIds = ((List<JsonObject>) reasonFuture.result().getList()).stream()
                            .map(reason -> reason.getInteger("id"))
                            .collect(Collectors.toList());

                    JsonObject structureSettings = settingsFuture.result()
                            .put("reasonIds", reasonIds);
                    settings.put(id, structureSettings);
                    List<Future<List<JsonObject>>> futures = new ArrayList<>();
                    Promise<List<JsonObject>> init = Promise.promise();
                    Future<List<JsonObject>> current = init.future();
                    for (int i = 0; i < students.size(); i++) {
                        int indice = i;
                        current = current.compose(v -> {
                            log.debug(String.format("[StatisticsPresences@IndicatorWorker::processStructure] " +
                                    "Processing student %s for structure %s", students.getString(indice), id));
                            Future<List<JsonObject>> next = processStudent(id, students.getString(indice));
                            futures.add(next);
                            return next;
                        });
                    }

                    current
                            .onSuccess(ar -> {
                                List<JsonObject> stats = new ArrayList<>();
                                for (Future<List<JsonObject>> handler : futures) {
                                    stats.addAll(handler.result());
                                }

                                save(id, students, stats, saveAr -> {
                                    if (saveAr.failed()) {
                                        report.failOnSave();
                                        promise.fail(saveAr.cause());
                                    } else {
                                        log.debug("[StatisticsPresences@IndicatorWorker::processStructure] Saved. Completing future");
                                        promise.complete();
                                    }
                                });
                            })
                            .onFailure(ar -> {
                                reportFailures(id, students, futures);
                                promise.fail(String.format("[StatisticsPresences@IndicatorWorker::processStructure] " +
                                        "Structure %s compose chaining throw an error. %s", id, ar.getCause()));
                            });
                    init.complete();
                })
                .onFailure(fail -> {
                    log.debug(String.format("[StatisticsPresences@IndicatorWorker::processStructure] " +
                            "Failed to retrieve settings from eventBus for structure %s", id), fail.getCause());
                    promise.fail(fail.getCause());
                });
        return promise.future();
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
    @SuppressWarnings("unchecked")
    private Future<List<JsonObject>> processStudent(String structureId, String studentId) {
        Promise<List<JsonObject>> promise = Promise.promise();
        List<EventType> eventTypes = Arrays.asList(EventType.values());
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
                        statsByEventTypes.put(eventType, fetchEvent(eventType, structureId, studentId, statProcessSettings.getTimeslot()));
                    }

                    return CompositeFuture.all(new ArrayList<>(statsByEventTypes.values()));
                })
                .onFailure(ar -> {
                    log.error(String.format("[StatisticsPresences@IndicatorWorker::processStudent] " +
                                    "Failed to process student %s in structure %s for indicator %s. %s",
                            studentId,
                            structureId,
                            indicatorName(),
                            ar.getCause().getMessage()
                    ));
                    promise.fail(ar.getCause());
                })
                .onSuccess(ar -> {
                    log.debug(String.format("[StatisticsPresences@IndicatorWorker::processStudent] Student %s proceed", studentId));
                    List<JsonObject> userStats = new ArrayList<>();
                    JsonObject student = studentFuture.result();
                    if (!student.containsKey("name")) {
                        promise.complete(userStats);
                        return;
                    }

                    userStats = statsByEventTypes.entrySet().stream()
                            .flatMap(statsByEventType -> statsByEventType.getValue().result().stream()
                                    .map(stat -> {
                                        stat.setUser(studentId)
                                                .setIndicator(indicatorName())
                                                .setName(student.getString("name"))
                                                .setClassName(String.join(",", student.getJsonArray("className").getList()))
                                                .setType(statsByEventType.getKey())
                                                .setStructure(structureId)
                                                .setAudiences(audienceFuture.result());
                                        return stat.toJSON();
                                    }))
                            .collect(Collectors.toList());
                    promise.complete(userStats);
                });

        return promise.future();
    }

    private void reportFailures(String structure, JsonArray students, List<Future<List<JsonObject>>> futures) {
        for (int i = 0; i < futures.size(); i++) {
            Future<List<JsonObject>> future = futures.get(i);
            if (future.failed()) {
                report.fail(new Failure(students.getString(i), structure, future.cause()));
            }
        }
    }

    protected abstract Future<List<Stat>> fetchEvent(EventType type, String structureId, String studentId, Timeslot timeslot);
}
