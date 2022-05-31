package fr.openent.presences.helper;

import fr.openent.presences.common.helper.DateHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;


@RunWith(VertxUnitRunner.class)
public class CalendarHelperTest {

    @Test
    public void testSetExcludeDay_is_false_when_endDate_is_not_included(TestContext ctx) throws Exception {
        String date = "2022-05-30T00:00:00";
        JsonArray exclusionDay = new JsonArray()
                .add(new JsonObject()
                        .put("start_date", "2022-05-26T00:00:00")
                        .put("end_date", "2022-05-30T00:00:00"));

        boolean res = Whitebox.invokeMethod(CalendarHelper.class, "isMatchWithExclusionsDate", exclusionDay, date);
        ctx.assertFalse(res);
    }

    @Test
    public void testSetExcludeDay_is_true_when_endDate_is_included(TestContext ctx) throws Exception {
        String date = "2022-05-30T00:00:00";
        JsonArray exclusionDay = new JsonArray()
                .add(new JsonObject()
                        .put("start_date", "2022-05-26T00:00:00")
                        .put("end_date", "2022-05-30T23:59:59"));

        boolean res = Whitebox.invokeMethod(CalendarHelper.class, "isMatchWithExclusionsDate", exclusionDay, date);
        ctx.assertTrue(res);
    }
}