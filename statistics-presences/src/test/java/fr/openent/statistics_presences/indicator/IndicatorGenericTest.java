package fr.openent.statistics_presences.indicator;

import fr.openent.presences.common.incidents.Incidents;
import fr.openent.presences.common.presences.Presences;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.util.Arrays;
import java.util.List;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({Incidents.class, Presences.class}) //Prepare the static class you want to mock
public class IndicatorGenericTest {
    private Vertx vertx;

    @Before
    public void setUp() throws NoSuchFieldException {
        vertx = Vertx.vertx();
        Sql.getInstance().init(vertx.eventBus(), "fr.openent.statistics-presences");
    }

    @Test
    public void fetchIncidentValueTest(TestContext ctx) {
        Async async = ctx.async();
        List<Integer> reasonsId = Arrays.asList(8,9);
        String expectedQuery = "SELECT select query FROM null.incident INNER JOIN null.protagonist ON (incident.id = protagonist.incident_id) " +
                "WHERE incident.structure_id = ? AND protagonist.user_id = ? AND incident.date >= ? AND incident.date <= ? GROUP BY group";
        String expectedParams = "[\"structureId\",\"studentId\",\"startDate\",\"endDate\"]";

        vertx.eventBus().consumer("fr.openent.statistics-presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams, body.getJsonArray("values").toString());
            async.complete();
        });

        IndicatorGeneric.fetchIncidentValue("structureId", "studentId", "select query", "group", "startDate", "endDate");

        async.awaitSuccess(10000);
    }

    @Test
    public void fetchIncidentValueTestWithoutDate(TestContext ctx) {
        Async async = ctx.async();
        List<Integer> reasonsId = Arrays.asList(8,9);
        String expectedQuery = "SELECT select query FROM null.incident INNER JOIN null.protagonist ON (incident.id = protagonist.incident_id) " +
                "WHERE incident.structure_id = ? AND protagonist.user_id = ? GROUP BY group";
        String expectedParams = "[\"structureId\",\"studentId\"]";

        vertx.eventBus().consumer("fr.openent.statistics-presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams, body.getJsonArray("values").toString());
            async.complete();
        });

        IndicatorGeneric.fetchIncidentValue("structureId", "studentId", "select query", "group");

        async.awaitSuccess(10000);
    }

    @Test
    public void retrieveEventCountTest(TestContext ctx) {
        Async async = ctx.async();
        List<Integer> reasonsId = Arrays.asList(8,9);
        String expectedQuery = "SELECT select query FROM null.event INNER JOIN null.register ON (event.register_id = register.id) " +
                "WHERE event.student_id = ? AND register.structure_id = ? AND event.type_id = ? " +
                "AND (event.reason_id IS NULL OR event.reason_id IN (?,?)) AND event.start_date >= ? " +
                "AND event.end_date <= ? GROUP BY ? ";
        String expectedParams = "[\"studentId\",\"structureId1\",1,8,9,\"startDate\",\"endDate\",\"group\"]";

        vertx.eventBus().consumer("fr.openent.statistics-presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams, body.getJsonArray("values").toString());
            async.complete();
        });

        IndicatorGeneric.retrieveEventCount("structureId1", "studentId", 1, "select query", "group", reasonsId, "startDate", "endDate");

        async.awaitSuccess(10000);
    }

    @Test
    public void retrieveEventCountTestWithoutDate(TestContext ctx) {
        Async async = ctx.async();
        List<Integer> reasonsId = Arrays.asList(8,9);
        String expectedQuery = "SELECT select query FROM null.event INNER JOIN null.register ON (event.register_id = register.id) " +
                "WHERE event.student_id = ? AND register.structure_id = ? AND event.type_id = ? " +
                "AND (event.reason_id IS NULL OR event.reason_id IN (?,?)) GROUP BY ? ";
        String expectedParams = "[\"studentId\",\"structureId1\",1,8,9,\"group\"]";

        vertx.eventBus().consumer("fr.openent.statistics-presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams, body.getJsonArray("values").toString());
            async.complete();
        });

        IndicatorGeneric.retrieveEventCount("structureId1", "studentId", 1, "select query", "group", reasonsId);

        async.awaitSuccess(10000);
    }

    @Test
    public void fetchEventsFromPresencesTest(TestContext ctx) {
        Presences presences = Mockito.spy(Presences.getInstance());
        PowerMockito.spy(Presences.class);
        PowerMockito.when(Presences.getInstance()).thenReturn(presences);

        PowerMockito.doNothing().when(presences).getEventsByStudent(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        List<Integer> reasonsId = Arrays.asList(8, 9);
        IndicatorGeneric.fetchEventsFromPresences("structureId1", "studentId", reasonsId, false, false);
        IndicatorGeneric.fetchEventsFromPresences("structureId2", "studentId", reasonsId, false, false, "2000-01-01", "2010-10-10");

        Mockito.verify(presences, Mockito.times(1)).getEventsByStudent(Mockito.any(), Mockito.any(), Mockito.eq("structureId1"), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(presences, Mockito.times(1)).getEventsByStudent(Mockito.any(), Mockito.any(), Mockito.eq("structureId2"), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void retrievePunishmentsTest(TestContext ctx) {
        Incidents incidents = Mockito.spy(Incidents.getInstance());
        PowerMockito.spy(Incidents.class);
        PowerMockito.when(Incidents.getInstance()).thenReturn(incidents);
        PowerMockito.doNothing().when(incidents).getPunishmentsByStudent(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        IndicatorGeneric.retrievePunishments("structureId1", "studentId", "eventType");
        IndicatorGeneric.retrievePunishments("structureId2", "studentId", "eventType", "startDate", "endDate");

        Mockito.verify(incidents, Mockito.times(1)).getPunishmentsByStudent(Mockito.eq("structureId1"), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(incidents, Mockito.times(1)).getPunishmentsByStudent(Mockito.eq("structureId2"), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }
}
