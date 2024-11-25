package fr.openent.presences.helper;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.WorkflowHelper;

import fr.openent.presences.enums.WorkflowActions;
import fr.openent.presences.service.impl.DefaultEventService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({EventQueryHelper.class}) //Prepare the static class you want to test
public class EventHelperTest {

    private EventHelper eventHelper;

    @Before
    public void setUp() {
        eventHelper = new EventHelper(Vertx.vertx().eventBus());
    }

    @Test
    public void testGetCreationStatementShould_Return_Correct_StatementObject(TestContext ctx) {
        UserInfos userInfos = new UserInfos();
        userInfos.setUserId("fzafazffaf56");
        userInfos.setAuthorizedActions(new ArrayList<>());

        JsonObject event = new JsonObject()
                .put("start_date", "2021-11-14 00:00:00")
                .put("end_date", "2021-11-14 23:59:59")
                .put("comment", "test")
                .put("student_id", "afd4ecyloupm52wndkr6")
                .put("register_id", 350)
                .put("type_id", 1)
                .put("reason_id", 50);

        String expectedQuery = "INSERT INTO " + Presences.dbSchema + ".event (start_date, end_date, comment, counsellor_input, student_id, register_id, type_id, owner, reason_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "RETURNING id, start_date, end_date, comment, counsellor_input, student_id, register_id, type_id, reason_id;";
        JsonArray expectedParams = new JsonArray()
                .add(event.getString("start_date"))
                .add(event.getString("end_date"))
                .add(event.containsKey("comment") ? event.getString("comment") : "")
                .add(WorkflowHelper.hasRight(userInfos, WorkflowActions.MANAGE.toString()))
                .add(event.getString("student_id"))
                .add(event.getInteger("register_id"))
                .add(event.getInteger("type_id"))
                .add(userInfos.getUserId())
                .add(event.getInteger("reason_id"));

        JsonObject expectedStatement = new JsonObject()
                .put("action", "prepared")
                .put("statement", expectedQuery)
                .put("values", expectedParams);

        ctx.assertEquals(this.eventHelper.getCreationStatement(event, userInfos), expectedStatement);
    }

    @Test
    public void testGetCreationStatement_Without_Reason_Should_Return_Correct_StatementObject(TestContext ctx) {
        UserInfos userInfos = new UserInfos();
        userInfos.setUserId("fzafazffaf56");
        userInfos.setAuthorizedActions(new ArrayList<>());

        JsonObject event = new JsonObject()
                .put("start_date", "2021-11-14 00:00:00")
                .put("end_date", "2021-11-14 23:59:59")
                .put("comment", "test")
                .put("student_id", "afd4ecyloupm52wndkr6")
                .put("register_id", 350)
                .put("type_id", 1);

        String expectedQuery = "INSERT INTO " + Presences.dbSchema + ".event (start_date, end_date, comment, counsellor_input, student_id, register_id, type_id, owner, reason_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "RETURNING id, start_date, end_date, comment, counsellor_input, student_id, register_id, type_id, reason_id;";
        JsonArray expectedParams = new JsonArray()
                .add(event.getString("start_date"))
                .add(event.getString("end_date"))
                .add(event.containsKey("comment") ? event.getString("comment") : "")
                .add(WorkflowHelper.hasRight(userInfos, WorkflowActions.MANAGE.toString()))
                .add(event.getString("student_id"))
                .add(event.getInteger("register_id"))
                .add(event.getInteger("type_id"))
                .add(userInfos.getUserId())
                .addNull();

        JsonObject expectedStatement = new JsonObject()
                .put("action", "prepared")
                .put("statement", expectedQuery)
                .put("values", expectedParams);

        ctx.assertEquals(this.eventHelper.getCreationStatement(event, userInfos), expectedStatement);
    }

    @Test
    public void testGetDeletionEventStatement_Should_Return_Correct_StatementObject(TestContext ctx) {

        JsonObject event = new JsonObject()
                .put("register_id", 350)
                .put("student_id", "afd4ecyloupm52wndkr6");

        String query = "DELETE FROM " + Presences.dbSchema + ".event WHERE type_id IN (2, 3) AND register_id = ? AND student_id = ?";
        JsonArray params = new JsonArray()
                .add(event.getInteger("register_id"))
                .add(event.getString("student_id"));

        JsonObject expectedStatement = new JsonObject()
                .put("action", "prepared")
                .put("statement", query)
                .put("values", params);

        ctx.assertEquals(this.eventHelper.getDeletionEventStatement(event), expectedStatement);

    }

    @Test
    public void testFilterLatenessReasons(TestContext ctx) throws Exception {
        List<String> listReasonIds = Arrays.asList("reason1", "reason2");
        Boolean noReasonLateness = true;
        JsonArray params = new JsonArray();
        String expectedQuery = "((reason_id IN (?,?) OR reason_id IS NULL) AND type_id = 2)";
        JsonArray expectedParams = new JsonArray(Arrays.asList("reason1", "reason2"));
        String res = Whitebox.invokeMethod(EventQueryHelper.class, "filterLatenessReasons", listReasonIds, noReasonLateness, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());
        ctx.assertEquals(expectedParams, params);

        params = new JsonArray();
        noReasonLateness = false;

        expectedQuery = "((reason_id IN (?,?)) AND type_id = 2)";
        expectedParams = new JsonArray(Arrays.asList("reason1", "reason2"));
        res = Whitebox.invokeMethod(EventQueryHelper.class, "filterLatenessReasons", listReasonIds, noReasonLateness, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());
        ctx.assertEquals(expectedParams, params);

        params = new JsonArray();
        listReasonIds = Arrays.asList();

        expectedQuery = "";
        expectedParams = new JsonArray(Arrays.asList());
        res = Whitebox.invokeMethod(EventQueryHelper.class, "filterLatenessReasons", listReasonIds, noReasonLateness, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());
        ctx.assertEquals(expectedParams, params);

        params = new JsonArray();
        noReasonLateness = true;

        expectedQuery = "((reason_id IS NULL) AND type_id = 2)";
        expectedParams = new JsonArray(Arrays.asList());
        res = Whitebox.invokeMethod(EventQueryHelper.class, "filterLatenessReasons", listReasonIds, noReasonLateness, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());
        ctx.assertEquals(expectedParams, params);
    }

    @Test
    public void testFilterAbsenceReasons(TestContext ctx) throws Exception {
        List<String> listReasonIds = Arrays.asList("reason1", "reason2");
        Boolean noReason = true;
        Boolean regularized = true;
        Boolean followed = true;
        JsonArray params = new JsonArray();

        String expectedQuery = "((followed = true AND (reason_id IN (?,?) AND counsellor_regularisation = true OR reason_id IS NULL)) AND type_id = 1)";
        JsonArray expectedParams = new JsonArray(Arrays.asList("reason1", "reason2"));

        String res = Whitebox.invokeMethod(EventQueryHelper.class, "filterAbsenceReasons", listReasonIds, regularized, followed, noReason, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());
        ctx.assertEquals(expectedParams, params);

        params = new JsonArray();
        followed = false;

        expectedQuery = "((followed = false AND (reason_id IN (?,?) AND counsellor_regularisation = true OR reason_id IS NULL)) AND type_id = 1)";
        expectedParams = new JsonArray(Arrays.asList("reason1", "reason2"));

        res = Whitebox.invokeMethod(EventQueryHelper.class, "filterAbsenceReasons", listReasonIds, regularized, followed, noReason, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());
        ctx.assertEquals(expectedParams, params);

        params = new JsonArray();
        followed = null;

        expectedQuery = "((reason_id IN (?,?) AND counsellor_regularisation = true OR reason_id IS NULL) AND type_id = 1)";
        expectedParams = new JsonArray(Arrays.asList("reason1", "reason2"));

        res = Whitebox.invokeMethod(EventQueryHelper.class, "filterAbsenceReasons", listReasonIds, regularized, followed, noReason, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());
        ctx.assertEquals(expectedParams, params);

        params = new JsonArray();
        noReason = null;

        expectedQuery = "((reason_id IN (?,?) AND counsellor_regularisation = true) AND type_id = 1)";
        expectedParams = new JsonArray(Arrays.asList("reason1", "reason2"));

        res = Whitebox.invokeMethod(EventQueryHelper.class, "filterAbsenceReasons", listReasonIds, regularized, followed, noReason, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());
        ctx.assertEquals(expectedParams, params);

        params = new JsonArray();
        regularized = false;

        expectedQuery = "((reason_id IN (?,?) AND counsellor_regularisation = false) AND type_id = 1)";
        expectedParams = new JsonArray(Arrays.asList("reason1","reason2"));

        res = Whitebox.invokeMethod(EventQueryHelper.class, "filterAbsenceReasons", listReasonIds, regularized, followed, noReason, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());
        ctx.assertEquals(expectedParams, params);

        params = new JsonArray();
        regularized = null;

        expectedQuery = "((reason_id IN (?,?)) AND type_id = 1)";
        expectedParams = new JsonArray(Arrays.asList("reason1","reason2"));

        res = Whitebox.invokeMethod(EventQueryHelper.class, "filterAbsenceReasons", listReasonIds, regularized, followed, noReason, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());
        ctx.assertEquals(expectedParams, params);

        params = new JsonArray();
        listReasonIds = Arrays.asList();
        noReason = true;
        regularized = true;
        followed = true;

        expectedQuery = "((followed = true AND (counsellor_regularisation = true OR reason_id IS NULL)) AND type_id = 1)";
        expectedParams = new JsonArray(Arrays.asList());

        res = Whitebox.invokeMethod(EventQueryHelper.class, "filterAbsenceReasons", listReasonIds, regularized, followed, noReason, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());
        ctx.assertEquals(expectedParams, params);

        noReason = false;
        regularized = null;

        expectedQuery = "((followed = true) AND type_id = 1)";
        expectedParams = new JsonArray(Arrays.asList());

        res = Whitebox.invokeMethod(EventQueryHelper.class, "filterAbsenceReasons", listReasonIds, regularized, followed, noReason, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());
        ctx.assertEquals(expectedParams, params);

        followed = null;

        expectedQuery = "";
        expectedParams = new JsonArray(Arrays.asList());

        res = Whitebox.invokeMethod(EventQueryHelper.class, "filterAbsenceReasons", listReasonIds, regularized, followed, noReason, params);
        ctx.assertEquals(expectedQuery, res);
        ctx.assertEquals(res.length() - res.replace("?","").length(), params.size());
        ctx.assertEquals(expectedParams, params);
    }
}