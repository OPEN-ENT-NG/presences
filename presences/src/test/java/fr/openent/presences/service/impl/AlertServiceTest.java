package fr.openent.presences.service.impl;

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

@RunWith(VertxUnitRunner.class)
public class AlertServiceTest extends DBService {

    Sql sql = Mockito.mock(Sql.class);

    private AlertService alertService;

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
}