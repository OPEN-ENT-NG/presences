package fr.openent.presences.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.ParseException;

import static fr.openent.presences.constants.Alerts.*;

public class AlertsCSVExport extends CSVExport {
    private JsonArray alerts;

    public AlertsCSVExport(JsonArray alerts) {
        super();
        this.alerts = alerts;
        this.filename = "presences.alerts.csv.filename";
    }

    @Override
    public void generate() {
        for (int i = 0; i < alerts.size(); i++) {
            try {
                JsonObject alert = this.alerts.getJsonObject(i);
                String type = this.alerts.getJsonObject(i).getString("type");
                this.value.append(getLine(alert, type));
            } catch (ParseException e) {
                LOGGER.error("[Presences@AlertsCSVExport::generate] Failed to parse line. Skipped", e);
            }
        }
    }

    public String getStringType(String type) {
        if (type == null) {
            return "";
        }

        switch (type) {
            case INCIDENT: {
                return I18n.getInstance().translate("presences.register.event_type.incident",
                        Renders.getHost(this.request), I18n.acceptLanguage(this.request));
            }
            case ABSENCE: {
                return I18n.getInstance().translate("presences.register.event_type.absences",
                        Renders.getHost(this.request), I18n.acceptLanguage(this.request));
            }
            case FORGOTTEN_NOTEBOOK: {
                return I18n.getInstance().translate("presences.forgotten.notebook",
                        Renders.getHost(this.request), I18n.acceptLanguage(this.request));
            }
            case LATENESS: {
                return I18n.getInstance().translate("presences.register.event_type.lateness",
                        Renders.getHost(this.request), I18n.acceptLanguage(this.request));
            }
            default: {
                return "";
            }
        }
    }

    private String getLine(JsonObject alert, String type) throws ParseException {
        String line = alert.getString("lastName") + SEPARATOR;
        line += alert.getString("firstName") + SEPARATOR;
        line += SPACE + alert.getString("audience") + SEPARATOR;
        line += getStringType(type) + SEPARATOR;
        line += alert.getLong("count") + SEPARATOR;
        return line + EOL;
    }

}
