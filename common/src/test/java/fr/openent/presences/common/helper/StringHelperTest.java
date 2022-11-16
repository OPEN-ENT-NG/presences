package fr.openent.presences.common.helper;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class StringHelperTest {

    @Test
    public void camelToSnakeTest(TestContext ctx) {
        ctx.assertEquals(StringHelper.camelToSnake(""), "");
        ctx.assertEquals(StringHelper.camelToSnake("camelCase"), "camel_case");
        ctx.assertEquals(StringHelper.camelToSnake("DTRE"), "d_t_r_e");
        ctx.assertEquals(StringHelper.camelToSnake("snake_case"), "snake_case");
        ctx.assertEquals(StringHelper.camelToSnake("D_T_R_E"), "d__t__r__e");
    }
}
