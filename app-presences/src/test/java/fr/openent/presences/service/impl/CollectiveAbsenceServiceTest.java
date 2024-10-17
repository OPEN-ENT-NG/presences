package fr.openent.presences.service.impl;

import fr.openent.presences.*;
import fr.openent.presences.db.*;
import fr.openent.presences.db.DB;
import fr.openent.presences.service.*;
import io.vertx.core.*;
import io.vertx.core.json.*;
import io.vertx.ext.unit.*;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.stubbing.*;
import org.powermock.reflect.*;

import java.util.*;

@RunWith(VertxUnitRunner.class)
public class CollectiveAbsenceServiceTest extends DBService {

    Sql sql = Mockito.mock(Sql.class);

    private CollectiveAbsenceService collectiveAbsenceService;

    private static final String STRUCTURE_ID = "structureId";
    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";
    private static final String AUDIENCE_ID = "audienceId";
    private static final long ABSENCE_ID = 42;
    private static final long COLLECTIVE_ID = 66;

    private static final List<String> AUDIENCE_IDS = new ArrayList<>(Collections.singletonList(AUDIENCE_ID));


    @Before
    public void setUp() {
        DB.getInstance().init(null, sql, null);
        this.collectiveAbsenceService = new DefaultCollectiveAbsenceService(null);
    }

    @Test
    public void testGetCollectives(TestContext ctx) throws Exception {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(params, new JsonArray()
                    .add(STRUCTURE_ID)
                    .add(END_DATE)
                    .add(START_DATE)
                    .add(0L)
                    .add(true)
                    .add(AUDIENCE_ID)
                    .add(Presences.PAGE_SIZE)
                    .add(0));
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        Whitebox.invokeMethod(collectiveAbsenceService, "getCollectives", STRUCTURE_ID,
                START_DATE, END_DATE, 0L, true, AUDIENCE_IDS, 0);
    }

    @Test
    public void testCountTotalPages(TestContext ctx) throws Exception {

        Mockito.doAnswer((Answer<Void>) invocation -> {

            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(params, new JsonArray()
                    .add(STRUCTURE_ID)
                    .add(END_DATE)
                    .add(START_DATE)
                    .add(0L)
                    .add(true)
                    .add(AUDIENCE_ID));
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        Whitebox.invokeMethod(collectiveAbsenceService, "countTotalPages", STRUCTURE_ID,
                START_DATE, END_DATE, 0L, true, AUDIENCE_IDS, null);
    }

    @Test
    public void testGetCollectiveFromAbsence(TestContext ctx) throws Exception {

        String PROPER_QUERY = " SELECT * FROM " + Presences.dbSchema +  ".collective_absence " +
                " WHERE id = ( " +
                "     SELECT collective_id " +
                "     FROM " + Presences.dbSchema + ".absence " +
                "     where id = ? " +
                " ) ";

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(PROPER_QUERY, query);
            ctx.assertEquals(params, new JsonArray()
                    .add(ABSENCE_ID));
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        Whitebox.invokeMethod(collectiveAbsenceService, "getCollectiveFromAbsence",
                ABSENCE_ID, null);
    }

    @Test
    public void testGetCollective(TestContext ctx) throws Exception {

        String PROPER_QUERY = "SELECT * FROM " + Presences.dbSchema + ".collective_absence " +
                " WHERE id = ? AND structure_id = ?";


        Mockito.doAnswer((Answer<Void>) invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(PROPER_QUERY, query);
            ctx.assertEquals(params, new JsonArray()
                    .add(COLLECTIVE_ID)
                    .add(STRUCTURE_ID));
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        Whitebox.invokeMethod(collectiveAbsenceService, "getCollective", STRUCTURE_ID,
                COLLECTIVE_ID, null);
    }
}
