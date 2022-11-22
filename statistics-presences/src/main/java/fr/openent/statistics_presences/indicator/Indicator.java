package fr.openent.statistics_presences.indicator;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DBService;
import fr.openent.statistics_presences.bean.Report;
import fr.openent.statistics_presences.model.StatisticsFilter;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public abstract class Indicator extends DBService {
    private static final String NAME_FORMATTER = "fr.openent.statistics_presences.indicator.impl.%s";
    private final String CSV_EXPORT_FORMATTER = "fr.openent.statistics_presences.indicator.export.%s";
    private final Logger log = LoggerFactory.getLogger(Indicator.class);
    private final String name;
    private final Vertx vertx;

    protected Indicator(Vertx vertx, String name) {
        this.vertx = vertx;
        this.name = name;
    }

    public static Indicator deploy(Vertx vertx, String name) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException {
        String className = String.format(NAME_FORMATTER, name);
        ClassLoader loader = Indicator.class.getClassLoader();
        return (Indicator) Class.forName(className, true, loader).getConstructors()[0].newInstance(vertx, name);
    }

    public static Handler<AsyncResult<JsonObject>> handler(HttpServerRequest request) {
        return ar -> {
            if (ar.failed()) Renders.renderError(request);
            else Renders.renderJson(request, ar.result());
        };
    }

    private String indicatorClassName() {
        return String.format(NAME_FORMATTER, name);
    }

    /**
     * Process search request
     *
     * @param filter  Filter query
     * @param handler Function handler returning data
     */
    public abstract void search(StatisticsFilter filter, Handler<AsyncResult<JsonObject>> handler);

    public abstract void searchGraph(StatisticsFilter filter, Handler<AsyncResult<JsonObject>> handler);

    public void export(HttpServerRequest request, StatisticsFilter filter, List<JsonObject> values, JsonObject count,
                       JsonObject slots, JsonObject rate, String recoveryMethod) throws ClassNotFoundException, IllegalAccessException,
            InvocationTargetException, InstantiationException {
        String className = String.format(CSV_EXPORT_FORMATTER, name);
        ClassLoader loader = Indicator.class.getClassLoader();
        CSVExport export = (count != null) ?
                (CSVExport) Class.forName(className, true, loader).getConstructors()[0].newInstance(filter, values, count, slots, rate, recoveryMethod) :
                (CSVExport) Class.forName(className, true, loader).getConstructors()[0].newInstance(filter, values);
        export.export(request);
    }

    /**
     * Process CRON indicator calculation
     *
     * @param structures student list group by structure
     * @return Future ending process
     * @deprecated Replaced by {@link IndicatorGeneric#process(Vertx, JsonObject)}
     */
    @Deprecated
    public Future<Report> process(JsonObject structures) {
        log.info(String.format("[StatisticsPresences@Indicator::process] processing indicator %s", this.name));
        String consumerName = indicatorClassName();
        Promise<Report> promise = Promise.promise();

        MessageConsumer<Report> consumer = vertx.eventBus().consumer(consumerName);
        Handler<Message<Report>> handler = message -> {
            log.info(String.format("[StatisticsPresences@Indicator::process] SIGTERM sent from %s indicator", this.indicatorClassName()));
            consumer.unregister();
            promise.handle(Future.succeededFuture(message.body()));
        };

        consumer.handler(handler);
        deployWorker(structures);
        return promise.future();
    }

    /**
     * @deprecated Replaced by {@link IndicatorGeneric#process(Vertx, JsonObject)}
     */
    @Deprecated
    private void deployWorker(JsonObject structures) {
        JsonObject config = new JsonObject()
                .put("structures", structures)
                .put("endpoint", indicatorClassName());

        String workerName = String.format("%s.worker.%s", Indicator.class.getPackage().getName(), this.name);
        vertx.deployVerticle(workerName, new DeploymentOptions().setConfig(config).setWorker(true));
    }

    /**
     * Process one indicator to run computing statistics
     * (JsonObject is a map with structure id as key and array of student id)
     *
     * @param structures student list group by structure
     * @return Future ending process
     * @deprecated Replaced by {@link IndicatorGeneric#manualProcess(List)}
     */
    @Deprecated
    public Future<JsonObject> manualProcess(JsonObject structures) {
        Promise<JsonObject> promise = Promise.promise();
        JsonObject config = new JsonObject()
                .put(Field.STRUCTURES, structures)
                .put(Field.ENDPOINT, indicatorClassName());
        String workerName = String.format("%s.worker.%s", Indicator.class.getPackage().getName(), this.name);
        ClassLoader loader = Indicator.class.getClassLoader();
        try {
            IndicatorWorker indicatorWorker = (IndicatorWorker) Class.forName(workerName, true, loader).getConstructors()[0].newInstance();
            indicatorWorker.manualStart(config)
                    .onSuccess(promise::complete)
                    .onFailure(promise::fail);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | ClassNotFoundException e) {
            String message = String.format("[Presences@%s::manualProcess] An error has occurred when starting process " +
                    "indicatorWorker manually : %s" , this.getClass().getSimpleName(), e.getMessage());
            log.error(message);
            promise.fail(e.getMessage());
        }
        return promise.future();
    }

}
