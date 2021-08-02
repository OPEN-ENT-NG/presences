package fr.openent.presences.service.impl;

import fr.openent.presences.db.DBService;
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

        Future<JsonObject> settingsFuture = Future.future();

        JsonArray params = new JsonArray();
        params.add(STRUCTURE_ID);
        params.add(5).add(3).add(3).add(3);
        params.add(STRUCTURE_ID);

        settingsFuture.onComplete(res -> {
            ctx.assertEquals(res.result().getJsonArray("values"), params);
        });

        initService.getSettingsStatement(STRUCTURE_ID, settingsFuture);
    }
}
