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
}