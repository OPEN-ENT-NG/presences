package fr.openent.statistics_presences.indicator.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.statistics_presences.filter.Filter;
import io.vertx.core.json.JsonObject;

import java.util.List;

public abstract class IndicatorExport extends CSVExport {
    protected Filter filter;
    protected List<JsonObject> values;

    public IndicatorExport(Filter filter, List<JsonObject> values) {
        this.filter = filter;
        this.values = values;
    }
}
