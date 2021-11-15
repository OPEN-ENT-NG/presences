package fr.openent.presences.helper;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.WorkflowHelper;

import fr.openent.presences.enums.WorkflowActions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;


@RunWith(VertxUnitRunner.class)
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
}