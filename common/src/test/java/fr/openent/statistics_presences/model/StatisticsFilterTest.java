package fr.openent.statistics_presences.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class StatisticsFilterTest {

    private final JsonArray types = new JsonArray()
            .add("type1")
            .add("type2");
    private final JsonArray audiences = new JsonArray()
            .add("3eme1");
    private final JsonArray users = new JsonArray();
    private final JsonArray reasons = new JsonArray()
            .add(31)
            .add(32)
            .add(40);
    private final JsonArray punishmentTypes = new JsonArray();
    private final JsonArray sanctionTypes = new JsonArray();
    private final JsonObject filters = new JsonObject()
            .put("FROM", 0)
            .put("TO", 100)
            .put("HOUR_DETAIL", true);
    private final JsonObject filterJsonObject_1 = new JsonObject()
            .put("start", "2021-12-14T00:00:00")
            .put("end", "2021-12-14T23:59:00")
            .put("types", types)
            .put("audiences", audiences)
            .put("users", users)
            .put("reasons", reasons)
            .put("punishmentTypes", punishmentTypes)
            .put("sanctionTypes", sanctionTypes)
            .put("export_option", "exportOption")
            .put("filters", filters);

    @Test
    public void testFilterNotNull(TestContext ctx) {
        StatisticsFilter filter = new StatisticsFilter("structureId", filterJsonObject_1);
        ctx.assertNotNull(filter);
    }

    @Test
    public void testFilterHasContentWithObject(TestContext ctx) {
        StatisticsFilter filter = new StatisticsFilter("structureId", filterJsonObject_1);
        boolean res = filter.start().equals("2021-12-14T00:00:00") &&
                filter.end().equals("2021-12-14T23:59:00") &&
                filter.structure().equals("structureId") &&
                filter.types().size() == 2 &&
                filter.audiences().size() == 1 &&
                filter.users().isEmpty() &&
                filter.reasons().size() == 3 &&
                filter.punishmentTypes().isEmpty() &&
                filter.sanctionTypes().isEmpty() &&
                filter.punishmentTypes().isEmpty() &&
                filter.exportOption().equals("exportOption") &&
                filter.from() == 0 &&
                filter.to() == 100 &&
                filter.hourDetail();
        ctx.assertTrue(res);
        ctx.assertNull(filter.page());
        filter.setPage(0);
        ctx.assertEquals(filter.page(), 0);
    }
}
