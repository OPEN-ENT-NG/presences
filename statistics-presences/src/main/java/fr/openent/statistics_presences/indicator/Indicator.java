package fr.openent.statistics_presences.indicator;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.db.DBService;
import fr.openent.statistics_presences.bean.Report;
import fr.openent.statistics_presences.filter.Filter;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.*;
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
    private MessageConsumer<Report> consumer = null;

    protected Indicator(Vertx vertx, String name) {
        this.vertx = vertx;
        this.name = name;
    }

    private String indicatorClassName() {
        return String.format(NAME_FORMATTER, name);
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

    /**
     * Process search request
     *
     * @param filter  Filter query
     * @param handler Function handler returning data
     */
    public abstract void search(Filter filter, Handler<AsyncResult<JsonObject>> handler);

    public abstract void searchGraph(Filter filter, Handler<AsyncResult<JsonObject>> handler);

    public void export(HttpServerRequest request, Filter filter, List<JsonObject> values, JsonObject count, JsonObject slots) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException {
        String className = String.format(CSV_EXPORT_FORMATTER, name);
        ClassLoader loader = Indicator.class.getClassLoader();
        CSVExport export = (count != null) ?
                (CSVExport) Class.forName(className, true, loader).getConstructors()[0].newInstance(filter, values, count, slots):
                (CSVExport) Class.forName(className, true, loader).getConstructors()[0].newInstance(filter, values);
        export.export(request);
    }

    /**
     * Process CRON indicator calculation
     *
     * @param structures student list group by structure
     * @return Future ending process
     */
    public Future<Report> process(JsonObject structures) {
        log.info(String.format("[StatisticsPresences@Indicator::process] processing indicator %s", this.name));
        String consumerName = indicatorClassName();
        Future<Report> future = Future.future();

        Handler<Message<Report>> handler = message -> {
            log.info(String.format("[StatisticsPresences@Indicator::process] SIGTERM sent from %s indicator", this.indicatorClassName()));
            consumer.unregister();
            future.handle(Future.succeededFuture(message.body()));
        };

        consumer = vertx.eventBus().consumer(consumerName, handler);
        deployWorker(structures);
        return future;
    }

    private void deployWorker(JsonObject structures) {
        JsonObject config = new JsonObject()
                .put("structures", structures)
                .put("endpoint", indicatorClassName());

        String workerName = String.format("%s.worker.%s", Indicator.class.getPackage().getName(), this.name);
        vertx.deployVerticle(workerName, new DeploymentOptions().setConfig(config).setWorker(true));
    }


}
