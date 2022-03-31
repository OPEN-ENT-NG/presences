package fr.openent.statistics_presences.service.impl;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DBService;
import fr.openent.statistics_presences.bean.Register;
import fr.openent.statistics_presences.bean.timeslot.Slot;
import fr.openent.statistics_presences.bean.timeslot.Timeslot;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@RunWith(VertxUnitRunner.class)
public class DefaultStatisticsWeeklyAudiencesServiceTest extends DBService {
    private DefaultStatisticsWeeklyAudiencesService weeklyAudienceService;

    private static final String STRUCTURE_ID = "111";
    private static final Integer REGISTER_ID = 444;
    private static final String AUDIENCE_ID ="333";
    private static final Integer STATE_ID = 3;
    private static final Integer STUDENT_COUNT = 29;

    @Before
    public void setUp(TestContext context) {
        CommonServiceFactory commonServiceFactory = new CommonServiceFactory(Vertx.vertx());

        /* Indicator to test */
        this.weeklyAudienceService = new DefaultStatisticsWeeklyAudiencesService(commonServiceFactory);
    }

    @Test
    public void testMapSimpleRegisterToWeeklyAudience(TestContext ctx) throws Exception {
        List<JsonObject> expected = expectedMapSimpleRegisterToWeeklyAudience();

        Register register = getRegister();
        register.setStartAt("2022-03-08T08:55:00");
        register.setEndAt("2022-03-08T09:50:00");

        List<JsonObject> result =  Whitebox.invokeMethod(weeklyAudienceService, "mapRegistersToMongoWeeklyAudiences",
                Collections.singletonList(register), Collections.singletonList(getTimeslot()), Collections.singletonList(getGroupCountStudents()));

        ctx.assertEquals(result, expected);
    }

    @Test
    public void testMapSplittableRegisterToWeeklyAudience(TestContext ctx) throws Exception {
        List<JsonObject> expected = expectedMapSplittableRegisterToWeeklyAudience();

        Register register = getRegister();
        register.setStartAt("2022-03-08T08:55:00");
        register.setEndAt("2022-03-08T10:30:00");

        List<JsonObject> result =  Whitebox.invokeMethod(weeklyAudienceService, "mapRegistersToMongoWeeklyAudiences",
                Collections.singletonList(register), Collections.singletonList(getTimeslot()), Collections.singletonList(getGroupCountStudents()));

        ctx.assertEquals(result, expected);
    }

    private Timeslot getTimeslot() {
        Timeslot timeslot = new Timeslot(new JsonObject("{\"_id\":\"222\",\"name\":\"slots\",\"schoolId\":\"111\"," +
                "\"created\":{\"$date\":1637318354978},\"modified\":{\"$date\":1637319121341}," +
                "\"owner\":{\"userId\":\"555\",\"displayName\":\"test\"}," +
                "\"audienceId\":\"333\"}"));


        return timeslot.setSlots(
                Arrays.asList(
                        new Slot(new JsonObject("{\"name\":\"M1\",\"startHour\":\"08:00\",\"endHour\":\"08:55\",\"id\":\"2221\"}")),
                        new Slot(new JsonObject("{\"name\":\"M2\",\"startHour\":\"08:55\",\"endHour\":\"09:50\",\"id\":\"2222\"}")),
                        new Slot(new JsonObject("{\"name\":\"M3\",\"startHour\":\"10:05\",\"endHour\":\"11:00\",\"id\":\"2223\"}")),
                        new Slot(new JsonObject("{\"name\":\"M4\",\"startHour\":\"11:00\",\"endHour\":\"11:55\",\"id\":\"2224\"}"))
                )
        );
    }

    private Register getRegister() {
        return new Register(
                new JsonObject()
                        .put(Field.ID, REGISTER_ID)
                        .put(Field.GROUP_ID, AUDIENCE_ID)
                        .put(Field.STATE_ID, STATE_ID)
                        .put(Field.STRUCTURE_ID, STRUCTURE_ID)
        );
    }

    private JsonObject getGroupCountStudents() {
        return new JsonObject()
                .put(Field.ID_GROUPE, AUDIENCE_ID)
                .put(Field.NB, STUDENT_COUNT);
    }


    private List<JsonObject> expectedMapSimpleRegisterToWeeklyAudience() {
        return Collections.singletonList(new JsonObject("{" +
                "\"_id\": {\"register_id\": 444, \"audience_id\":\"333\", \"start_at\":\"2022-03-08T08:55:00\", \"end_at\":\"2022-03-08T09:50:00\"}," +
                "\"structure_id\": \"111\", \"slot_id\":\"2222\", \"student_count\":29}"));
    }

    private List<JsonObject> expectedMapSplittableRegisterToWeeklyAudience() {
        return Arrays.asList(new JsonObject("{" +
                "\"_id\": {\"register_id\": 444, \"audience_id\":\"333\", \"start_at\":\"2022-03-08T08:55:00\", \"end_at\":\"2022-03-08T09:50:00\"}," +
                "\"structure_id\": \"111\", \"slot_id\":\"2222\", \"student_count\":29}"),
                new JsonObject("{" +
                        "\"_id\": {\"register_id\": 444, \"audience_id\":\"333\", \"start_at\":\"2022-03-08T10:05:00\", \"end_at\":\"2022-03-08T11:00:00\"}," +
                        "\"structure_id\": \"111\", \"slot_id\":\"2223\", \"student_count\":29}"));
    }
}
