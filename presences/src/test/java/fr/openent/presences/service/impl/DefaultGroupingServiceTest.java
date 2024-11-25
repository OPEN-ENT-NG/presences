package fr.openent.presences.service.impl;

import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.db.DBService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import io.vertx.core.Future;

import java.util.Arrays;


@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({Viescolaire.class}) //Prepare the static class you want to mock
public class DefaultGroupingServiceTest extends DBService {
    private DefaultGroupingService groupingService;

    @Before
    public void setUp() {
        this.groupingService = Mockito.spy(new DefaultGroupingService());
    }

    @Test
    public void testGetGroupingStructure(TestContext ctx) {
        String structureId = "structureId";
        Mockito.doReturn(null).when(this.groupingService).searchGrouping(structureId, null);

        this.groupingService.getGroupingStructure(structureId);
        Mockito.verify(this.groupingService, Mockito.times(1)).searchGrouping(structureId, null);
    }

    @Test
    public void testSearchGrouping(TestContext ctx) {
        Async async = ctx.async();
        Viescolaire viescolaire = Mockito.spy(Viescolaire.getInstance());
        PowerMockito.spy(Viescolaire.class);
        PowerMockito.when(Viescolaire.getInstance()).thenReturn(viescolaire);

        String structureId = "structureId";
        JsonObject grouping1 = new JsonObject("{\"id\":\"3c49e1ce-5754-4297-a6cd-2dbf26dd5d34\",\"name\":\"Grouping3eme\"," +
                "\"structure_id\":\"46094e4c-a86f-4b73-812e-890e791a6900\",\"student_divisions\":[{\"id\":" +
                "\"c4e6e906-fd55-4d7d-ab04-49215d3b4fad\",\"name\":\"3EME1_LCALA\"}]}");
        JsonObject grouping2 = new JsonObject("{\"id\":\"3c49e1ce-5754-4297-a6cd-2dbf26dd5d34\",\"name\":\"grouping\",\"structure_id\":" +
                "\"46094e4c-a86f-4b73-812e-890e791a6900\",\"student_divisions\":[{\"id\":\"c4e6e906-fd55-4d7d-ab04-49215d3b4fad\"," +
                "\"name\":\"3EME1_LCALA\"}]}");
        JsonObject grouping3 = new JsonObject("{\"id\":\"3c49e1ce-5754-4297-a6cd-2dbf26dd5d34\",\"name\":\"groupi\",\"structure_id\":" +
                "\"46094e4c-a86f-4b73-812e-890e791a6900\",\"student_divisions\":[{\"id\":\"c4e6e906-fd55-4d7d-ab04-49215d3b4fad\"," +
                "\"name\":\"3EME1_LCALA\"}]}");

        Future<JsonArray> groupingListFuture = Future.succeededFuture(new JsonArray(Arrays.asList(grouping1, grouping2, grouping3)));
        PowerMockito.doReturn(groupingListFuture).when(viescolaire).getGroupingStructure("structureId", "grouping");
        this.groupingService.searchGrouping(structureId, "grouping")
                .onSuccess(groupingList -> {
                    ctx.assertEquals(groupingList.size(), 2);
                    ctx.assertEquals(groupingList.get(0).toJson().toString(), grouping1.toString());
                    ctx.assertEquals(groupingList.get(1).toJson().toString(), grouping2.toString());
                    async.complete();
                });

        async.awaitSuccess(10000);
    }
}