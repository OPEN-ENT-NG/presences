package fr.openent.presences.common.statistics_presences;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class StatisticsPresencesTest {
    private Vertx vertx;
    private StatisticsPresences statisticsPresences;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        statisticsPresences = StatisticsPresences.getInstance();
        statisticsPresences.init(vertx.eventBus());
    }

    @Test
    public void testGetStatistics(TestContext ctx) {
        Async async = ctx.async();
        JsonObject filterObjectMock = new JsonObject();
        filterObjectMock.put("data", "Some data");
        String structure = "structureId";
        String indicator = "indicator";
        int page = 0;

        vertx.eventBus().consumer("fr.openent.statistics.presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("get-statistics", body.getString("action"));
            ctx.assertEquals(structure, body.getString("structureId"));
            ctx.assertEquals(indicator, body.getString("indicator"));
            ctx.assertEquals(page, body.getInteger("page"));
            ctx.assertEquals(filterObjectMock, body.getJsonObject("filter"));
            async.complete();
        });

        try {
            Whitebox.invokeMethod(this.statisticsPresences, "getStatistics", filterObjectMock, structure, indicator, page, (Handler) e -> {
            });
        } catch (Exception e) {
            ctx.assertNotNull(e);
        }
    }

    @Test
    public void testGetStatisticsGraph(TestContext ctx) {
        Async async = ctx.async();
        JsonObject filterObjectMock = new JsonObject();
        filterObjectMock.put("data", "Some data");
        String structure = "structureId";
        String indicator = "indicator";

        vertx.eventBus().consumer("fr.openent.statistics.presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("get-statistics-graph", body.getString("action"));
            ctx.assertEquals(structure, body.getString("structureId"));
            ctx.assertEquals(indicator, body.getString("indicator"));
            ctx.assertEquals(filterObjectMock, body.getJsonObject("filter"));
            async.complete();
        });

        try {
            Whitebox.invokeMethod(this.statisticsPresences, "getStatisticsGraph", filterObjectMock, structure, indicator, (Handler) e -> {
            });
        } catch (Exception e) {
            ctx.assertNotNull(e);
        }
    }

    @Test
    public void testGetStatisticsIndicator(TestContext ctx) {
        Async async = ctx.async();

        vertx.eventBus().consumer("fr.openent.statistics.presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("get-statistics-indicator", body.getString("action"));
            async.complete();
        });

        try {
            Whitebox.invokeMethod(this.statisticsPresences, "getStatisticsIndicator", (Handler) e -> {
            });
        } catch (Exception e) {
            ctx.assertNotNull(e);
        }
    }

    @Test
    public void testPostUsers(TestContext ctx) {
        Async async = ctx.async();
        String structure = "structureId";
        List<String> studentIds = new ArrayList<>(Arrays.asList("id1", "id2", "id3"));

        vertx.eventBus().consumer("fr.openent.statistics.presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("post-users", body.getString("action"));
            ctx.assertEquals(structure, body.getString("structureId"));
            ctx.assertEquals(studentIds, body.getJsonArray("studentIds").getList());
            async.complete();
        });

        try {
            Whitebox.invokeMethod(this.statisticsPresences, "postUsers", structure, studentIds, (Handler) e -> {
            });
        } catch (Exception e) {
            ctx.assertNotNull(e);
        }
    }
}
