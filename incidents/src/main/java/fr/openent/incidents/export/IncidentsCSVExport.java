package fr.openent.incidents.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.ParseException;

public class IncidentsCSVExport extends CSVExport {
    private JsonArray incidents;
    private String NAMES_SEPARATOR = "-";

    public IncidentsCSVExport(JsonArray incidents) {
        super();
        this.incidents = incidents;
        this.filename = "incidents.csv.filename";
    }

    @Override
    public void generate() {
        for (int i = 0; i < this.incidents.size(); i++) {
            try {
                JsonObject incident = this.incidents.getJsonObject(i);
                this.value.append(getLine(incident));
            } catch (ParseException e) {
                LOGGER.error("[Incidents@IncidentsCSVExport] Failed to parse line. Skipped", e);
            }
        }
    }

    private String getLine(JsonObject incident) throws ParseException {
        String line = DateHelper.getDateString(incident.getString("date"), "dd/MM/YYYY kk'h'mm") + SEPARATOR;
        line += incident.getJsonObject("place").getString("label") + SEPARATOR;
        line += incident.getJsonObject("incident_type").getString("label") + SEPARATOR;
        line += incident.getString("description") + SEPARATOR;
        line += incident.getJsonObject("seriousness").getString("label") + SEPARATOR;
        line += getProtagonists(incident.getJsonArray("protagonists")) + SEPARATOR;
        line += incident.getJsonObject("partner").getString("label") + SEPARATOR;
        line += getProcessed(incident.getBoolean("processed")) + SEPARATOR;
        return line + EOL;
    }

    private String getProcessed(boolean processed) {
        if (processed) {
            return I18n.getInstance().translate("incidents.csv.header.processed.done",
                    Renders.getHost(this.request), I18n.acceptLanguage(this.request));
        } else {
            return I18n.getInstance().translate("incidents.csv.header.processed.undone",
                    Renders.getHost(this.request), I18n.acceptLanguage(this.request));
        }
    }

    private String getProtagonists(JsonArray protagonists) {
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < protagonists.size(); i++) {
            JsonObject protagonist = protagonists.getJsonObject(i);

            values.append(" ")
                    .append(this.NAMES_SEPARATOR)
                    .append(" ")
                    .append(protagonist.getJsonObject("type").getString("label"))
                    .append(": ")
                    .append(protagonist.getJsonObject("student").getString("lastName"))
                    .append(" ")
                    .append(protagonist.getJsonObject("student").getString("firstName"))
                    .append(" ");
        }

        return values.toString();
    }

}
