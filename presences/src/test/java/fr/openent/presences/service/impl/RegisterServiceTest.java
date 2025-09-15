package fr.openent.presences.service.impl;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DB;
import fr.openent.presences.service.AbsenceService;
import fr.openent.presences.service.CommonPresencesServiceFactory;
import fr.openent.presences.service.RegisterService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class RegisterServiceTest {

    private RegisterService registerService;

    @Before
    public void setUp() {
        CommonPresencesServiceFactory commonPresencesServiceFactory = new CommonPresencesServiceFactory(Vertx.vertx(), null, new JsonObject(), "");
        this.registerService = new DefaultRegisterService(commonPresencesServiceFactory);
    }

    @Test
    public void testFormatStudent(TestContext ctx) throws Exception {
        Integer registerId = 0;
        JsonObject student = new JsonObject().put(Field.ID, Field.ID)
                .put(Field.LASTNAME, Field.LASTNAME)
                .put(Field.FIRSTNAME, Field.FIRSTNAME)
                .put(Field.BIRTHDATE, Field.BIRTHDATE)
                .put(Field.GROUPID, Field.GROUPID);
        JsonObject event1 = new JsonObject().put("event","custom data1").put(Field.REGISTER_ID, 1);
        JsonObject event2 = new JsonObject().put("event","custom data2").put(Field.REGISTER_ID, 2);
        JsonObject event3 = new JsonObject().put("event","custom data3").put(Field.REGISTER_ID, 2);
        JsonArray events = new JsonArray()
                .add(event1)
                .add(event2)
                .add(event3);
        boolean lastCourseAbsent = false;
        String groupName = Field.GROUP_NAME;
        Boolean exempted = false;
        List<JsonObject> exemptions = new ArrayList<>();
        JsonArray forgottenNotebooks = new JsonArray();
        JsonObject result = Whitebox.invokeMethod(registerService, "formatStudent",
                registerId, student, events, lastCourseAbsent, groupName, exempted, exemptions, forgottenNotebooks);
        ctx.assertEquals(result.toString(), "{\"id\":\"id\",\"name\":\"lastName firstName\",\"birth_day\":\"birthDay\"," +
                "\"group\":\"groupId\",\"group_name\":\"group_name\",\"events\":[],\"last_course_absent\":false,\"forgotten_notebook\":false," +
                "\"day_history\":[{\"event\":\"custom data1\",\"register_id\":1},{\"event\":\"custom data2\",\"register_id\":2}," +
                "{\"event\":\"custom data3\",\"register_id\":2}],\"exempted\":false,\"exemptions\":[]}");

        registerId = 1;
        result = Whitebox.invokeMethod(registerService, "formatStudent",
                registerId, student, events, lastCourseAbsent, groupName, exempted, exemptions, forgottenNotebooks);
        ctx.assertEquals(result.getJsonArray(Field.EVENTS).toString(), new JsonArray().add(event1).toString());

        registerId = 2;
        result = Whitebox.invokeMethod(registerService, "formatStudent",
                registerId, student, events, lastCourseAbsent, groupName, exempted, exemptions, forgottenNotebooks);
        ctx.assertEquals(result.getJsonArray(Field.EVENTS).toString(), new JsonArray().add(event2).add(event3).toString());

        lastCourseAbsent = true;
        result = Whitebox.invokeMethod(registerService, "formatStudent",
                registerId, student, events, lastCourseAbsent, groupName, exempted, exemptions, forgottenNotebooks);
        ctx.assertEquals(result.getBoolean(Field.LAST_COURSE_ABSENT), true);

        exempted = true;
        result = Whitebox.invokeMethod(registerService, "formatStudent",
                registerId, student, events, lastCourseAbsent, groupName, exempted, exemptions, forgottenNotebooks);
        ctx.assertEquals(result.getBoolean(Field.EXEMPTED), true);

        forgottenNotebooks = new JsonArray()
                .add(new JsonObject().put(Field.STUDENT_ID, Field.STUDENT_ID));
        result = Whitebox.invokeMethod(registerService, "formatStudent",
                registerId, student, events, lastCourseAbsent, groupName, exempted, exemptions, forgottenNotebooks);
        ctx.assertEquals(result.getBoolean(Field.FORGOTTEN_NOTEBOOK), false);

        forgottenNotebooks = new JsonArray()
                .add(new JsonObject().put(Field.STUDENT_ID, Field.STUDENT_ID))
                .add(new JsonObject().put(Field.STUDENT_ID, Field.ID));
        result = Whitebox.invokeMethod(registerService, "formatStudent",
                registerId, student, events, lastCourseAbsent, groupName, exempted, exemptions, forgottenNotebooks);
        ctx.assertEquals(result.getBoolean(Field.FORGOTTEN_NOTEBOOK), true);

        exemptions.add(new JsonObject().put("data", "exemptions1"));
        exemptions.add(new JsonObject().put("data", "exemptions2"));
        result = Whitebox.invokeMethod(registerService, "formatStudent",
                registerId, student, events, lastCourseAbsent, groupName, exempted, exemptions, forgottenNotebooks);
        ctx.assertEquals(result.getJsonArray(Field.EXEMPTIONS).toString(), new JsonArray(exemptions).toString());
    }
}
