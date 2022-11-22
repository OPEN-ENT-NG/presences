package fr.openent.statistics_presences.indicator.impl;

import fr.openent.presences.db.DB;
import fr.openent.presences.db.DBService;
import fr.openent.statistics_presences.bean.monthly.MonthlySearch;
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
public class MonthlyTest extends DBService {

    MongoDb mongoDb = Mockito.mock(MongoDb.class);
    private Monthly monthly;
    private MonthlySearch search;

    private static final String STRUCTURE_ID = "111";
    private static final String START = "2021-06-01 08:00:00";
    private static final String END = "2021-06-30 23:59:59";
    private static final String LATENESS = "LATENESS";
    private static final String PUNISHMENT = "PUNISHMENT";
    private static final List<Integer> PUNISHMENT_TYPE_IDS = Arrays.asList(1, 2);
    private static final List<Integer> LATENESS_TYPE_IDS = Arrays.asList(1, 2, null);
    private static final String NO_REASON = "NO_REASON";
    private static final List<String> BASIC_TYPES = Arrays.asList(LATENESS, PUNISHMENT);
    private static final String HALFDAY = "12:30:00";
    private static final String RECOVERY = "HALF_DAY";


    @Before
    public void setUp(TestContext context) {
        DB.getInstance().init(null, null, mongoDb);

        /* Indicator to test */
        this.monthly = new Monthly(Vertx.vertx(), "Monthly");
    }

    @Test
    public void testBasicTypedEventByAudiencesMonthlyPipeline(TestContext ctx) {
        JsonObject filter = new JsonObject()
                .put(StatisticsFilter.StatisticsFilterField.START, START)
                .put(StatisticsFilter.StatisticsFilterField.END, END)
                .put(StatisticsFilter.StatisticsFilterField.TYPES, BASIC_TYPES)
                .put(StatisticsFilter.StatisticsFilterField.NOLATENESSREASON, true)
                .put(StatisticsFilter.StatisticsFilterField.REASONS, new JsonArray(Arrays.asList(1, 2)))
                .put(StatisticsFilter.StatisticsFilterField.PUNISHMENT_TYPES, PUNISHMENT_TYPE_IDS);

        search = new MonthlySearch(new StatisticsFilter(STRUCTURE_ID, filter));

        JsonObject expected = expectedBasicTypedEventMonthlyPipeline();

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            ctx.assertEquals(new JsonObject(query), expected);
            return null;
        }).when(mongoDb).command(Mockito.anyString(), Mockito.any(Handler.class));

        try {
            Whitebox.invokeMethod(monthly, "retrieveStatistics", search.searchBasicEventTypedByAudiencePipeline());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAbsencesByAudiencesMonthlyPipeline(TestContext ctx) throws Exception {
        JsonObject filter = new JsonObject()
                .put(StatisticsFilter.StatisticsFilterField.START, START)
                .put(StatisticsFilter.StatisticsFilterField.END, END)
                .put(StatisticsFilter.StatisticsFilterField.TYPES, Collections.singletonList(NO_REASON));

        search = new MonthlySearch(new StatisticsFilter(STRUCTURE_ID, filter));
        search.setHalfDay(HALFDAY);
        search.setRecoveryMethod(RECOVERY);

        JsonObject expected = expectedAbsencesMonthlyPipeline();

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            ctx.assertEquals(new JsonObject(query), expected);
            return null;
        }).when(mongoDb).command(Mockito.anyString(), Mockito.any(Handler.class));

        Whitebox.invokeMethod(monthly, "retrieveStatistics", search.searchAbsencesByAudiencePipeline());
    }

    private JsonObject expectedBasicTypedEventMonthlyPipeline() {
        return new JsonObject()
                .put("aggregate", "presences.statistics")
                .put("allowDiskUse", true)
                .put("cursor",
                        new JsonObject().put("batchSize", 2147483647)
                )
                .put("pipeline",
                        new JsonArray(
                                Arrays.asList(
                                        new JsonObject()
                                                .put("$addFields", new JsonObject()
                                                        .put("start_at", new JsonObject()
                                                                .put("$dateFromString", new JsonObject()
                                                                        .put("dateString", "$start_date")))),
                                        new JsonObject()
                                                .put("$match", new JsonObject()
                                                        .put("structure", STRUCTURE_ID)
                                                        .put("$or", new JsonArray()
                                                                .add(new JsonObject()
                                                                        .put("type", LATENESS)
                                                                        .put("reason", new JsonObject()
                                                                                .put("$in", LATENESS_TYPE_IDS)))
                                                                .add(new JsonObject()
                                                                        .put("type", PUNISHMENT)
                                                                        .put("punishment_type", new JsonObject()
                                                                                .put("$in", PUNISHMENT_TYPE_IDS))))
                                                        .put("start_date", new JsonObject().put("$gte", START))
                                                        .put("end_date", new JsonObject().put("$lte", END))),
                                        new JsonObject()
                                                .put("$addFields", new JsonObject()
                                                        .put("countId", new JsonObject()
                                                                .put("$cond", new JsonArray()
                                                                        .add(new JsonObject().put("$gte", new
                                                                                JsonArray()
                                                                                .add("$grouped_punishment_id")
                                                                                .addNull()
                                                                        ))
                                                                        .add("$grouped_punishment_id")
                                                                        .add("$_id")
                                                                ))),
                                        new JsonObject()
                                                .put("$group", new JsonObject()
                                                        .put("_id", new JsonObject()
                                                                .put("class_name", "$class_name")
                                                                .put("month", new JsonObject().put("$month", "$start_at"))
                                                                .put("year", new JsonObject().put("$year", "$start_at"))
                                                                .put("countId", "$countId"))
                                                        .put("slots", new JsonObject().put("$sum", "$slots"))
                                                        .put("start_at", new JsonObject().put("$first", "$start_at"))),
                                        new JsonObject()
                                                .put("$group", new JsonObject()
                                                        .put("_id", new JsonObject()
                                                                .put("class_name", "$_id.class_name")
                                                                .put("month", "$_id.month")
                                                                .put("year", "$_id.year"))
                                                        .put("count", new JsonObject().put("$sum", 1))
                                                        .put("start_at", new JsonObject().put("$first", "$start_at"))
                                                        .put("slots", new JsonObject().put("$sum", 1))),
                                        new JsonObject()
                                                .put("$project", new JsonObject()
                                                        .put("_id", 0)
                                                        .put("month", new JsonObject().put("$dateToString", new JsonObject()
                                                                .put("format", "%Y-%m")
                                                                .put("date", "$start_at")))
                                                        .put("class_name", "$_id.class_name")
                                                        .put("count", new JsonObject().put("$sum", "$count"))
                                                        .put("slots", new JsonObject().put("$sum", "$slots")))
                                )
                        )
                );
    }

    private JsonObject expectedAbsencesMonthlyPipeline() {
        return new JsonObject()
                .put("aggregate", "presences.statistics")
                .put("allowDiskUse", true)
                .put("cursor",
                        new JsonObject().put("batchSize", 2147483647)
                )
                .put("pipeline",
                        new JsonArray(
                                Arrays.asList(
                                        new JsonObject()
                                                .put("$addFields", new JsonObject()
                                                        .put("start_at", new JsonObject()
                                                                .put("$dateFromString", new JsonObject()
                                                                        .put("dateString", "$start_date")))),
                                        new JsonObject()
                                                .put("$match", new JsonObject()
                                                        .put("structure", STRUCTURE_ID)
                                                        .put("$or", new JsonArray().add(new JsonObject().put("type", NO_REASON)))
                                                        .put("start_date", new JsonObject().put("$gte", START))
                                                        .put("end_date", new JsonObject().put("$lte", END))),
                                        new JsonObject()
                                                .put("$group", new JsonObject()
                                                        .put("_id", new JsonObject()
                                                                .put("name", "$name")
                                                                .put("class_name", "$class_name")
                                                                .put("day", new JsonObject().put("$dayOfMonth", "$start_at"))
                                                                .put("month", new JsonObject().put("$month", "$start_at"))
                                                                .put("year", new JsonObject().put("$year", "$start_at"))
                                                                .put("is_before_halfday", new JsonObject().put("$lt", new JsonArray(
                                                                        Arrays.asList(
                                                                                new JsonObject()
                                                                                        .put("$dateToString", new JsonObject()
                                                                                                .put("format", "%H:%M:%S")
                                                                                                .put("date", "$start_at")
                                                                                        ),
                                                                                HALFDAY
                                                                        )))))
                                                        .put("start_at", new JsonObject().put("$first", "$start_at"))
                                                        .put("slots", new JsonObject().put("$sum", 1))),
                                        new JsonObject()
                                                .put("$group", new JsonObject()
                                                        .put("_id", new JsonObject()
                                                                .put("month", "$_id.month")
                                                                .put("year", "$_id.year")
                                                                .put("class_name", "$_id.class_name"))
                                                        .put("start_at", new JsonObject().put("$first", "$start_at"))
                                                        .put("count", new JsonObject().put("$sum", 1))
                                                        .put("slots", new JsonObject().put("$sum", "$slots"))),
                                        new JsonObject()
                                                .put("$project", new JsonObject()
                                                        .put("_id", 0)
                                                        .put("month", new JsonObject().put("$dateToString", new JsonObject()
                                                                .put("format", "%Y-%m")
                                                                .put("date", "$start_at")))
                                                        .put("class_name", "$_id.class_name")
                                                        .put("count", new JsonObject().put("$sum", "$count"))
                                                        .put("slots", new JsonObject().put("$sum", "$slots")))
                                )
                        )
                );
    }
}
