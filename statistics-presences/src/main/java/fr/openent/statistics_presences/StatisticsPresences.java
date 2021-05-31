package fr.openent.statistics_presences;

import fr.openent.presences.common.eventbus.GenericCodec;
import fr.openent.presences.common.incidents.*;
import fr.openent.presences.common.presences.Presences;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.db.DB;
import fr.openent.statistics_presences.bean.Report;
import fr.openent.statistics_presences.controller.EventBusController;
import fr.openent.statistics_presences.controller.StatisticsController;
import fr.openent.statistics_presences.indicator.Indicator;
import fr.openent.statistics_presences.indicator.IndicatorGeneric;
import fr.openent.statistics_presences.indicator.ProcessingScheduledTask;
import fr.wseduc.cron.CronTrigger;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.BaseServer;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsPresences extends BaseServer {
    public static final String COLLECTION = "presences.statistics";
    public static final String VIEW = "statistics_presences.view";
    public static final GenericCodec codec = new GenericCodec(Report.class);
    public static String DB_SCHEMA = null;
    public static String PRESENCES_SCHEMA = null;
    public static String INCIDENTS_SCHEMA = null;
    public static IndicatorGeneric indicatorGeneric = IndicatorGeneric.getInstance();

    public static final Map<String, Indicator> indicatorMap = new HashMap<>();

    @Override
    public void start() throws Exception {
        super.start();
        DB.getInstance().init(Neo4j.getInstance(), Sql.getInstance(), MongoDb.getInstance());

        addController(new EventBusController());
        addController(new StatisticsController());

        setSchemas();
        registerCodec();
        deployIndicators();

        Presences.getInstance().init(vertx.eventBus());
        Viescolaire.getInstance().init(vertx.eventBus());
        Incidents.getInstance().init(vertx.eventBus());

        if (config.containsKey("processing-cron")) {
            String processingCron = config.getString("processing-cron");
            new CronTrigger(vertx, processingCron).schedule(new ProcessingScheduledTask(vertx, config));
        }
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
                log.error(String.format("Failed to deploy indicator %s", indicatorName), e);
            }
        }
    }

}
