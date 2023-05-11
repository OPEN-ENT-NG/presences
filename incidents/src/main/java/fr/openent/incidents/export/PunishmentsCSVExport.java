package fr.openent.incidents.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import fr.wseduc.webutils.I18n;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.text.ParseException;
import java.util.*;

public class PunishmentsCSVExport extends CSVExport {
    private final JsonArray punishments;
    private final String locale;
    private final String domain;

    public PunishmentsCSVExport(JsonArray punishments, String domain, String locale) {
        super();
        this.punishments = sort(punishments);
        this.domain = domain;
        this.locale = locale;
        String date = DateHelper.getDateString(new Date(), DateHelper.MONGO_FORMAT);
        this.filename = String.format("%s - %s.csv", "export_punitions", date);
    }

    @Override
    public void generate() {
        for (int i = 0; i < this.punishments.size(); i++) {
            try {
                JsonObject punishment = this.punishments.getJsonObject(i);
                this.value.append(getLine(punishment));
            } catch (ParseException e) {
                LOGGER.error("[Incidents@PunishmentsCSVExport] Failed to parse line. Skipped", e);
            }
        }
    }

    private String getLine(JsonObject punishment) throws ParseException {
        JsonObject fields = punishment.getJsonObject("fields", new JsonObject());

        JsonObject student = punishment.getJsonObject("student");
        Integer punishmentCategoryId = punishment.getJsonObject("type").getInteger("punishment_category_id");

        String line = student.getString("lastName") + SEPARATOR;
        line += student.getString("firstName") + SEPARATOR;
        line += SPACE + student.getString("className") + SEPARATOR;
        line += punishment.getJsonObject("type").getString("label") + SEPARATOR;

        String startAt = fields.getString("start_at");
        String endAt = fields.getString("end_at");

        // Dates fields + time slots
        switch (punishmentCategoryId) {

            case 4: //EXCLUSION
                if (startAt != null && endAt != null) {
                    line += DateHelper.getDateString(startAt, DateHelper.MONGO_FORMAT, DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
                    line += DateHelper.getDateString(endAt, DateHelper.MONGO_FORMAT, DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
                } else {
                    line += " " + SEPARATOR;
                    line += " " + SEPARATOR;
                }
                line += " " + SEPARATOR;
                break;
            case 2: // DETENTION
                if (startAt != null && endAt != null) {
                    line += DateHelper.getDateString(fields.getString("start_at"), DateHelper.MONGO_FORMAT, DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
                    line += " " + SEPARATOR;
                    line += DateHelper.getTimeString(fields.getString("start_at"), DateHelper.MONGO_FORMAT) + " - " +
                            DateHelper.getTimeString(fields.getString("end_at"), DateHelper.MONGO_FORMAT) + SEPARATOR;
                } else {
                    line += " " + SEPARATOR;
                    line += " " + SEPARATOR;
                    line += " " + SEPARATOR;
                }
                break;
            default:
                line += DateHelper.getDateString(punishment.getString("created_at"), DateHelper.MONGO_FORMAT, DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
                line += " " + SEPARATOR;
                line += " " + SEPARATOR;
                break;
        }

        if (punishment.getString("description") == null) {
            line += " " + SEPARATOR;
        } else {
            line += punishment.getString("description").replaceAll("\\s+",  " ").trim() + SEPARATOR;
        }
        line += punishment.getJsonObject("owner").getString("displayName") + SEPARATOR;
        line += getProcessed(punishment.getBoolean("processed")) + SEPARATOR;
        return line + EOL;
    }

    private String getProcessed(boolean processed) {
        if (processed) {
            return I18n.getInstance().translate("incidents.punishments.csv.header.processed.done",
                    domain, locale);
        } else {
            return I18n.getInstance().translate("incidents.punishments.csv.header.processed.undone",
                    domain, locale);
        }
    }

    private JsonArray sort(JsonArray punishments) {
        List<JsonObject> list = punishments.getList();
        Collections.sort(list, (o1, o2) -> o1.getJsonObject("student").getString("name").compareToIgnoreCase(o2.getJsonObject("student").getString("name")));
        return new JsonArray(list);
    }
}
