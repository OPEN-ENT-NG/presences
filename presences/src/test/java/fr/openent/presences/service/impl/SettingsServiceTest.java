package fr.openent.presences.service.impl;

import fr.openent.presences.db.DB;
import fr.openent.presences.db.DBService;
import io.vertx.core.json.*;
import io.vertx.ext.unit.*;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import io.vertx.core.Handler;
import org.mockito.stubbing.*;


@RunWith(VertxUnitRunner.class)
public class SettingsServiceTest extends DBService {

    Sql sql = Mockito.mock(Sql.class);

    private DefaultSettingsService settingsService;

    private static final String STRUCTURE_ID = "structureId";
    private static final String SETTING_BOOLEAN_PARAM = "setting";


    @Before
    public void setUp(TestContext context) {
        DB.getInstance().init(null, sql, null);
        this.settingsService = new DefaultSettingsService();
    }

    @Test
    public void testRetrieveSettings(TestContext ctx)  {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(params, new JsonArray().add(STRUCTURE_ID));
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        settingsService.retrieve(STRUCTURE_ID, null);
    }

    @Test
    public void testRetrieveMultipleSlotSettings(TestContext ctx)  {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(params, new JsonArray().add(STRUCTURE_ID));
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        settingsService.retrieveMultipleSlots(STRUCTURE_ID);
    }

    @Test
    public void testSetSettingsStructureId(TestContext ctx)  {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(params, new JsonArray().add(STRUCTURE_ID));
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        settingsService.put(STRUCTURE_ID, null, null);
    }

    @Test
    public void testSetSettingTrue(TestContext ctx)  {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(params.getJsonObject(0).getBoolean(SETTING_BOOLEAN_PARAM), true);
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        settingsService.put(STRUCTURE_ID, new JsonObject().put(SETTING_BOOLEAN_PARAM, true), null);
    }

    @Test
    public void testSetSettingFalse(TestContext ctx)  {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(params.getJsonObject(0).getBoolean(SETTING_BOOLEAN_PARAM), false);
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        settingsService.put(STRUCTURE_ID, new JsonObject().put(SETTING_BOOLEAN_PARAM, false), null);
    }
}
