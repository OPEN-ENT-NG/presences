package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.db.DB;
import fr.openent.presences.db.DBService;
import fr.openent.presences.service.*;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.*;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.*;
import org.entcore.common.sql.Sql;
import org.entcore.common.storage.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.*;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.*;

import java.util.Arrays;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class DefaultRegisterServiceTest extends DBService {

    private Vertx vertx;
    private final Sql sql = mock(Sql.class);
    private final Neo4j neo4j = Neo4j.getInstance();
    private final Neo4jRest neo4jRest = mock(Neo4jRest.class);


    private CommonPresencesServiceFactory commonPresencesServiceFactory;
    private RegisterService registerService;

    @Before
    public void setUp() throws NoSuchFieldException {
        vertx = Vertx.vertx();
        DB.getInstance().init(neo4j, sql, null);
        FieldSetter.setField(neo4j, neo4j.getClass().getDeclaredField("database"), neo4jRest);
        Sql.getInstance().init(vertx.eventBus(), "fr.openent.presences");
        this.commonPresencesServiceFactory = new CommonPresencesServiceFactory(Vertx.vertx(),
                new StorageFactory(Vertx.vertx(), null).getStorage(), new JsonObject(), "");
        this.registerService = new DefaultRegisterService(commonPresencesServiceFactory);
    }

    @Test
    public void testFetchingRegister_Should_Return_Correct_Query(TestContext ctx) {
        Integer registerId = 350;

        String query = "SELECT personnel_id, proof_id, course_id, owner, notified, subject_id, start_date, end_date, " +
                "structure_id, counsellor_input, state_id FROM " + Presences.dbSchema + ".register " +
                "WHERE register.id = ?";

        JsonArray params = new JsonArray()
                .add(registerId);

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);

            ctx.assertEquals(queryResult, query);
            ctx.assertEquals(paramsResult, params);

            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        registerService.fetchRegister(registerId);
    }


    @Test
    public void testGetFirstCounsellorId(TestContext ctx) throws Exception {

        String query = "MATCH (u:User)-[:IN]->(g:ProfileGroup)-[:DEPENDS]->(s:Structure {id:{structureId}}) " +
                "WHERE ANY(function IN u.functions WHERE function =~ '.*(?=\\\\$EDUCATION).*(?=EDU).*(?=\\\\$E0030).*') " +
                "OPTIONAL MATCH (u:User)-[:IN]->(:FunctionGroup {filter:'DIRECTION'})-[:DEPENDS]->(s:Structure {id:{structureId}}) " +
                "RETURN u.id as id";

        JsonObject params = new JsonObject().put("structureId", "structureId");

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonObject paramsResult = invocation.getArgument(1);

            ctx.assertEquals(queryResult, query);
            ctx.assertEquals(paramsResult, params);

            return null;
        }).when(neo4jRest).execute(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        Whitebox.invokeMethod(registerService, "getFirstCounsellorId", "structureId",
                null);
    }

    @Test
    public void testUpdateStatus(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "UPDATE null.register SET state_id = ? WHERE id = ? AND state_id != 3";
        JsonArray expectedParams = new JsonArray(Arrays.asList(2,1));

        vertx.eventBus().consumer("fr.openent.presences", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });
        registerService.updateStatus(1, 2, null);
    }

}