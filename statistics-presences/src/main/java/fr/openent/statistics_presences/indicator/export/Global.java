package fr.openent.statistics_presences.indicator.export;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.EventRecoveryDay;
import fr.openent.statistics_presences.model.StatisticsFilter;
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
    private final String HEADER_FORMATTER_RATE = "statistics-presences.indicator.filter.type.%s.abbr.rate";
    private final String HEADER_SLOTS_FORMATTER = "statistics-presences.indicator.filter.type.%s.abbr.slots";
    private final String FILENAME = "statistics-presences.indicator.Global.export.filename";
    private final String ABSENCE_TOTAL = "ABSENCE_TOTAL";
    private final String TOTAL = "TOTAL";

    private final JsonObject count;
    private final JsonObject slots;
    private final JsonObject rate;
    private final String recoveryMethod;

    public Global(StatisticsFilter filter, List<JsonObject> values, JsonObject count, JsonObject slots, JsonObject rate,
                  String recoveryMethod) {
        super(filter, values);
        this.count = count;
        this.slots = slots;
        this.rate = rate;
        this.recoveryMethod = recoveryMethod;
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
            // type total absence + its rate
            exportHeaders.add(String.format(HEADER_FORMATTER, ABSENCE_TOTAL));
            if (canDisplayRateAbsence()) {
                exportHeaders.add(String.format(HEADER_FORMATTER_RATE, ABSENCE_TOTAL));
            }
        }
        for (String type : types) {
            // type column
            exportHeaders.add(String.format(HEADER_FORMATTER, type));
            // type rate absence column
            if (isAbsenceType(type) && canDisplayRateAbsence()) {
                exportHeaders.add(String.format(HEADER_FORMATTER_RATE, type));
            }
            // type column in slots
            if (Boolean.TRUE.equals(filter.hourDetail()) && isAbsenceType(type)) {
                exportHeaders.add(String.format(HEADER_SLOTS_FORMATTER, type));
            }
        }

        super.setHeader(exportHeaders);
    }

    private String getLine(JsonObject value) {
        StringBuilder line = new StringBuilder();
        line.append(SPACE).append(value.getString("audience")).append(SEPARATOR)
                .append(value.getString(Field.NAME)).append(SEPARATOR);

        JsonObject statistics = value.getJsonObject("statistics", new JsonObject());
        if (isTotalAbsenceSelected()) {
            // total absence + its rate value
            line.append(statistics.getJsonObject(ABSENCE_TOTAL, new JsonObject())
                    .getInteger(Field.COUNT, 0)).append(SEPARATOR);
            if (canDisplayRateAbsence()) {
                line.append(statistics.getJsonObject(ABSENCE_TOTAL, new JsonObject())
                        .getDouble(Field.RATE, 0.0)).append("%").append(SEPARATOR);
            }
        }
        for (String type : filter.types()) {
            JsonObject statType = statistics.getJsonObject(type, new JsonObject());
            // type type event absence rate value
            line.append(statType.getInteger(Field.COUNT, 0)).append(SEPARATOR);
            // type event absence rate value
            if (isAbsenceType(type) && canDisplayRateAbsence()) {
                line.append(statType.getDouble(Field.RATE, 0.0)).append("%").append(SEPARATOR);
            }
            // type line event type slot
            if (Boolean.TRUE.equals(filter.hourDetail()) && isAbsenceType(type)) {
                line.append(statType.getInteger(Field.SLOTS, 0)).append(SEPARATOR);
            }
        }
        return line.append(EOL).toString();
    }

    private String getTotals() {
        StringBuilder line = new StringBuilder();
        line.append(SEPARATOR);
        line.append(TOTAL).append(SEPARATOR);
        if (isTotalAbsenceSelected()) {
            // total absence value
            line.append(this.count.getInteger(ABSENCE_TOTAL, 0).toString()).append(SEPARATOR);
            // total absence rate value
            if (canDisplayRateAbsence()) {
                line.append(this.rate.getDouble(ABSENCE_TOTAL, 0.0)).append("%").append(SEPARATOR);
            }
        }
        for (String type : filter.types()) {
            // total event type value
            line.append(this.count.getInteger(type, 0).toString()).append(SEPARATOR);
            // total event type rate value
            if (isAbsenceType(type) && canDisplayRateAbsence()) {
                line.append(this.rate.getDouble(type, 0.0)).append("%").append(SEPARATOR);
            }
            // total event type value in slots
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

    /**
     * check if you should display rate according to its recovery method
     * should allow only for DAY and HALF DAY
     *
     * @return {@link boolean}
     */
    private boolean canDisplayRateAbsence() {
        return recoveryMethod.equals(EventRecoveryDay.DAY.type()) || recoveryMethod.equals(EventRecoveryDay.HALF_DAY.type());
    }

}
