package fr.openent.presences.service.impl;

import fr.openent.presences.common.service.*;
import fr.openent.presences.common.service.impl.*;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.db.*;
import fr.openent.presences.db.DB;
import fr.openent.presences.service.*;
import io.vertx.core.*;
import io.vertx.core.json.*;
import io.vertx.ext.unit.*;
import io.vertx.ext.unit.junit.*;
import org.entcore.common.neo4j.*;
import org.entcore.common.sql.*;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.stubbing.*;
import org.powermock.reflect.Whitebox;

import java.util.*;


@RunWith(VertxUnitRunner.class)
public class GroupServiceTest extends DBService {

    Neo4j neo4j = Mockito.mock(Neo4j.class);

    private GroupService groupService;

    @Before
    public void setUp() {
        DB.getInstance().init(neo4j, null, null);
        this.groupService = new DefaultGroupService(Vertx.vertx().eventBus());
    }

    @Test
    public void testGetGroupStudents(TestContext ctx) {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonObject params = invocation.getArgument(1);

            ctx.assertEquals(params, new JsonObject()
                    .put("ids", new JsonArray(Collections.singletonList(Field.GROUP))));

            return null;
        }).when(neo4j).execute(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        groupService.getGroupStudents(Field.GROUP, handler -> {});
    }

    @Test
    public void testGetFirstCounsellorId(TestContext ctx) throws Exception {

        String structureId = "AA";
        List<String> groupIds = Arrays.asList("GA", "GB");


        String expected = "MATCH (u:User {profiles: ['Student']})-[:IN]->(pg:ProfileGroup)-[:DEPENDS]" +
                "->(s:Structure {id:{structureId}})  " +
                " OPTIONAL MATCH (c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) " +
                " OPTIONAL MATCH (g:Group)<-[:IN]-(u) " +
                " WITH u, c, g " +
                " WHERE c.id IN {groupIds} OR g.id IN {groupIds} " +
                " RETURN DISTINCT u.id as id, (u.lastName + ' ' + u.firstName) as displayName, u.lastName as lastName, " +
                " u.firstName as firstName, 'USER' as type, COLLECT(DISTINCT {id: c.id, name: c.name}) as classes,  " +
                " COLLECT(DISTINCT {id: g.id, name: g.name}) as groups " +
                " ORDER BY displayName";

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonObject paramsResult = invocation.getArgument(1);

            ctx.assertEquals(queryResult.trim().replaceAll("\\s+"," "),
                    expected.trim().replaceAll("\\s+"," "));
            ctx.assertEquals(paramsResult, new JsonObject()
                    .put(Field.STRUCTUREID, structureId)
                    .put(Field.GROUPIDS, groupIds));

            return null;
        }).when(neo4j).execute(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        Whitebox.invokeMethod(groupService, "getGroupStudents", structureId, groupIds);
    }
}