package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.db.DB;
import fr.openent.presences.db.DBService;
import fr.openent.presences.service.CommonPresencesServiceFactory;
import fr.openent.presences.service.EventService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class DefaultEventServiceTest extends DBService {

    Sql sql = Mockito.mock(Sql.class);

    private EventService eventService;

    @Before
    public void setUp() {
        DB.getInstance().init(null, sql, null);
        CommonPresencesServiceFactory commonPresencesServiceFactory = new CommonPresencesServiceFactory(Vertx.vertx(), null, new JsonObject());
        this.eventService = new DefaultEventService(commonPresencesServiceFactory);
    }

    @Test
    public void testFetchingEventBetweenDates_Should_Return_Correct_Query(TestContext ctx) {

        String startDate = "2021-11-14 00:00:00";
        String endDate = "2021-11-14 23:59:59";
        List<String> users = new ArrayList<>(Collections.singletonList("4fg5de1d2saeggt85rggz"));
        List<Integer> eventType = new ArrayList<>(Collections.singletonList(1));
        String structureId = "f4e4q6v2r3h9o8k7y1fgzhwmalq";

        String expectedQuery = "SELECT e.* FROM " + Presences.dbSchema + ".event AS e" +
                " INNER JOIN " + Presences.dbSchema + ".register AS r ON (r.id = e.register_id AND r.structure_id = ?)" +
                " WHERE ? < e.end_date AND e.start_date < ?  AND e.student_id IN (?) " +
                " AND type_id IN (?) ";
        JsonArray expectedParam = new JsonArray()
                .add(structureId)
                .add(startDate)
                .add(endDate)
                .add("4fg5de1d2saeggt85rggz")
                .add(1);

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);

            ctx.assertEquals(queryResult, expectedQuery);
            ctx.assertEquals(paramsResult, expectedParam);

            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        eventService.getEventsBetweenDates(startDate, endDate, users, eventType, structureId);

    }
}