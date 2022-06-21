package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.db.*;
import fr.openent.presences.db.DB;
import fr.openent.presences.service.*;
import io.vertx.core.*;
import io.vertx.core.json.*;
import io.vertx.ext.unit.*;
import io.vertx.ext.unit.junit.*;
import org.entcore.common.sql.*;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.stubbing.*;
import org.powermock.reflect.*;

import java.util.Arrays;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class AlertServiceTest extends DBService {

    Sql sql = Mockito.mock(Sql.class);

    private AlertService alertService;

    private static final String STRUCTURE_ID = "111";
    private static final List<String> REASON_IDS = Arrays.asList("222", "333");
    private static final String START = "2022-06-01 08:00:00";
    private static final String END = "2022-06-30 23:59:59";

    @Before
    public void setUp() {
        DB.getInstance().init(null, sql, null);
        this.alertService = new DefaultAlertService();
    }

    @Test
    public void testResetStudentAlertsCount(TestContext ctx) throws Exception {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(params, new JsonArray()
                    .add(Field.STUDENT_ID)
                    .add(Field.STRUCTURE_ID)
                    .add(Field.TYPE));

            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        Whitebox.invokeMethod(alertService, "resetStudentAlertsCount",
                Field.STRUCTURE_ID, Field.STUDENT_ID, Field.TYPE);


    }

    @Test
    public void testDelete(TestContext ctx) throws Exception {
        String expectedQuery = String.format("DELETE FROM %s.alerts WHERE structure_id = ? AND id IN (?,?) AND created >= ? AND created <= ? ",
                Presences.dbSchema
        );

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(query, expectedQuery);

            ctx.assertEquals(params, new JsonArray()
                    .add(STRUCTURE_ID)
                    .addAll(new JsonArray(REASON_IDS))
                    .add(START)
                    .add(END));

            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        Whitebox.invokeMethod(alertService, "delete",
                STRUCTURE_ID, REASON_IDS, START, END);
    }

}