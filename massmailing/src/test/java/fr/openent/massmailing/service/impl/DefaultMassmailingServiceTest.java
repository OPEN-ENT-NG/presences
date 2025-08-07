package fr.openent.massmailing.service.impl;

import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.MassmailingType;
import fr.openent.presences.common.presences.Presences;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jRest;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class DefaultMassmailingServiceTest {

    private final Neo4j neo4j = Neo4j.getInstance();
    private final Neo4jRest neo4jRest = mock(Neo4jRest.class);
    private Vertx vertx;
    private DefaultMassmailingService massmailingService;

    @Before
    public void setUp() throws NoSuchFieldException {
        vertx = Vertx.vertx();
        Presences.getInstance().init(vertx.eventBus());
        Sql.getInstance().init(vertx.eventBus(), "fr.openent.massmailing");
        FieldSetter.setField(neo4j, neo4j.getClass().getDeclaredField("database"), neo4jRest);
        this.massmailingService = new DefaultMassmailingService(vertx.eventBus());
    }

    @Test
    public void testGetCountEventByStudent(TestContext ctx) {
        Async async = ctx.async();

        String structure = "structure";
        MassmailingType type = MassmailingType.LATENESS;
        Boolean massmailed = false;
        List<Integer> reasons = Arrays.asList(1, 2);
        List<Integer> punishmentsTypes = Arrays.asList(3, 4);
        List<Integer> sanctionsTypes = Arrays.asList(5, 6);
        Integer startAt = 10;
        String startDate = "startDate";
        String endDate = "endDate";
        List<String> students = Arrays.asList("student1", "student2");
        boolean noReasons = true;
        String expectedQuery = "{\"eventType\":2,\"justified\":null,\"students\":[\"student1\",\"student2\"]," +
                "\"structure\":\"structure\",\"startAt\":10,\"reasonsId\":[1,2],\"massmailed\":false,\"startDate\":\"startDate\"," +
                "\"endDate\":\"endDate\",\"regularized\":null,\"noReasons\":true,\"recoveryMethod\":\"HOUR\"," +
                "\"action\":\"get-count-event-by-student\"}";


        vertx.eventBus().consumer("fr.openent.presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(body.toString(), expectedQuery);
            async.complete();
        });

        massmailingService.getCountEventByStudent(structure, type, massmailed, reasons, punishmentsTypes, sanctionsTypes, startAt, startDate, endDate, students, noReasons, null);
    }

    @Test
    public void testGetStatus(TestContext ctx) {
        Async async = ctx.async();

        String structure = "structure";
        MassmailingType type = MassmailingType.NO_REASON;
        Boolean massmailed = false;
        List<Integer> reasons = Arrays.asList(1, 2);
        List<Integer> punishmentsTypes = Arrays.asList(3, 4);
        List<Integer> sanctionsTypes = Arrays.asList(5, 6);
        Integer startAt = 10;
        String startDate = "startDate";
        String endDate = "endDate";
        List<String> students = Arrays.asList("student1", "student2");
        boolean noReasons = true;
        String expectedQuery = "{\"eventType\":1,\"justified\":null,\"students\":[\"student1\",\"student2\"]," +
                "\"structure\":\"structure\",\"startAt\":10,\"reasonsId\":[],\"massmailed\":false,\"startDate\":\"startDate\"," +
                "\"endDate\":\"endDate\",\"regularized\":null,\"noReasons\":true,\"recoveryMethod\":null," +
                "\"action\":\"get-count-event-by-student\"}";

        vertx.eventBus().consumer("fr.openent.presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(body.toString(), expectedQuery);
            async.complete();
        });

        massmailingService.getStatus(structure, type, massmailed, reasons, punishmentsTypes, sanctionsTypes, startAt, startDate, endDate, students, noReasons, new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {

            }
        });
    }

    @Test
    public void testGetStudentsPrimaryRelativesIds(TestContext ctx) {
        Async async = ctx.async();
        List<String> students = Arrays.asList("student1", "student2");

        String expectedQuery = "{\"action\":\"eleve.getPrimaryRelatives\",\"studentIds\":[\"student1\",\"student2\"]}";

        vertx.eventBus().consumer("viescolaire", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(body.toString(), expectedQuery);
            async.complete();
        });

        try {
            Whitebox.invokeMethod(massmailingService, "getStudentsPrimaryRelativesIds", students, null);
        } catch (Exception e) {
            ctx.fail(e);
        }
    }

    @Test
    public void testGetAnomalies(TestContext ctx) {
        Async async = ctx.async();
        List<String> students = Arrays.asList("student1", "student2");
        MailingType type = MailingType.MAIL;

        String expectedQuery = "MATCH (u:User)-[:RELATED]->(r:User) WHERE u.id IN {users} " +
                " WITH u, r " +
                " MATCH (u)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
                "WITH u, collect(r.email) as emails " +
                "WHERE size(coalesce(emails)) = 0 RETURN DISTINCT u.id as id, (u.lastName + ' ' + u.firstName) as displayName, c.name as className";

        String expectedParams = "{\"users\":[\"student1\",\"student2\"]}";

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String result = invocation.getArgument(0);
            JsonObject params = invocation.getArgument(1);
            ctx.assertEquals(result, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(neo4jRest).execute(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));
        massmailingService.getAnomalies(type, students, null);
    }

    @Test
    public void testGetStudentRelatives(TestContext ctx) {
        Async async = ctx.async();
        List<String> students = Arrays.asList("student1", "student2");
        MailingType type = MailingType.MAIL;

        String expectedQuery = "MATCH (u:User)-[:RELATED]->(r:User) WHERE u.id IN {students} " +
                " WITH u, r " +
                " MATCH (u)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
                " RETURN u.id as id, (u.lastName + ' ' + u.firstName)" +
                " AS displayName, c.name AS className, collect({id: r.id, displayName: (r.lastName + ' ' + r.firstName)," +
                " contact: r.email}) AS relative";

        String expectedParams = "{\"students\":[\"student1\",\"student2\"]}";

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String result = invocation.getArgument(0);
            JsonObject params = invocation.getArgument(1);
            ctx.assertEquals(result, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(neo4jRest).execute(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        try {
            Whitebox.invokeMethod(massmailingService, "getStudentRelatives", type, students, null);
        } catch (Exception e) {
            ctx.fail(e);
        }
    }

    @Test
    public void getRelativeIdsFromList(TestContext ctx) {
        String studentId = "studentId";
        JsonObject jsonObject1 = new JsonObject().put("id", "1");
        JsonObject jsonObject2 = new JsonObject().put("id", "1");
        JsonArray studentsRelativeIds = new JsonArray(Arrays.asList(jsonObject1, jsonObject2));
        String expectedQuery = "[]";

        try {
            JsonArray result = Whitebox.invokeMethod(massmailingService, "getRelativeIdsFromList", studentId, studentsRelativeIds);
            ctx.assertEquals(result.toString(), expectedQuery);
            JsonObject jsonObject3 = new JsonObject().put("id", "studentId").put("primaryRelatives", new JsonArray(Arrays.asList(1, 2, 3)));
            studentsRelativeIds = new JsonArray(Arrays.asList(jsonObject1, jsonObject2, jsonObject3));
            result = Whitebox.invokeMethod(massmailingService, "getRelativeIdsFromList", studentId, studentsRelativeIds);
            expectedQuery = "[1,2,3]";
            ctx.assertEquals(result.toString(), expectedQuery);
        } catch (Exception e) {
            ctx.fail(e);
        }
    }
}
