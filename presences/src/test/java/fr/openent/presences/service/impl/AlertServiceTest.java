package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.db.*;
import fr.openent.presences.db.DB;
import fr.openent.presences.service.*;
import fr.wseduc.webutils.eventbus.ResultMessage;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.json.*;
import io.vertx.ext.unit.*;
import io.vertx.ext.unit.junit.*;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.*;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.stubbing.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.reflect.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({Sql.class, Neo4j.class}) //Prepare the static class you want to test
public class AlertServiceTest extends DBService {

    Sql sql = PowerMockito.mock(Sql.class);
    Neo4j neo4j = PowerMockito.mock(Neo4j.class);
    Vertx vertx;

    private AlertService alertService;

    private static final String STRUCTURE_ID = "111";
    private static final String START = "2022-06-01";
    private static final String END = "2022-06-30";

    @Before
    public void setUp() {
        DB.getInstance().init(null, sql, null);
        this.alertService = new DefaultAlertService();
        this.vertx = Vertx.vertx();
        Sql.getInstance().init(vertx.eventBus(), "fr.openent.presences");
    }

    @Test
    public void testResetStudentAlertsCount(TestContext ctx) throws Exception {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(params, new JsonArray()
                    .add(Field.STUDENT_ID)
                    .add(Field.STRUCTURE_ID)
                    .add(Field.TYPE));

            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        Whitebox.invokeMethod(alertService, "resetStudentAlertsCount",
                Field.STRUCTURE_ID, Field.STUDENT_ID, Field.TYPE);


    }

    @Test
    public void testDelete(TestContext ctx) throws Exception {
        Async async = ctx.async();
        String expectedQuery = "DELETE FROM null.alerts WHERE structure_id = ?  AND ((student_id = ? AND type IN (?,?)) " +
                "OR (student_id = ? AND type IN (?))) AND created >= ?::date + '00:00:00'::time AND created <= ?::date + '23:59:59'::time ";
        String expectedParams = "[\"111\",\"student2\",\"ABSENCE\",\"LATENESS\",\"student1\",\"ABSENCE\",\"2022-06-01\",\"2022-06-30\"]";

        Map<String, List<String>> deletedAlertMap = new HashMap<>();
        deletedAlertMap.put("student1", Collections.singletonList("ABSENCE"));
        deletedAlertMap.put("student2", Arrays.asList("ABSENCE", "LATENESS"));

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(query, expectedQuery);

            ctx.assertEquals(params.toString(), expectedParams);

            async.complete();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        this.alertService.delete(STRUCTURE_ID, deletedAlertMap, START, END);
    }

    @Test
    public void testGetSummary(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT tc.type, count(*) AS count FROM (SELECT type, count(*) AS count FROM null.alerts" +
                " WHERE structure_id = ? GROUP BY student_id, type) as tc WHERE tc.count >= null.get_alert_thresholder(tc.type, ?)" +
                " GROUP BY tc.type;";
        String expectedParams = "[\"111\",\"111\"]";

        vertx.eventBus().consumer("fr.openent.presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams, body.getJsonArray("values").toString());
            async.complete();
        });

        this.alertService.getSummary(STRUCTURE_ID);
    }

    @Test
    public void testGetAlertsStudents(TestContext ctx) {
        Async async = ctx.strictAsync(2);
        PowerMockito.spy(Sql.class);
        PowerMockito.when(Sql.getInstance()).thenReturn(sql);
        PowerMockito.spy(Neo4j.class);
        PowerMockito.when(Neo4j.getInstance()).thenReturn(neo4j);

        String expectedQuerySql = "SELECT student_id, type, count(*) AS count FROM null.alerts" +
                " WHERE structure_id = ? AND type IN (?,?) AND student_id IN (?,?)" +
                "AND created >= ?::date + '00:00:00'::time AND created <= ?::date + '23:59:59'::time  " +
                "GROUP BY student_id, type HAVING count(*) >= null.get_alert_thresholder(type, ?);";
        String expectedParamsSql = "[\"111\",\"ABSENCE\",\"LATENESS\",\"student3\",\"student4\",\"2022-06-01\",\"2022-06-30\",\"111\"]";
        String expectedQueryNeo = "MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) WHERE u.id IN {studentsId}" +
                " RETURN u.firstName as firstName, u.lastName as lastName, c.name as audience, u.id as student_id;";
        String expectedParamsNeo = "{\"studentsId\":[\"student1\",\"student2\"]}";

        PowerMockito.doAnswer((Answer<Void>) invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuerySql);
            ctx.assertEquals(params.toString(), expectedParamsSql);

            Handler<Message<JsonObject>> handler = invocation.getArgument(2);
            handler.handle(new ResultMessage(new JsonObject().put("results", new JsonArray(Arrays.asList(
                    new JsonArray(Arrays.asList("student1")),
                    new JsonArray(Arrays.asList("student2"))
                    )))
                    .put("fields", new JsonArray(Arrays.asList("student_id")))));
            async.countDown();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        PowerMockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonObject paramResult = invocation.getArgument(1);
            ctx.assertEquals(queryResult, expectedQueryNeo);
            ctx.assertEquals(paramResult.toString(), expectedParamsNeo);
            async.countDown();
            return null;
        }).when(neo4j).execute(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));
        this.alertService.getAlertsStudents(STRUCTURE_ID, Arrays.asList("ABSENCE", "LATENESS"), Arrays.asList("student3", "student4"), START, END);
        async.awaitSuccess(100000);
    }

}