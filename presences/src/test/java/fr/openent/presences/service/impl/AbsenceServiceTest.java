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

import java.util.*;

@RunWith(VertxUnitRunner.class)
public class AbsenceServiceTest extends DBService {

    Sql sql = Mockito.mock(Sql.class);

    private AbsenceService absenceService;

    @Before
    public void setUp() {
        DB.getInstance().init(null, sql, null);
        this.absenceService = new DefaultAbsenceService(Vertx.vertx().eventBus());
    }

    @Test
    public void testGetAbsences(TestContext ctx) throws Exception {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(params, new JsonArray()
                    .add(Field.STRUCTURE_ID)
                    .add(Field.START_DATE)
                    .add(Field.END_DATE)
                    .add(Field.ID));

            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        Whitebox.invokeMethod(absenceService, "get",
                Field.STRUCTURE_ID, Field.START_DATE, Field.END_DATE, Collections.singletonList(Field.ID));


    }
}