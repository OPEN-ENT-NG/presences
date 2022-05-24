package fr.openent.presences.common.service.impl;

import fr.openent.presences.common.service.UserService;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DB;
import fr.openent.presences.db.DBService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.Neo4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class DefaultUserServiceTest extends DBService {

    Neo4j neo4j = Mockito.mock(Neo4j.class);

    private UserService userService;
    private static final String STRUCTURE_ID = "111";
    private static final List<String> STUDENT_IDS = Arrays.asList("333", "444");

    @Before
    public void setUp() {
        DB.getInstance().init(neo4j, null, null);
        userService = new DefaultUserService();
    }

    @Test
    public void testGetStudents(TestContext ctx) throws Exception {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            String query = invocation.getArgument(0);
            JsonObject params = invocation.getArgument(1);

            ctx.assertEquals(query,"MATCH (u:User {profiles:['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]" +
                    "->(c:Class)-[:BELONGS]->(s:Structure)  WHERE s.id = {structureId} AND u.id IN {studentIds}" +
                    " AND (  (u.accommodation CONTAINS {halfBoarder}) OR  (u.accommodation CONTAINS {internal}) ) " +
                    " RETURN u.id as id, u.lastName + ' ' + u.firstName as name, u.lastName as lastName," +
                    " u.firstName as firstName, c.name as className, u.accommodation as accommodation");

            ctx.assertEquals(params,
                    new JsonObject()
                            .put(Field.STRUCTUREID, STRUCTURE_ID)
                            .put(Field.STUDENTIDS, STUDENT_IDS)
                            .put(Field.HALFBOARDER, DefaultUserService.HALF_BOARDER)
                            .put(Field.INTERNAL, DefaultUserService.INTERNAL)
            );
            return null;
        }).when(neo4j).execute(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        Whitebox.invokeMethod(userService, "getStudents",
                STRUCTURE_ID, STUDENT_IDS, true, true);
    }
}
