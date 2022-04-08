package fr.openent.incidents.service.impl;

import fr.openent.incidents.service.IncidentsTypeService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

@RunWith(VertxUnitRunner.class)
public class IncidentsTypeServiceTest {

    private Vertx vertx;

    private IncidentsTypeService incidentsService;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        Sql.getInstance().init(vertx.eventBus(), "fr.openent.incidents");
        this.incidentsService = new DefaultIncidentsTypeService();
    }

    @Test
    public void testFetchIncidentsType(TestContext ctx) throws Exception {
        Async async = ctx.async();

        String expectedQuery = "SELECT * FROM null.incident_type where structure_id = ?";
        JsonArray expectedParams = new JsonArray().add("structureId");

        vertx.eventBus().consumer("fr.openent.incidents", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });

        Whitebox.invokeMethod(this.incidentsService, "fetchIncidentsType", "structureId", null);
    }

    @Test
    public void testFetchUsedIncidentsType(TestContext ctx) throws Exception {
        Async async = ctx.async();

        String expectedQuery = "WITH ids AS (SELECT i.id, i.label FROM null.incident_type i WHERE structure_id = ?) " +
                "SELECT DISTINCT i.id, i.label FROM ids i WHERE (i.id IN (SELECT type_id FROM null.incident))";
        JsonArray expectedParams = new JsonArray().add("structureId");

        vertx.eventBus().consumer("fr.openent.incidents", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });

        Whitebox.invokeMethod(this.incidentsService, "fetchUsedIncidentsType", "structureId", null);
    }

    @Test
    public void testCreate(TestContext ctx) throws Exception {
        Async async = ctx.async();

        String expectedQuery = "INSERT INTO null.incident_type (structure_id, label, hidden) VALUES (?, ?, false) RETURNING id";
        JsonArray expectedParams = new JsonArray().add("structureId").add("label");
        JsonObject incidentTypeBody = new JsonObject().put("structureId", "structureId").put("label", "label");

        vertx.eventBus().consumer("fr.openent.incidents", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });

        Whitebox.invokeMethod(this.incidentsService, "create", incidentTypeBody, null);
    }

    @Test
    public void testPut(TestContext ctx) throws Exception {
        Async async = ctx.async();

        String expectedQuery = "UPDATE null.incident_type SET label = ?, hidden = ? WHERE id = ? RETURNING id";
        JsonArray expectedParams = new JsonArray().add("label").add(true).add(3);
        JsonObject incidentTypeBody = new JsonObject().put("label", "label").put("hidden", true).put("id", 3);

        vertx.eventBus().consumer("fr.openent.incidents", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });

        Whitebox.invokeMethod(this.incidentsService, "put", incidentTypeBody, null);
    }

    @Test
    public void testDelete(TestContext ctx) throws Exception {
        Async async = ctx.async();

        String expectedQuery = "DELETE FROM null.incident_type WHERE id = ? RETURNING id as id_deleted";
        JsonArray expectedParams = new JsonArray().add(3);

        vertx.eventBus().consumer("fr.openent.incidents", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });

        Whitebox.invokeMethod(this.incidentsService, "delete", 3, null);
    }
}