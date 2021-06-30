package fr.openent.statistics_presences.indicator.export;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.statistics_presences.filter.Filter;
import fr.openent.statistics_presences.utils.EventType;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Global extends IndicatorExport {
    private final String HEADER_FORMATTER = "statistics-presences.indicator.filter.type.%s.abbr";
    private final String HEADER_SLOTS_FORMATTER = "statistics-presences.indicator.filter.type.%s.abbr.slots";
    private final String FILENAME = "statistics-presences.indicator.Global.export.filename";
    private final String ABSENCE_TOTAL = "ABSENCE_TOTAL";
    private final String TOTAL = "TOTAL";

    private JsonObject count;
    private JsonObject slots;

    public Global(Filter filter, List<JsonObject> values, JsonObject count, JsonObject slots) {
        super(filter, values);
        this.count = count;
        this.slots = slots;
    }

    @Override
    public void generate() {
        this.setHeader(filter.types());
        this.setFilename(filename());
        value.append(getTotals());
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
        if (isTotalAbsenceSelected()) {
            exportHeaders.add(String.format(HEADER_FORMATTER, ABSENCE_TOTAL));
        }
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
        if (isTotalAbsenceSelected()) {
            line.append(statistics.getJsonObject(ABSENCE_TOTAL, new JsonObject())
                    .getInteger("count", 0)).append(SEPARATOR);
        }
        for (String type : filter.types()) {
            JsonObject statType = statistics.getJsonObject(type, new JsonObject());
            line.append(statType.getInteger("count", 0)).append(SEPARATOR);
            if (Boolean.TRUE.equals(filter.hourDetail()) && isAbsenceType(type)) {
                line.append(statType.getInteger("slots", 0)).append(SEPARATOR);
            }
        }

        return line.append(EOL).toString();
    }


    private String getTotals() {
        StringBuilder line = new StringBuilder();
        line.append(SEPARATOR);
        line.append(TOTAL).append(SEPARATOR);
        if (isTotalAbsenceSelected()) {
            line.append(this.count.getInteger(ABSENCE_TOTAL, 0).toString()).append(SEPARATOR);
        }
        for (String type : filter.types()) {
            line.append(this.count.getInteger(type, 0).toString()).append(SEPARATOR);
            if (Boolean.TRUE.equals(filter.hourDetail()) && isAbsenceType(type)) {
                line.append(this.slots.getInteger(type, 0).toString()).append(SEPARATOR);
            }
        }

        return line.append(EOL).toString();
    }

    private boolean isAbsenceType(String type) {
        return Arrays.asList(EventType.NO_REASON.name(), EventType.UNREGULARIZED.name(), EventType.REGULARIZED.name())
                .contains(type);
    }

    private boolean isTotalAbsenceSelected() {
        return filter.types().contains(EventType.NO_REASON.name()) ||
               filter.types().contains(EventType.UNREGULARIZED.name()) ||
               filter.types().contains(EventType.REGULARIZED.name());
    }

}
