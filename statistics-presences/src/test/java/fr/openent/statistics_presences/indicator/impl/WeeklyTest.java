package fr.openent.statistics_presences.indicator.impl;

import fr.openent.presences.db.DB;
import fr.openent.presences.db.DBService;
import fr.openent.statistics_presences.bean.weekly.WeeklySearch;
import fr.openent.statistics_presences.model.StatisticsFilter;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@RunWith(VertxUnitRunner.class)
public class WeeklyTest extends DBService {

    MongoDb mongoDb = Mockito.mock(MongoDb.class);
    private Weekly weekly;
    private WeeklySearch search;

    private static final String STRUCTURE_ID = "111";
    private static final List<String> AUDIENCE_IDS = Collections.singletonList("222");
    private static final String STUDENT_ID = "333";
    private static final List<Integer> REASON_IDS = Arrays.asList(12, 13);
    private static final String START = "2021-06-01 08:00:00";
    private static final String END = "2021-06-30 23:59:59";
    private static final String NO_REASON = "NO_REASON";
    private static final String UNREGULARIZED = "UNREGULARIZED";


    @Before
    public void setUp(TestContext context) {
        DB.getInstance().init(null, null, mongoDb);

        /* Indicator to test */
        this.weekly = new Weekly(Vertx.vertx(), "Weekly");
    }

    @Test
    public void testStudentsBySlotsPipeline(TestContext ctx) throws Exception {
        JsonObject filter = new JsonObject()
                .put(StatisticsFilter.StatisticsFilterField.START, START)
                .put(StatisticsFilter.StatisticsFilterField.END, END)
                .put(StatisticsFilter.StatisticsFilterField.AUDIENCES, AUDIENCE_IDS)
                .put(StatisticsFilter.StatisticsFilterField.TYPES, Arrays.asList(NO_REASON, UNREGULARIZED))
                .put(StatisticsFilter.StatisticsFilterField.REASONS, REASON_IDS)
                .put(StatisticsFilter.StatisticsFilterField.PUNISHMENT_TYPES, Collections.emptyList());

        search = new WeeklySearch(new StatisticsFilter(STRUCTURE_ID, filter));

        String expected = "[{\"$match\":{\"structure\":\"111\",\"start_date\":{\"$lt\":\"2021-06-30 23:59:59\"}," +
                "\"end_date\":{\"$gt\":\"2021-06-01 08:00:00\"},\"audiences\":{\"$in\":[\"222\"]}," +
                "\"$or\":[{\"type\":\"NO_REASON\"},{\"type\":\"UNREGULARIZED\"," +
                "\"reason\":{\"$in\":[12,13]}}]}}," +
                "{\"$addFields\":{\"dayOfWeek\":{\"$isoDayOfWeek\":{\"$dateFromString\":{\"dateString\":\"$start_date\"}}}}}," +
                "{\"$unwind\":\"$slots\"}," +
                "{\"$group\":{\"_id\":{\"slot\":\"$slots\",\"dayOfWeek\":\"$dayOfWeek\"},\"count\":{\"$sum\":1}}}," +
                "{\"$project\":{\"_id\":0,\"slot_id\":\"$_id.slot.id\",\"dayOfWeek\":\"$_id.dayOfWeek\",\"count\":{\"$sum\":\"$count\"}}}]";

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            ctx.assertEquals(new JsonObject(query).getJsonArray("pipeline", new JsonArray()).toString(), expected);
            return null;
        }).when(mongoDb).command(Mockito.anyString(), Mockito.any(Handler.class));

        this.weekly.retrieveStatistics(search.countEventTypedBySlotsCommand());

    }

    @Test
    public void testCountEventTypedBySlotsPipelineFromStudent(TestContext ctx) throws Exception {
        JsonObject filter = new JsonObject()
                .put(StatisticsFilter.StatisticsFilterField.START, START)
                .put(StatisticsFilter.StatisticsFilterField.END, END)
                .put(StatisticsFilter.StatisticsFilterField.AUDIENCES, AUDIENCE_IDS)
                .put(StatisticsFilter.StatisticsFilterField.TYPES, Arrays.asList(NO_REASON, UNREGULARIZED))
                .put(StatisticsFilter.StatisticsFilterField.REASONS, REASON_IDS)
                .put(StatisticsFilter.StatisticsFilterField.PUNISHMENT_TYPES, Collections.emptyList());

        search = new WeeklySearch(new StatisticsFilter(STRUCTURE_ID, filter));
        search.filter().setUserId(STUDENT_ID);

        JsonArray expected = expectedCountEventTypedBySlotsPipelineFromStudent().getJsonArray("pipeline");

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            ctx.assertEquals(new JsonObject(query).getJsonArray("pipeline", new JsonArray()), expected);
            return null;
        }).when(mongoDb).command(Mockito.anyString(), Mockito.any(Handler.class));

        Whitebox.invokeMethod(weekly, "retrieveStatistics", search.countStudentsBySlotsCommand());
    }

    @Test
    public void testCountEventTypedBySlotsPipelineFromAudience(TestContext ctx) throws Exception {
        JsonObject filter = new JsonObject()
                .put(StatisticsFilter.StatisticsFilterField.START, START)
                .put(StatisticsFilter.StatisticsFilterField.END, END)
                .put(StatisticsFilter.StatisticsFilterField.AUDIENCES, AUDIENCE_IDS)
                .put(StatisticsFilter.StatisticsFilterField.TYPES, Arrays.asList(NO_REASON, UNREGULARIZED))
                .put(StatisticsFilter.StatisticsFilterField.REASONS, REASON_IDS)
                .put(StatisticsFilter.StatisticsFilterField.PUNISHMENT_TYPES, Collections.emptyList());

        search = new WeeklySearch(new StatisticsFilter(STRUCTURE_ID, filter));

        JsonArray expected = expectedCountEventTypedBySlotsPipelineFromAudience().getJsonArray("pipeline");

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            ctx.assertEquals(new JsonObject(query).getJsonArray("pipeline", new JsonArray()), expected);
            return null;
        }).when(mongoDb).command(Mockito.anyString(), Mockito.any(Handler.class));

        Whitebox.invokeMethod(weekly, "retrieveStatistics", search.countStudentsBySlotsCommand());
    }

    @Test
    public void testRates(TestContext ctx) throws Exception {
        double testClassicRates = Whitebox.invokeMethod(weekly, "getEventRates", 3, 1);
        ctx.assertEquals(testClassicRates, 33.33);

        double testNoMoreThanUndredRates = Whitebox.invokeMethod(weekly, "getEventRates", 1, 3);
        ctx.assertEquals(testNoMoreThanUndredRates, 100.0);

        double testAbsolute = Whitebox.invokeMethod(weekly, "getEventRates", 4, -1);
        ctx.assertEquals(testAbsolute, 25.0);

        double testNullParam = Whitebox.invokeMethod(weekly, "getEventRates", 4, null);
        ctx.assertEquals(testNullParam, 0.0);

        double testNaN = Whitebox.invokeMethod(weekly, "getEventRates", 0, 1);
        ctx.assertEquals(testNaN, 0.0);

    }

    private JsonObject expectedCountEventTypedBySlotsPipelineFromStudent() {
        return new JsonObject("{" +
                "\"pipeline\": [{\"$match\": {" +
                "\"structure_id\": \"111\"," +
                "\"_id.start_at\": {\"$lt\": \"2021-06-30 23:59:59\"}," +
                "\"_id.end_at\": {\"$gt\": \"2021-06-01 08:00:00\"}," +
                "\"_id.audience_id\": {\"$in\": [\"222\"]}" +
                "}}, {\"$addFields\": {\"dayOfWeek\": {\"$isoDayOfWeek\": {\"$dateFromString\": {\"dateString\": \"$_id.start_at\"}}}" +
                "}}, {\"$group\": {\"_id\": {\"slot_id\": \"$slot_id\",\"dayOfWeek\": \"$dayOfWeek\"},\"count\": {\"$sum\": 1}" +
                "}}, {\"$project\": { \"_id\": 0,\"slot_id\": \"$_id.slot_id\",\"dayOfWeek\": \"$_id.dayOfWeek\",\"count\": {\"$sum\": \"$count\"}" +
                "}}]" +
                "}");
    }

    private JsonObject expectedCountEventTypedBySlotsPipelineFromAudience() {
        return new JsonObject("{" +
                "\"pipeline\": [{\"$match\": {" +
                "\"structure_id\": \"111\"," +
                "\"_id.start_at\": {\"$lt\": \"2021-06-30 23:59:59\"}," +
                "\"_id.end_at\": {\"$gt\": \"2021-06-01 08:00:00\"}," +
                "\"_id.audience_id\": {\"$in\": [\"222\"]}" +
                "}}, {\"$addFields\": {\"dayOfWeek\": {\"$isoDayOfWeek\": {\"$dateFromString\": {\"dateString\": \"$_id.start_at\"}}}" +
                "}}, {\"$group\": {\"_id\": {\"slot_id\": \"$slot_id\",\"dayOfWeek\": \"$dayOfWeek\"},\"count\": {\"$sum\": \"$student_count\"}" +
                "}}, {\"$project\": { \"_id\": 0,\"slot_id\": \"$_id.slot_id\",\"dayOfWeek\": \"$_id.dayOfWeek\",\"count\": {\"$sum\": \"$count\"}" +
                "}}]" +
                "}");
    }
}
