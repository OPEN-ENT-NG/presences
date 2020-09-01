package fr.openent.incidents.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;

public class PunishmentsCSVExport extends CSVExport {
    private JsonArray punishments;

    public PunishmentsCSVExport(JsonArray punishments) {
        super();
        this.punishments = sort(punishments);
        this.filename = "incidents.punishments.csv.filename";
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
        String line = DateHelper.getDateString(punishment.getString("created_at"), DateHelper.YEAR_MONTH_DAY_HOUR_MINUTES_SECONDS, DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
        line += DateHelper.getTimeString(punishment.getString("created_at"), DateHelper.YEAR_MONTH_DAY_HOUR_MINUTES_SECONDS) + SEPARATOR;
        line += punishment.getJsonObject("student").getString("lastName") + SEPARATOR;
        line += punishment.getJsonObject("student").getString("firstName") + SEPARATOR;
        line += punishment.getJsonObject("student").getString("className") + SEPARATOR;
        line += punishment.getJsonObject("type").getString("label") + SEPARATOR;
        if(punishment.getString("description") == null) {
            line += "" + SEPARATOR;
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
                    Renders.getHost(this.request), I18n.acceptLanguage(this.request));
        } else {
            return I18n.getInstance().translate("incidents.punishments.csv.header.processed.undone",
                    Renders.getHost(this.request), I18n.acceptLanguage(this.request));
        }
    }

    private JsonArray sort(JsonArray punishments) {
        List<JsonObject> list = punishments.getList();
        Collections.sort(list, (o1, o2) -> o1.getJsonObject("student").getString("name").compareToIgnoreCase(o2.getJsonObject("student").getString("name")));
        return new JsonArray(list);
    }
}
