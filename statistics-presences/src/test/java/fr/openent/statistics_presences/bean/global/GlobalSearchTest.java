package fr.openent.statistics_presences.bean.global;

import fr.openent.statistics_presences.model.StatisticsFilter;
import fr.openent.statistics_presences.utils.EventType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.List;


@RunWith(VertxUnitRunner.class)
public class GlobalSearchTest {
    private GlobalSearch search;

    private static final String STRUCTURE_ID = "111";
    private static final List<Integer> PUNISHMENT_TYPE_IDS = Arrays.asList(11, 12);
    private static final List<Integer> SANCTION_TYPE_IDS = Arrays.asList(21, 22);
    private static final List<String> TYPES = Arrays.asList(EventType.UNREGULARIZED.name(), EventType.REGULARIZED.name(),
            EventType.SANCTION.name(), EventType.PUNISHMENT.name(), EventType.LATENESS.name());


    @Test
    public void testFilterTypeWithNullNOLATENESSREASON(TestContext ctx) throws Exception {
        JsonObject filter = new JsonObject()
                .put(StatisticsFilter.StatisticsFilterField.TYPES, TYPES)
                .putNull(StatisticsFilter.StatisticsFilterField.NOLATENESSREASON)
                .put(StatisticsFilter.StatisticsFilterField.REASONS, new JsonArray(Arrays.asList(1, 2)))
                .put(StatisticsFilter.StatisticsFilterField.SANCTION_TYPES, SANCTION_TYPE_IDS)
                .put(StatisticsFilter.StatisticsFilterField.PUNISHMENT_TYPES, PUNISHMENT_TYPE_IDS);

        search = new GlobalSearch(new StatisticsFilter(STRUCTURE_ID, filter));

        JsonArray expected = new JsonArray()
                .add(new JsonObject("{\"type\":\"UNREGULARIZED\",\"reason\":{\"$in\":[1,2]}}"))
                .add(new JsonObject("{\"type\":\"REGULARIZED\",\"reason\":{\"$in\":[1,2]}}"))
                .add(new JsonObject("{\"type\":\"SANCTION\",\"punishment_type\":{\"$in\":[21,22]}}"))
                .add(new JsonObject("{\"type\":\"PUNISHMENT\",\"punishment_type\":{\"$in\":[11,12]}}"))
                .add(new JsonObject("{\"type\":\"LATENESS\",\"reason\":{\"$in\":[1,2]}}"));

        JsonArray result = Whitebox.invokeMethod(search, "filterType", TYPES);

        ctx.assertEquals(expected, result);
    }
}
