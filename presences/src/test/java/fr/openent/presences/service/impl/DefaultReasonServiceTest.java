package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.db.DB;
import fr.openent.presences.db.DBService;
import fr.openent.presences.service.ReasonService;
import fr.openent.statistics_presences.model.StatisticsFilter;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.minidev.json.JSONObject;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;
import scala.Int;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class DefaultReasonServiceTest extends DBService {

    private Vertx vertx;

    private ReasonService reasonService;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        Sql.getInstance().init(vertx.eventBus(), "fr.openent.presences");

        this.reasonService = new DefaultReasonService();
    }

    @Test
    public void testFetchReasonWithTypeId(TestContext ctx) {
        Async async = ctx.async();

        Integer reasonTypeId = 2;
        String structureId = "structureId";

        String expectedQuery = "SELECT * FROM " + Presences.dbSchema + ".reason " +
                "WHERE (structure_id = ? OR structure_id = '-1') " +
                "AND reason_type_id = ? " +
                "ORDER BY label ASC";


        JsonArray expectedParam = new JsonArray()
                .add(structureId)
                .add(reasonTypeId);

        vertx.eventBus().consumer("fr.openent.presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParam.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        reasonService.fetchReason(structureId, reasonTypeId);
    }

    @Test
    public void testFetchReasonWithoutTypeId(TestContext ctx) {
        Async async = ctx.async();

        Integer reasonTypeId = null;
        String structureId = "structureId";

        String expectedQuery = "SELECT * FROM " + Presences.dbSchema + ".reason " +
                "WHERE (structure_id = ? OR structure_id = '-1') " +
                "AND reason_type_id = ? " +
                "ORDER BY label ASC";

        JsonArray expectedParam = new JsonArray()
                .add(structureId)
                .add(1);

        vertx.eventBus().consumer("fr.openent.presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParam.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        reasonService.fetchReason(structureId, reasonTypeId);
    }

    @Test
    public void testCreateReasonWithTypeId(TestContext ctx) {
        Async async = ctx.async();

        Integer reasonTypeId = 1;

        String expectedQuery = "INSERT INTO " + Presences.dbSchema + ".reason " +
                "(structure_id, label, proving, comment, hidden, absence_compliance, reason_type_id)" +
                " VALUES (?, ?, ?, '', false, ?, ?) RETURNING id";

        JsonArray expectedParam = new JsonArray()
                .add("structureId")
                .add("label")
                .add(false)
                .add(true)
                .add(reasonTypeId);

        vertx.eventBus().consumer("fr.openent.presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParam.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        reasonService.create(getReasonBody());
    }

    @Test
    public void testCreateReasonWithoutTypeId(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "INSERT INTO " + Presences.dbSchema + ".reason " +
                "(structure_id, label, proving, comment, hidden, absence_compliance, reason_type_id)" +
                " VALUES (?, ?, ?, '', false, ?, ?) RETURNING id";

        JsonArray expectedParam = new JsonArray()
                .add("structureId")
                .add("label")
                .add(false)
                .add(true)
                .add(1);




        vertx.eventBus().consumer("fr.openent.presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParam.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        reasonService.create(getReasonBody());
    }

    @Test
    public void testFetchUsedReasonWithTypeId(TestContext ctx) throws Exception {
        Async async = ctx.async();

        String structureId = "structureId";
        Integer reasonTypeId = 1;

        String expectedQuery  = "SELECT DISTINCT r.id, r.label " +
                "FROM " + Presences.dbSchema + ".reason r " +
                "INNER JOIN " + Presences.dbSchema + ".event e on r.id = e.reason_id " +
                "WHERE (r.structure_id = ? OR r.structure_id = '-1') " +
                "AND r.reason_type_id = ? " +
                "UNION " +
                "SELECT DISTINCT r.id, r.label " +
                "FROM " + Presences.dbSchema + ".reason r " +
                "INNER JOIN " + Presences.dbSchema + ".absence a on r.id = a.reason_id " +
                "WHERE (r.structure_id = ? OR r.structure_id = '-1') " +
                "AND r.reason_type_id = ?";

        JsonArray expectedParam = new JsonArray()
                .add(structureId)
                .add(reasonTypeId)
                .add(structureId)
                .add(reasonTypeId);

        vertx.eventBus().consumer("fr.openent.presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParam.toString(), body.getJsonArray("values").toString());
            async.complete();
        });

        Whitebox.invokeMethod(reasonService, "fetchUsedReason", "structureId", reasonTypeId);
    }

    @Test
    public void testFetchUsedReasonWithOutTypeId(TestContext ctx) throws Exception {
        Async async = ctx.async();

        String structureId = "structureId";

        String expectedQuery  = "SELECT DISTINCT r.id, r.label " +
                "FROM " + Presences.dbSchema + ".reason r " +
                "INNER JOIN " + Presences.dbSchema + ".event e on r.id = e.reason_id " +
                "WHERE (r.structure_id = ? OR r.structure_id = '-1') " +
                "AND r.reason_type_id = ? " +
                "UNION " +
                "SELECT DISTINCT r.id, r.label " +
                "FROM " + Presences.dbSchema + ".reason r " +
                "INNER JOIN " + Presences.dbSchema + ".absence a on r.id = a.reason_id " +
                "WHERE (r.structure_id = ? OR r.structure_id = '-1') " +
                "AND r.reason_type_id = ?";

        JsonArray expectedParam = new JsonArray()
                .add(structureId)
                .add(1)
                .add(structureId)
                .add(1);


        vertx.eventBus().consumer("fr.openent.presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParam.toString(), body.getJsonArray("values").toString());
            async.complete();
        });

        Whitebox.invokeMethod(reasonService, "fetchUsedReason", "structureId", null);
    }

    private JsonObject getReasonBody() {
        return new JsonObject()
                .put("structureId", "structureId")
                .put("label", "label")
                .put("proving", false)
                .put("absenceCompliance", true);
    }

}