package fr.openent.incidents.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import fr.wseduc.webutils.I18n;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.*;

public class IncidentsCSVExport extends CSVExport {
    private final JsonArray incidents;
    private final String locale;
    private final String domain;
    private static final String NAMES_SEPARATOR = "-";

    public IncidentsCSVExport(JsonArray incidents, String domain, String locale) {
        super();
        this.incidents = incidents;
        this.domain = domain;
        this.locale = locale;
        String date = DateHelper.getDateString(new Date(), DateHelper.MONGO_FORMAT);
        this.filename = String.format("%s - %s.csv", "export_incidents", date);
    }

    @Override
    public void generate() {
        for (int i = 0; i < this.incidents.size(); i++) {
            JsonObject incident = this.incidents.getJsonObject(i);
            this.value.append(getLine(incident));
        }
    }

    private String getLine(JsonObject incident) {
        String line = DateHelper.getDateString(incident.getString("date"), "dd/MM/YYYY kk'h'mm") + SEPARATOR;
        line += incident.getJsonObject("place").getString("label") + SEPARATOR;
        line += incident.getJsonObject("incident_type").getString("label") + SEPARATOR;
        line += incident.getString("description").replaceAll("\\s+",  " ").trim() + SEPARATOR;
        line += incident.getJsonObject("seriousness").getString("label") + SEPARATOR;
        line += getProtagonists(incident.getJsonArray("protagonists")) + SEPARATOR;
        line += incident.getJsonObject("partner").getString("label") + SEPARATOR;
        line += getProcessed(incident.getBoolean("processed")) + SEPARATOR;
        return line + EOL;
    }

    private String getProcessed(boolean processed) {
        if (processed) {
            return I18n.getInstance().translate("incidents.csv.header.processed.done",
                    this.domain, this.locale);
        } else {
            return I18n.getInstance().translate("incidents.csv.header.processed.undone",
                    this.domain, this.locale);
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
                    .append(protagonist.getJsonObject("student") != null ? protagonist.getJsonObject("student").getString("displayName") : "")
                    .append(" ");
        }

        return values.toString();
    }

}
