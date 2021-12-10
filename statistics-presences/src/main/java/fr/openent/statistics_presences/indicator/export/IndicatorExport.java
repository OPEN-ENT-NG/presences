package fr.openent.statistics_presences.indicator.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.statistics_presences.model.StatisticsFilter;
import io.vertx.core.json.JsonObject;

import java.util.List;

public abstract class IndicatorExport extends CSVExport {
    protected StatisticsFilter filter;
    protected List<JsonObject> values;

    protected IndicatorExport(StatisticsFilter filter, List<JsonObject> values) {
        this.filter = filter;
        this.values = values;
    }
}
