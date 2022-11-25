package fr.openent.statistics_presences.service.impl;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({DefaultStatisticsService.class, MongoDb.class}) //Prepare the static class you want to mock
public class DefaultStatisticsServiceTest {
    private DefaultStatisticsService defaultStatisticsService;
    private static MongoDb mongo;

    @Before
    public void before() {
        PowerMockito.spy(MongoDb.class);
        mongo = PowerMockito.spy(MongoDb.getInstance());
        PowerMockito.when(MongoDb.getInstance()).thenReturn(mongo);
        this.defaultStatisticsService = PowerMockito.spy(new DefaultStatisticsService("indicatorName"));
    }

    @Test
    public void saveTest(TestContext ctx) throws Exception {
        JsonArray students = new JsonArray(Arrays.asList("student1", "student2"));
        List<JsonObject> values = new ArrayList<>();

        PowerMockito.doReturn(Future.succeededFuture(values))
                .when(this.defaultStatisticsService, "deleteOldValues", Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.anyList(), Mockito.any(), Mockito.any());

        PowerMockito.doReturn(Future.succeededFuture())
                .when(this.defaultStatisticsService, "storeValues", Mockito.anyList());

        this.defaultStatisticsService.save("structure1", students, values, "startDate", "endDate", event -> {});
        this.defaultStatisticsService.save("structure2", students, values, event -> {});

        PowerMockito.verifyPrivate(this.defaultStatisticsService, Mockito.times(1))
                .invoke("deleteOldValues", Mockito.eq("structure1"), Mockito.any(JsonArray.class), Mockito.eq(values), Mockito.eq("startDate"), Mockito.eq("endDate"));
        PowerMockito.verifyPrivate(this.defaultStatisticsService, Mockito.times(1))
                .invoke("deleteOldValues", Mockito.eq("structure2"), Mockito.any(JsonArray.class), Mockito.eq(values), Mockito.isNull(), Mockito.isNull());
        PowerMockito.verifyPrivate(this.defaultStatisticsService, Mockito.never())
                .invoke("storeValues", Mockito.anyList());

        values.add(new JsonObject().put("data", "dataValue"));

        this.defaultStatisticsService.save("structure1", students, values, "startDate", "endDate", event -> {});
        this.defaultStatisticsService.save("structure2", students, values, event -> {});

        PowerMockito.verifyPrivate(this.defaultStatisticsService, Mockito.times(2))
                .invoke("deleteOldValues", Mockito.eq("structure1"), Mockito.any(JsonArray.class), Mockito.eq(values), Mockito.eq("startDate"), Mockito.eq("endDate"));
        PowerMockito.verifyPrivate(this.defaultStatisticsService, Mockito.times(2))
                .invoke("deleteOldValues", Mockito.eq("structure2"), Mockito.any(JsonArray.class), Mockito.eq(values), Mockito.isNull(), Mockito.isNull());
        PowerMockito.verifyPrivate(this.defaultStatisticsService, Mockito.times(2))
                .invoke("storeValues", values);
    }

    @Test
    public void overrideStatisticsStudentTest() throws Exception {
        List<JsonObject> values = new ArrayList<>();

        PowerMockito.doReturn(Future.succeededFuture(values))
                .when(this.defaultStatisticsService, "deleteOldValuesForStudent", Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.any(), Mockito.any());

        PowerMockito.doReturn(Future.succeededFuture())
                .when(this.defaultStatisticsService, "storeValues", Mockito.anyList());

        this.defaultStatisticsService.overrideStatisticsStudent("structureId", "studentId", values, "startDate", "endDate");
        PowerMockito.verifyPrivate(this.defaultStatisticsService, Mockito.times(1))
                .invoke("deleteOldValuesForStudent", Mockito.eq("structureId"), Mockito.eq("studentId"), Mockito.eq(values), Mockito.eq("startDate"), Mockito.eq("endDate"));
        PowerMockito.verifyPrivate(this.defaultStatisticsService, Mockito.never())
                .invoke("storeValues", Mockito.anyList());

        values.add(new JsonObject().put("data", "dataValue"));

        this.defaultStatisticsService.overrideStatisticsStudent("structureId", "studentId", values, "startDate", "endDate");

        PowerMockito.verifyPrivate(this.defaultStatisticsService, Mockito.times(2))
                .invoke("deleteOldValuesForStudent", Mockito.eq("structureId"), Mockito.eq("studentId"), Mockito.eq(values), Mockito.eq("startDate"), Mockito.eq("endDate"));
        PowerMockito.verifyPrivate(this.defaultStatisticsService, Mockito.times(1))
                .invoke("storeValues", Mockito.anyList());
    }

    @Test
    public void deleteOldValuesTest(TestContext ctx) throws Exception {
        Async async = ctx.async(2);
        String expectedCollection = "presences.statistics";
        String expectedMatcher1 = "{\"indicator\":\"indicatorName\",\"structure\":\"structureId1\",\"user\":{\"$in\":" +
                "[\"student1\",\"student2\"]},\"start_date\":{\"$gte\":\"startDate\"},\"end_date\":{\"$lte\":\"endDate\"}}";
        String expectedMatcher2 = "{\"indicator\":\"indicatorName\",\"structure\":\"structureId2\",\"user\":{\"$in\":[\"student1\",\"student2\"]}}";

        PowerMockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject matcher = invocation.getArgument(1);
            ctx.assertEquals(expectedCollection, collection);
            ctx.assertEquals(expectedMatcher1, matcher.toString());
            async.countDown();
            return null;
        }).when(mongo).delete(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any());

        List<JsonObject> values = new ArrayList<>();
        JsonArray students = new JsonArray(Arrays.asList("student1", "student2"));
        Whitebox.invokeMethod(this.defaultStatisticsService, "deleteOldValues", "structureId1", students, values, "startDate", "endDate");

        PowerMockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject matcher = invocation.getArgument(1);
            ctx.assertEquals(expectedCollection, collection);
            ctx.assertEquals(expectedMatcher2, matcher.toString());
            async.countDown();
            return null;
        }).when(mongo).delete(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any());
        Whitebox.invokeMethod(this.defaultStatisticsService, "deleteOldValues", "structureId2", students, values);

        async.awaitSuccess(10000);
    }

    @Test
    public void deleteOldValuesForStudentTest(TestContext ctx) throws Exception {
        Async async = ctx.async();
        String expectedCollection = "presences.statistics";
        String expectedMatcher = "{\"structure\":\"structureId\",\"user\":\"studentId\"," +
                "\"start_date\":{\"$gte\":\"startDate\"},\"end_date\":{\"$lte\":\"endDate\"}}";

        PowerMockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject matcher = invocation.getArgument(1);
            ctx.assertEquals(expectedCollection, collection);
            ctx.assertEquals(expectedMatcher, matcher.toString());
            async.complete();
            return null;
        }).when(mongo).delete(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any());

        List<JsonObject> values = new ArrayList<>();
        Whitebox.invokeMethod(this.defaultStatisticsService, "deleteOldValuesForStudent", "structureId", "studentId", values, "startDate", "endDate");

        async.awaitSuccess(10000);
    }

    @Test
    public void deleteStudentStatsTest(TestContext ctx) throws Exception {
        Async async = ctx.async();
        String expectedCollection = "presences.statistics";
        String expectedMatcher = "{\"structure\":\"structureId\",\"user\":\"studentId\"," +
                "\"start_date\":{\"$gte\":\"startDate\"},\"end_date\":{\"$lte\":\"endDate\"}}";

        PowerMockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject matcher = invocation.getArgument(1);
            ctx.assertEquals(expectedCollection, collection);
            ctx.assertEquals(expectedMatcher, matcher.toString());
            async.complete();
            return null;
        }).when(mongo).delete(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any());

        List<JsonObject> values = new ArrayList<>();
        this.defaultStatisticsService.deleteStudentStats("structureId", "studentId", "startDate", "endDate");

        async.awaitSuccess(10000);
    }

    @Test
    public void storeValuesTest(TestContext ctx) throws Exception {
        Async async = ctx.async();
        String expectedCollection = "presences.statistics";
        String expectedData = "[{\"data\":\"data1\"},{\"data\":\"data2\"}]";

        PowerMockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonArray data = invocation.getArgument(1);
            ctx.assertEquals(expectedCollection, collection);
            ctx.assertEquals(expectedData, data.toString());
            async.complete();
            return null;
        }).when(mongo).insert(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any());

        List<JsonObject> values = new ArrayList<>();
        values.add(new JsonObject().put("data", "data1"));
        values.add(new JsonObject().put("data", "data2"));
        Whitebox.invokeMethod(this.defaultStatisticsService, "storeValues", values);

        async.awaitSuccess(10000);
    }
}
