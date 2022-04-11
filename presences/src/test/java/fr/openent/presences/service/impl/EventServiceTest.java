package fr.openent.presences.service.impl;

import fr.openent.presences.core.constants.*;
import fr.openent.presences.db.*;
import fr.openent.presences.db.DB;
import fr.openent.presences.service.*;
import io.vertx.core.*;
import io.vertx.core.json.*;
import io.vertx.ext.unit.*;
import io.vertx.ext.unit.junit.*;
import org.entcore.common.sql.*;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.stubbing.*;

import java.util.*;

@RunWith(VertxUnitRunner.class)
public class EventServiceTest extends DBService {

    Sql sql = Mockito.mock(Sql.class);

    private EventService eventService;

    @Before
    public void setUp() {
        DB.getInstance().init(null, sql, null);
        this.eventService = new DefaultEventService(Vertx.vertx().eventBus());
    }

    @Test
    public void testGetEvents(TestContext ctx) {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(params, new JsonArray()
                    .add(Field.STRUCTURE_ID)
                    .add(Field.EVENT_TYPE)
                    .add(Field.START_DATE + " 00:00:00")
                    .add(Field.END_DATE + " 23:59:59")
                    .add(Field.END_TIME)
                    .add(Field.START_TIME)
                    .add(true)
                    .add(Field.REASON_ID)
                    .add(Field.REASON_ID)
                    .add(0)
                    .add(20));

            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        eventService.get(Field.STRUCTURE_ID, Field.START_DATE, Field.END_DATE,
                Field.START_TIME, Field.END_TIME,  Collections.singletonList(Field.EVENT_TYPE), Collections.singletonList(Field.REASON_ID), false, false, new ArrayList<>(),
                new ArrayList<>(), true, true, 0, handler -> {});
    }
}