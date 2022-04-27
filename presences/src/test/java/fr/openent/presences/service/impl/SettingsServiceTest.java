package fr.openent.presences.service.impl;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DB;
import fr.openent.presences.db.DBService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;


@RunWith(VertxUnitRunner.class)
public class SettingsServiceTest extends DBService {

    private static final String STRUCTURE_ID = "structureId";
    private static final String OTHER_KEY = "otherKey";
    private static final String OTHER_VALUE = "otherValue";
    Sql sql = Mockito.mock(Sql.class);
    private DefaultSettingsService settingsService;

    @Before
    public void setUp(TestContext context) {
        DB.getInstance().init(null, sql, null);
        this.settingsService = new DefaultSettingsService();
    }

    @Test
    public void testRetrieveSettings(TestContext ctx) {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(params, new JsonArray().add(STRUCTURE_ID));
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        settingsService.retrieve(STRUCTURE_ID, null);
    }

    @Test
    public void testRetrieveMultipleSlotSettings(TestContext ctx) {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(params, new JsonArray().add(STRUCTURE_ID));
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        settingsService.retrieveMultipleSlots(STRUCTURE_ID);
    }

    @Test
    public void testSetSettingsStructureId(TestContext ctx) {
        String queryExpected = "SELECT COUNT(structure_id) FROM null.settings WHERE structure_id = ?";
        Mockito.doAnswer((Answer<Void>) invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, queryExpected);
            ctx.assertEquals(params, new JsonArray().add(STRUCTURE_ID));
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        settingsService.put(STRUCTURE_ID, null, null);
    }

    @Test
    public void testSetSettingTrue(TestContext ctx) throws Exception {
        String queryExpected = "INSERT INTO null.settings (structure_id,alert_absence_threshold) VALUES (?,?) RETURNING *";
        Mockito.doAnswer((Answer<Void>) invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, queryExpected);
            ctx.assertEquals(params, new JsonArray(Arrays.asList("structureId", true)));
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        Whitebox.invokeMethod(settingsService, "create",
                STRUCTURE_ID, new JsonObject().put(Field.ALERT_ABSENCE_THRESHOLD, true).put(OTHER_KEY, OTHER_VALUE), null);
    }

    @Test
    public void testSetSettingFalse(TestContext ctx) throws Exception {
        String queryExpected = "INSERT INTO null.settings (structure_id,alert_absence_threshold) VALUES (?,?) RETURNING *";
        Mockito.doAnswer((Answer<Void>) invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, queryExpected);
            ctx.assertEquals(params, new JsonArray(Arrays.asList("structureId", false)));
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        Whitebox.invokeMethod(settingsService, "create",
                STRUCTURE_ID, new JsonObject().put(Field.ALERT_ABSENCE_THRESHOLD, false).put(OTHER_KEY, OTHER_VALUE), null);
    }

    @Test
    public void testUpdate(TestContext ctx) throws Exception {
        String queryExpected = "UPDATE null.settings SET alert_absence_threshold = ? WHERE structure_id = ? RETURNING *;";
        Mockito.doAnswer((Answer<Void>) invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, queryExpected);
            ctx.assertEquals(params, new JsonArray(Arrays.asList(true, STRUCTURE_ID)));
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(), Mockito.any());

        Whitebox.invokeMethod(settingsService, "update",
                STRUCTURE_ID, new JsonObject().put(Field.ALERT_ABSENCE_THRESHOLD, true).put(OTHER_KEY, OTHER_VALUE), null);
    }
}
