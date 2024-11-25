package fr.openent.presences.service.impl;

import fr.openent.presences.db.DBService;
import fr.openent.presences.enums.InitTypeEnum;
import fr.openent.presences.service.*;
import io.vertx.core.*;
import io.vertx.core.json.*;
import io.vertx.ext.unit.*;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class InitServiceTest extends DBService {

    private InitService initService;
    private static final String STRUCTURE_ID = "structureId";

    @Before
    public void setUp() {
        this.initService = new DefaultInitService();
    }

    @Test
    public void testGetSettingsStatement(TestContext ctx)  {
        Async async = ctx.async(2);

        Promise<JsonObject> settingsPromise1D = Promise.promise();
        Promise<JsonObject> settingsPromise2D = Promise.promise();

        String statement = "INSERT INTO null.settings(structure_id, alert_absence_threshold, alert_lateness_threshold," +
                " alert_incident_threshold, alert_forgotten_notebook_threshold, initialized, allow_multiple_slots) " +
                "VALUES (?, ?, ?, ?, ?, true, true) ON CONFLICT ON CONSTRAINT settings_pkey DO UPDATE SET initialized = true," +
                " alert_absence_threshold = ?, alert_lateness_threshold = ?, alert_incident_threshold = ?," +
                " alert_forgotten_notebook_threshold = ? WHERE settings.structure_id = ? ;";

        JsonArray params2D = new JsonArray();
        params2D.add(STRUCTURE_ID);
        params2D.add(5).add(3).add(3).add(3);
        params2D.add(5).add(3).add(3).add(3);
        params2D.add(STRUCTURE_ID);

        JsonArray params1D = new JsonArray();
        params1D.add(STRUCTURE_ID);
        params1D.add(4).add(3).add(3).add(3);
        params1D.add(4).add(3).add(3).add(3);
        params1D.add(STRUCTURE_ID);

        settingsPromise1D.future().onComplete(res -> {
            ctx.assertEquals(res.result().getJsonArray("values"), params1D);
            ctx.assertEquals(res.result().getString("statement"), statement);
            async.countDown();
        });

        settingsPromise2D.future().onComplete(res -> {
            ctx.assertEquals(res.result().getJsonArray("values"), params2D);
            ctx.assertEquals(res.result().getString("statement"), statement);
            async.countDown();
        });

        initService.getSettingsStatement(STRUCTURE_ID, InitTypeEnum.ONE_D, settingsPromise1D);
        initService.getSettingsStatement(STRUCTURE_ID, InitTypeEnum.TWO_D, settingsPromise2D);
        async.awaitSuccess(10000);
    }
}
