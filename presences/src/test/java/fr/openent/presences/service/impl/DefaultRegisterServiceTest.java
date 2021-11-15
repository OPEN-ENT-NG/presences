package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.db.DB;
import fr.openent.presences.db.DBService;
import fr.openent.presences.service.RegisterService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

@RunWith(VertxUnitRunner.class)
public class DefaultRegisterServiceTest extends DBService {

    Sql sql = Mockito.mock(Sql.class);

    private RegisterService registerService;

    @Before
    public void setUp() {
        DB.getInstance().init(null, sql, null);
        this.registerService = new DefaultRegisterService(Vertx.vertx().eventBus());
    }

    @Test
    public void testFetchingRegister_Should_Return_Correct_Query(TestContext ctx) {
        Integer registerId = 350;

        String query = "SELECT personnel_id, proof_id, course_id, owner, notified, subject_id, start_date, end_date, " +
                "structure_id, counsellor_input, state_id FROM " + Presences.dbSchema + ".register " +
                "WHERE register.id = ?";

        JsonArray params = new JsonArray()
                .add(registerId);

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);

            ctx.assertEquals(queryResult, query);
            ctx.assertEquals(paramsResult, params);

            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        registerService.fetchRegister(registerId);
    }
}