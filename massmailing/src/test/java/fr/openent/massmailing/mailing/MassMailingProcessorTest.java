package fr.openent.massmailing.mailing;

import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.MassmailingType;
import fr.openent.presences.core.constants.Field;
import io.vertx.core.json.JsonArray;
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
public class MassMailingProcessorTest {
    private Template template;
    private static final String STRUCTURE_ID = "111";
    private static final String START_AT = "2022-01-01";
    private static final String END_AT = "2022-05-01";
    private static final List<MassmailingType> MASSMAILING_TYPES = Arrays.asList(MassmailingType.PUNISHMENT, MassmailingType.SANCTION);


    @Before
    public void setUp() {
        this.template = new Template(MailingType.MAIL, 1, STRUCTURE_ID, new JsonObject());
    }

    @Test
    public void testDuplicatePunishments(TestContext ctx) throws Exception {
        MassMailingProcessor mailing = new Mail(STRUCTURE_ID, template, null, MASSMAILING_TYPES, null,
                null, null, START_AT, END_AT, null, true, null);

        JsonObject massmail = new JsonObject()
                .put("receiver_data", "test")
                .put(Field.EVENTS,
                        new JsonObject().put(MassmailingType.PUNISHMENT.name(), new JsonArray().add(
                                new JsonObject().put(Field.PUNISHMENTS, new JsonArray()
                                        .add(simplePunishment())
                                        .add(groupedPunishment1())
                                        .add(groupedPunishment2())
                                )))
                );

        List<JsonObject> result = Whitebox.invokeMethod(mailing, "formatFromPunishments",
                massmail);

        List<JsonObject> expected = Arrays.asList(
                new JsonObject("{\"receiver_data\":\"test\",\"events\":" +
                        "{\"PUNISHMENT\":{\"punishment\":{\"grouped_punishment_id\":null," +
                        "\"fields\":{\"delay_at\":\"2022-02-01 00:00:00\"}}}}}"),
                new JsonObject("{\"receiver_data\":\"test\",\"events\":" +
                        "{\"PUNISHMENT\":{\"punishment\":{\"grouped_punishment_id\":\"group1\"," +
                        "\"fields\":{\"end_at\":\"2022-02-02 14:55:00\",\"start_at\":\"2022-02-02 14:00:00\"}," +
                        "\"slots\":[{\"end_at\":\"2022-02-02 14:55:00\",\"start_at\":\"2022-02-02 14:00:00\"}," +
                        "{\"end_at\":\"2022-02-04 17:55:00\",\"start_at\":\"2022-02-04 17:00:00\"}]},\"punishments\":" +
                        "[{\"grouped_punishment_id\":\"group1\"," +
                        "\"fields\":{\"end_at\":\"2022-02-02 14:55:00\",\"start_at\":\"2022-02-02 14:00:00\"},\"slots\":" +
                        "[{\"end_at\":\"2022-02-02 14:55:00\",\"start_at\":\"2022-02-02 14:00:00\"}," +
                        "{\"end_at\":\"2022-02-04 17:55:00\",\"start_at\":\"2022-02-04 17:00:00\"}]},{" +
                        "\"grouped_punishment_id\":null,\"fields\":{\"delay_at\":\"2022-02-01 00:00:00\"}}]}," +
                        "\"SANCTION\":{\"punishment\":{},\"punishments\":[]}}}")
        );

        ctx.assertEquals(result, expected);
    }

    @Test
    public void testNotDuplicatePunishments(TestContext ctx) throws Exception {
        MassMailingProcessor mailing = new Mail(STRUCTURE_ID, template, null, MASSMAILING_TYPES, null,
                null, null, START_AT, END_AT, null, false, null);

        JsonObject massmail = new JsonObject()
                .put("receiver_data", "test")
                .put(Field.EVENTS,
                        new JsonObject().put(MassmailingType.PUNISHMENT.name(), new JsonArray().add(
                                new JsonObject().put(Field.PUNISHMENTS, new JsonArray()
                                        .add(simplePunishment())
                                        .add(groupedPunishment1())
                                        .add(groupedPunishment2())
                                )))
                );

        List<JsonObject> result = Whitebox.invokeMethod(mailing, "formatFromPunishments",
                massmail);

        List<JsonObject> expected = Collections.singletonList(
                new JsonObject("{\"receiver_data\":\"test\",\"events\":" +
                        "{\"PUNISHMENT\":{\"punishment\":{\"grouped_punishment_id\":\"group1\"," +
                        "\"fields\":{\"end_at\":\"2022-02-02 14:55:00\",\"start_at\":\"2022-02-02 14:00:00\"}," +
                        "\"slots\":[{\"end_at\":\"2022-02-02 14:55:00\",\"start_at\":\"2022-02-02 14:00:00\"}," +
                        "{\"end_at\":\"2022-02-04 17:55:00\",\"start_at\":\"2022-02-04 17:00:00\"}]},\"punishments\":" +
                        "[{\"grouped_punishment_id\":\"group1\"," +
                        "\"fields\":{\"end_at\":\"2022-02-02 14:55:00\",\"start_at\":\"2022-02-02 14:00:00\"},\"slots\":" +
                        "[{\"end_at\":\"2022-02-02 14:55:00\",\"start_at\":\"2022-02-02 14:00:00\"}," +
                        "{\"end_at\":\"2022-02-04 17:55:00\",\"start_at\":\"2022-02-04 17:00:00\"}]},{" +
                        "\"grouped_punishment_id\":null,\"fields\":{\"delay_at\":\"2022-02-01 00:00:00\"}}]}," +
                        "\"SANCTION\":{\"punishment\":{},\"punishments\":[]}}}")
        );

        ctx.assertEquals(result, expected);
    }

    @Test
    public void testNotDuplicateIfOnlyOneElement(TestContext ctx) throws Exception {
        MassMailingProcessor mailing = new Mail(STRUCTURE_ID, template, null, MASSMAILING_TYPES, null,
                null, null, START_AT, END_AT, null, true, null);

        JsonObject massmail = new JsonObject()
                .put("receiver_data", "test")
                .put(Field.EVENTS,
                        new JsonObject().put(MassmailingType.PUNISHMENT.name(), new JsonArray().add(
                                new JsonObject().put(Field.PUNISHMENTS, new JsonArray()
                                        .add(simplePunishment())
                                )))
                                .put(MassmailingType.SANCTION.name(), new JsonArray().add(
                                new JsonObject().put(Field.PUNISHMENTS, new JsonArray()
                                        .add(groupedPunishment1())
                                        .add(groupedPunishment2())
                                )))
                );

        List<JsonObject> result = Whitebox.invokeMethod(mailing, "formatFromPunishments",
                massmail);

        List<JsonObject> expected = Collections.singletonList(
                new JsonObject("{\"receiver_data\": \"test\",\"events\": {\"PUNISHMENT\": {" +
                        "\"punishment\": {\"grouped_punishment_id\": null,\"fields\": {\"delay_at\": \"2022-02-01 00:00:00\"}}," +
                        "\"punishments\": [{\"grouped_punishment_id\": null,\"fields\": {\"delay_at\": \"2022-02-01 00:00:00\"}}]}," +
                        "\"SANCTION\": {\"punishment\": {\"grouped_punishment_id\": \"group1\"," +
                        "\"fields\": {\"end_at\": \"2022-02-02 14:55:00\",\"start_at\": \"2022-02-02 14:00:00\"}," +
                        "\"slots\": [{\"end_at\": \"2022-02-02 14:55:00\",\"start_at\": \"2022-02-02 14:00:00\"}," +
                        "{\"end_at\": \"2022-02-04 17:55:00\",\"start_at\": \"2022-02-04 17:00:00\"}]}," +
                        "\"punishments\": [{\"grouped_punishment_id\": \"group1\"," +
                        "\"fields\": {\"end_at\": \"2022-02-02 14:55:00\",\"start_at\": \"2022-02-02 14:00:00\"}," +
                        "\"slots\": [{\"end_at\": \"2022-02-02 14:55:00\",\"start_at\": \"2022-02-02 14:00:00\"}," +
                        "{\"end_at\": \"2022-02-04 17:55:00\",\"start_at\": \"2022-02-04 17:00:00\"}]}]}}}")
        );

        ctx.assertEquals(result, expected);
    }


    private JsonObject simplePunishment() {
        return new JsonObject()
                .putNull(Field.GROUPED_PUNISHMENT_ID)
                .put(Field.FIELDS, new JsonObject().put("delay_at", "2022-02-01 00:00:00"));
    }

    private JsonObject groupedPunishment1() {
        return new JsonObject()
                .put(Field.GROUPED_PUNISHMENT_ID, "group1")
                .put(Field.FIELDS,
                        new JsonObject().put(Field.START_AT, "2022-02-02 14:00:00").put(Field.END_AT, "2022-02-02 14:55:00"));
    }

    private JsonObject groupedPunishment2() {
        return new JsonObject()
                .put(Field.GROUPED_PUNISHMENT_ID, "group1")
                .put(Field.FIELDS,
                        new JsonObject().put(Field.START_AT, "2022-02-04 17:00:00").put(Field.END_AT, "2022-02-04 17:55:00"));
    }

}
