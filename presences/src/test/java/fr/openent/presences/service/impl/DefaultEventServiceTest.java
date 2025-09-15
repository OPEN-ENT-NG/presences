package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.db.DB;
import fr.openent.presences.db.DBService;
import fr.openent.presences.helper.EventQueryHelper;
import fr.openent.presences.service.CommonPresencesServiceFactory;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(VertxUnitRunner.class)
@PrepareForTest({DefaultEventService.class, EventQueryHelper.class})
public class DefaultEventServiceTest extends DBService {

    Sql sql = Mockito.mock(Sql.class);

    private DefaultEventService eventService;

    @Before
    public void setUp() {
        DB.getInstance().init(null, sql, null);
        CommonPresencesServiceFactory commonPresencesServiceFactory = new CommonPresencesServiceFactory(Vertx.vertx(), null, new JsonObject(), "");
        this.eventService = PowerMockito.spy(new DefaultEventService(commonPresencesServiceFactory));
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

    @Test
    public void testSetParamsForQueryEvents(TestContext ctx) throws Exception {
        PowerMockito.spy(EventQueryHelper.class);
        List<String> listReasonIds = Arrays.asList("reason1", "reason2");
        List<String> userId = Arrays.asList("user1", "user2", "user3");
        Boolean regularized = true;
        Boolean followed = true;
        Boolean noReason = true;
        Boolean noReasonLateness = true;
        JsonArray userIdFromClasses = new JsonArray(Arrays.asList(new JsonObject().put("studentId","studentId1"), new JsonObject().put("studentId", "studentId2")));
        List<String> typeIds = Arrays.asList("1", "2", "3");
        JsonArray params = new JsonArray();

        PowerMockito.doReturn(" AND (paramsStudent)").when(EventQueryHelper.class, "filterStudentIds", Mockito.any(), Mockito.any());
        PowerMockito.doReturn(" AND (paramsReason)").when(EventQueryHelper.class, "filterReasons", Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        String expectedQuery = " AND ( (paramsStudent) AND (paramsReason))";
        JsonArray expectedParams = new JsonArray(Arrays.asList());

        String res = Whitebox.invokeMethod(eventService, "setParamsForQueryEvents", listReasonIds, userId, regularized, followed, noReason, noReasonLateness, userIdFromClasses, typeIds, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(expectedParams, params);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());

        PowerMockito.doReturn("").when(EventQueryHelper.class, "filterReasons", Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        expectedQuery = " AND ( (paramsStudent))";
        expectedParams = new JsonArray(Arrays.asList());

        res = Whitebox.invokeMethod(eventService, "setParamsForQueryEvents", listReasonIds, userId, regularized, followed, noReason, noReasonLateness, userIdFromClasses, typeIds, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(expectedParams, params);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());

        PowerMockito.doReturn("").when(EventQueryHelper.class, "filterStudentIds", Mockito.any(), Mockito.any());

        expectedQuery = "";
        expectedParams = new JsonArray(Arrays.asList());

        res = Whitebox.invokeMethod(eventService, "setParamsForQueryEvents", listReasonIds, userId, regularized, followed, noReason, noReasonLateness, userIdFromClasses, typeIds, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(expectedParams, params);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());

        PowerMockito.doReturn(" AND (paramsReason)").when(EventQueryHelper.class, "filterReasons", Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        expectedQuery = " AND (paramsReason)";
        expectedParams = new JsonArray(Arrays.asList());

        res = Whitebox.invokeMethod(eventService, "setParamsForQueryEvents", listReasonIds, userId, regularized, followed, noReason, noReasonLateness, userIdFromClasses, typeIds, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(expectedParams, params);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());
    }
}