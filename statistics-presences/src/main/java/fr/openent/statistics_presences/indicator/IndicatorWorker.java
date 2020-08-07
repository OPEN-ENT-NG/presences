package fr.openent.statistics_presences.indicator;

import fr.openent.presences.common.presences.Presences;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.bean.Failure;
import fr.openent.statistics_presences.bean.Report;
import fr.openent.statistics_presences.indicator.worker.Global;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.neo4j.Neo4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class IndicatorWorker extends AbstractVerticle {
    protected final Logger log = LoggerFactory.getLogger(IndicatorWorker.class);
    protected final Map<String, JsonObject> settings = new HashMap<>();
    protected Report report;

    @Override
    public void start() throws Exception {
        log.info(String.format("Launching worker %s", Global.class.getName()));
        initNeo4j();
        this.report = new Report(this.indicatorName()).start();
        JsonObject structures = config().getJsonObject("structures");
        List<Future> futures = new ArrayList<>();
        for (String structure : structures.fieldNames()) {
            futures.add(processStructure(structure, structures.getJsonArray(structure)));
        }

        CompositeFuture.join(futures).setHandler(this::sendSigTerm);
    }

    private void initNeo4j() {
        String neo4jConfig = (String) vertx.sharedData().getLocalMap("server").get("neo4jConfig");
        Neo4j.getInstance().init(vertx, new JsonObject(neo4jConfig));
    }

    protected String recoveryMethod(String structureId) {
        return settings.get(structureId).getString("event_recovery_method", null);
    }

    protected String indicatorName() {
        return config().getString("endpoint");
    }

    protected void save(List<JsonObject> values, Handler<AsyncResult<Void>> handler) {
        if (values.isEmpty()) {
            handler.handle(Future.succeededFuture());
            return;
        }

        deleteOldValues(values)
                .compose(this::storeValues)
                .setHandler(handler);
    }

    private Future<List<JsonObject>> deleteOldValues(List<JsonObject> values) {
        Future<List<JsonObject>> future = Future.future();
        List<String> students = values.stream().map(value -> value.getString("user")).collect(Collectors.toList());
        JsonObject $in = new JsonObject()
                .put("$in", new JsonArray(students));
        JsonObject selector = new JsonObject()
                .put("indicator", this.indicatorName())
                .put("user", $in);

        MongoDb.getInstance().delete(StatisticsPresences.COLLECTION, selector, MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("Failed to remove old statistics for indicator %s", this.indicatorName()));
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
                log.error(String.format("%s indicator failed to store new values", this.indicatorName()));
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
            log.error("Some structure failed to process", ar.cause());
        }

        log.info("Sending term signal");

        DeliveryOptions deliveryOptions = new DeliveryOptions().setCodecName(StatisticsPresences.codec.name());
        vertx.eventBus().send(config().getString("endpoint"), report, deliveryOptions);
        log.info(String.format("Undeploy verticle %s", vertx.getOrCreateContext().deploymentID()));
        vertx.undeploy(vertx.getOrCreateContext().deploymentID());
    }

    protected Future<Void> processStructure(String id, JsonArray students) {
        Future<Void> future = Future.future();
        Presences.getInstance().getSettings(id, either -> {
            if (either.isLeft()) {
                future.fail(either.left().getValue());
                return;
            }

            JsonObject structureSettings = either.right().getValue();
            settings.put(id, structureSettings);
            List<Future<List<JsonObject>>> futures = new ArrayList<>();
            Future<List<JsonObject>> init = Future.future();
            Future<List<JsonObject>> current = init;
            for (int i = 0; i < students.size(); i++) {
                int indice = i;
                current = current.compose(v -> {
                    log.debug(String.format("Processing student %s for structure %s", students.getString(indice), id));
                    Future<List<JsonObject>> next = processStudent(id, students.getString(indice));
                    futures.add(next);
                    return next;
                });
            }

            current.setHandler(ar -> {
                if (ar.failed()) {
                    reportFailures(id, students, futures);
                    future.fail(String.format("Structure %s compose chaining throw an error.", id));
                } else {
                    List<JsonObject> stats = new ArrayList<>();
                    for (Future<List<JsonObject>> handler : futures) {
                        stats.addAll(handler.result());
                    }

                    save(stats, saveAr -> {
                        if (saveAr.failed()) {
                            report.failOnSave();
                            future.fail(saveAr.cause());
                        } else {
                            log.debug("Saved. Completing future");
                            future.complete();
                        }
                    });
                }
            });

            init.complete();
        });

        return future;
    }

    private void reportFailures(String structure, JsonArray students, List<Future<List<JsonObject>>> futures) {
        for (int i = 0; i < futures.size(); i++) {
            Future<List<JsonObject>> future = futures.get(i);
            if (future.failed()) {
                report.fail(new Failure(students.getString(i), structure, future.cause()));
            }
        }
    }

    protected abstract Future<List<JsonObject>> processStudent(String structureId, String id);
}
