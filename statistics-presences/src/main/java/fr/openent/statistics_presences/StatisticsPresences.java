package fr.openent.statistics_presences;

import fr.openent.presences.common.eventbus.GenericCodec;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.incidents.Incidents;
import fr.openent.presences.common.presences.Presences;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.db.DB;
import fr.openent.statistics_presences.bean.Report;
import fr.openent.statistics_presences.controller.*;
import fr.openent.statistics_presences.event.StatisticsRepositoryEvents;
import fr.openent.statistics_presences.indicator.*;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import fr.wseduc.cron.CronTrigger;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.BaseServer;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class StatisticsPresences extends BaseServer {
    public static final String COLLECTION = "presences.statistics";

    public static final String WEEKLY_AUDIENCES_COLLECTION = "presences.statistics_weekly_audiences";
    public static final String VIEW = "statistics_presences.view";
    public static final String VIEW_RESTRICTED = "statistics_presences.view.restricted";
    public static final String MANAGE = "statistics_presences.manage";
    public static final String MANAGE_RESTRICTED = "statistics_presences.manage.restricted";

    public static final GenericCodec codec = new GenericCodec(Report.class);
    public static final String STATISTICS_PRESENCES_CLASS = StatisticsPresences.class.getName();
    public static String DB_SCHEMA = null;
    public static String PRESENCES_SCHEMA = null;
    public static String INCIDENTS_SCHEMA = null;
    public static IndicatorGeneric indicatorGeneric = IndicatorGeneric.getInstance();

    public static final Map<String, Indicator> indicatorMap = new HashMap<>();

    @Override
    public void start() throws Exception {
        super.start();
        DB.getInstance().init(Neo4j.getInstance(), Sql.getInstance(), MongoDb.getInstance());
        CommonServiceFactory commonServiceFactory = new CommonServiceFactory(vertx);

        addController(new EventBusController(commonServiceFactory));
        addController(new StatisticsController(commonServiceFactory));
        addController(new StatisticsWeeklyAudiencesController(commonServiceFactory));
        addController(new ConfigController());
//        addController(new WorkerController()); todo for next step to monitor/secure worker usage

        setRepositoryEvents(new StatisticsRepositoryEvents(commonServiceFactory));


        setSchemas();
        registerCodec();
        deployIndicators();

        Presences.getInstance().init(vertx.eventBus());
        Viescolaire.getInstance().init(vertx.eventBus());
        Incidents.getInstance().init(vertx.eventBus());

        if (config.containsKey("processing-cron")) {
            String processingCron = config.getString("processing-cron");
            new CronTrigger(vertx, processingCron).schedule(new ProcessingScheduledTask(vertx, config, commonServiceFactory));
        }

        // worker to be triggered manually
        vertx.deployVerticle(ProcessingScheduledManual.class, new DeploymentOptions().setConfig(config).setWorker(true));
        vertx.deployVerticle(ProcessingWeeklyAudiencesManual.class, new DeploymentOptions().setConfig(config).setWorker(true));
    }

    private void registerCodec() {
        vertx.eventBus().registerCodec(codec);
    }

    private void setSchemas() {
        JsonObject schemas = config.getJsonObject("schemas", new JsonObject());
        DB_SCHEMA = config.getString("db-schema");
        PRESENCES_SCHEMA = schemas.getString("presences", "presences");
        INCIDENTS_SCHEMA = schemas.getString("incidents", "incidents");
    }

    private void deployIndicators() {
        List<String> indicatorList = config.getJsonArray("indicators", new JsonArray()).getList();
        for (String indicatorName : indicatorList) {
            try {
                Indicator indicator = Indicator.deploy(vertx, indicatorName);
                indicatorMap.put(indicatorName, indicator);
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException | ClassNotFoundException e) {
                log.error(String.format("Failed to deploy indicator %s. %s", indicatorName, e.getMessage()));
            }
        }
    }

    public static Future<JsonObject> launchProcessingStatistics(EventBus eb, JsonObject params) {
        Promise<JsonObject> promise = Promise.promise();
        eb.request(ProcessingScheduledManual.class.getName(), params,
                new DeliveryOptions(), handlerToAsyncHandler(res -> FutureHelper.handleObjectResult(res.body(), promise)));
        return promise.future();
    }

}
