package fr.openent.statistics_presences.indicator.export;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.statistics_presences.filter.Filter;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Global extends IndicatorExport {
    private String HEADER_FORMATTER = "statistics-presences.indicator.filter.type.%s.abbr";
    private String HEADER_SLOTS_FORMATTER = "statistics-presences.indicator.filter.type.%s.abbr.slots";
    private String FILENAME = "statistics-presences.indicator.Global.export.filename";

    public Global(Filter filter, List<JsonObject> values) {
        super(filter, values);
    }

    @Override
    public void generate() {
        this.setHeader(filter.types());
        this.setFilename(filename());
        for (JsonObject stat : values) {
            value.append(getLine(stat));
        }
    }

    private String filename() {
        String name = I18n.getInstance().translate(FILENAME, Renders.getHost(request), I18n.acceptLanguage(request));
        String date = DateHelper.getDateString(new Date(), DateHelper.MONGO_FORMAT);
        return String.format("%s - %s.csv", name, date);
    }

    @Override
    public void setHeader(List<String> types) {
        List<String> exportHeaders = new ArrayList<>();
        exportHeaders.add("statistics-presences.classes");
        exportHeaders.add("statistics-presences.students");
        for (String type : types) {
            exportHeaders.add(String.format(HEADER_FORMATTER, type));
            if (Boolean.TRUE.equals(filter.hourDetail()) && isAbsenceType(type)) {
                exportHeaders.add(String.format(HEADER_SLOTS_FORMATTER, type));
            }
        }

        super.setHeader(exportHeaders);
    }

    private String getLine(JsonObject value) {
        StringBuilder line = new StringBuilder();
        line.append(value.getString("audience")).append(SEPARATOR)
                .append(value.getString("name")).append(SEPARATOR);

        JsonObject statistics = value.getJsonObject("statistics", new JsonObject());
        for (String type : filter.types()) {
            JsonObject statType = statistics.getJsonObject(type, new JsonObject());
            line.append(statType.getInteger("count", 0)).append(SEPARATOR);
            if (Boolean.TRUE.equals(filter.hourDetail()) && isAbsenceType(type)) {
                line.append(statType.getInteger("slots", 0)).append(SEPARATOR);
            }
        }

        return line.append(EOL).toString();
    }

    private boolean isAbsenceType(String type) {
        return ("JUSTIFIED_UNREGULARIZED_ABSENCE".equals(type) || "UNJUSTIFIED_ABSENCE".equals(type) || "REGULARIZED_ABSENCE".equals(type));
    }


}
