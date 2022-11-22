package fr.openent.statistics_presences.service.impl;


import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DBService;
import fr.openent.presences.model.StatisticsUser;
import fr.openent.presences.model.StructureStatisticsUser;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.indicator.ComputeStatistics;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import fr.openent.statistics_presences.service.StatisticsPresencesService;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.junit.Before;
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
import java.util.stream.Collectors;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({Viescolaire.class, Sql.class}) //Prepare the static class you want to mock
public class DefaultStatisticsPresencesServiceTest {
    private Vertx vertx;
    private Sql sql;
    private Viescolaire viescolaire;
    private DefaultStatisticsPresencesService defaultStatisticsPresencesService;

    @Before
    public void before() {
        vertx = Vertx.vertx();
        PowerMockito.spy(Sql.class);
        sql = PowerMockito.spy(Sql.getInstance());
        PowerMockito.when(Sql.getInstance()).thenReturn(sql);
        PowerMockito.spy(Viescolaire.class);
        viescolaire = PowerMockito.spy(Viescolaire.getInstance());
        PowerMockito.when(Viescolaire.getInstance()).thenReturn(viescolaire);
        this.defaultStatisticsPresencesService = PowerMockito.spy(new DefaultStatisticsPresencesService(new CommonServiceFactory(vertx)));
        Whitebox.setInternalState(this.defaultStatisticsPresencesService, "sql", this.sql);
    }

    @Test
    public void testCreate(TestContext ctx) throws Exception {
        Async async = ctx.async();
        List<String> studentList = Arrays.asList("studentId1", "studentId2");

        PowerMockito.doReturn(Future.succeededFuture(new JsonObject().put(Field.START_DATE, "startDate"))).when(viescolaire).getSchoolYear("structure");

        JsonArray expected = new JsonArray("[" +
                "{\"statement\":\" INSERT INTO null.user(id, structure, modified)  VALUES (?, ?, ?)  " +
                "ON CONFLICT (id, structure) DO UPDATE SET modified = ?;\",\"values\":[\"studentId1\",\"structure\"," +
                "\"startDate\",\"startDate\"],\"action\":\"prepared\"}," +
                "{\"statement\":\" INSERT INTO null.user(id, structure, modified)  VALUES (?, ?, ?)  " +
                "ON CONFLICT (id, structure) DO UPDATE SET modified = ?;\",\"values\":[\"studentId2\",\"structure\"," +
                "\"startDate\",\"startDate\"],\"action\":\"prepared\"}" +
                "]");

        PowerMockito.doAnswer(invocation -> {
            JsonArray statements = invocation.getArgument(0);
            ctx.assertEquals(statements.toString(), expected.toString());
            async.complete();
            return null;
        }).when(sql, "transaction", Mockito.any(), Mockito.any());

        this.defaultStatisticsPresencesService.create("structure", studentList, null);
        async.awaitSuccess(10000);
    }

    @Test
    public void testCreateWithModifiedDate(TestContext ctx) throws Exception {
        Async async = ctx.async();
        List<StatisticsUser> studentIdModifiedDateMap = new ArrayList<>();
        studentIdModifiedDateMap.add(new StatisticsUser().setId("userId1").setStructureId("structure").setModified("modified1"));
        studentIdModifiedDateMap.add(new StatisticsUser().setId("userId2").setStructureId("structure").setModified("modified2"));

        PowerMockito.doReturn(Future.succeededFuture(new JsonObject().put(Field.START_DATE, "startDate"))).when(viescolaire).getSchoolYear("structure");

        JsonArray expected = new JsonArray("[" +
                "{\"statement\":\" INSERT INTO null.user(id, structure, modified)  VALUES (?, ?, ?)  " +
                "ON CONFLICT (id, structure) DO UPDATE SET modified = ?;\",\"values\":[\"userId1\",\"structure\"," +
                "\"modified1\",\"modified1\"],\"action\":\"prepared\"}," +
                "{\"statement\":\" INSERT INTO null.user(id, structure, modified)  VALUES (?, ?, ?)  " +
                "ON CONFLICT (id, structure) DO UPDATE SET modified = ?;\",\"values\":[\"userId2\",\"structure\"," +
                "\"modified2\",\"modified2\"],\"action\":\"prepared\"}" +
                "]");

        PowerMockito.doAnswer(invocation -> {
            JsonArray statements = invocation.getArgument(0);
            ctx.assertEquals(statements.toString(), expected.toString());
            async.complete();
            return null;
        }).when(sql, "transaction", Mockito.any(), Mockito.any());

        this.defaultStatisticsPresencesService.createWithModifiedDate("structure", studentIdModifiedDateMap, null);
        async.awaitSuccess(10000);
    }

    @Test
    public void testFetchUsers(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "SELECT structure as structure_id, json_object_agg(id, modified) as statistics_users FROM null.user GROUP BY structure_id;";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            ctx.assertEquals(query, expectedQuery);
            async.complete();
            return null;
        }).when(sql).raw(Mockito.anyString(), Mockito.any());

        this.defaultStatisticsPresencesService.fetchUsers();
    }

    @Test
    public void testClearWaitingList1(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "DELETE FROM null.user WHERE id IN (?,?) ;";
        String expectedParams = "[\"student1\",\"student2\"]";

        List<String> studentIdList = Arrays.asList("student1", "student2");

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(), Mockito.any());

        this.defaultStatisticsPresencesService.clearWaitingList(studentIdList);
    }

    @Test
    public void testClearWaitingList2(TestContext ctx) {
        Async async = ctx.async();
        String expectedQuery = "TRUNCATE TABLE null.user;";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            ctx.assertEquals(query, expectedQuery);
            async.complete();
            return null;
        }).when(sql).raw(Mockito.anyString(), Mockito.any());

        this.defaultStatisticsPresencesService.clearWaitingList();
    }
}
