package fr.openent.presences.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.enums.EventType;
import static fr.openent.presences.enums.EventType.*;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.ParseException;


public class RegistryCSVExport extends CSVExport {

    private final JsonArray events;

    public RegistryCSVExport(JsonArray events) {
        super();
        this.events = events;
        this.filename = "presences.registry.csv.filename";
    }

    @Override
    public void generate() {

        events.forEach(event -> {
            try {
                this.value.append(getLine((JsonObject) event));
            } catch (ParseException e) {
                LOGGER.error("[Presences@RegistryCSVExport] Failed to parse line. Skipped", e);
            }
        });

    }

    /**
     * Get a CSV line for the provided event
     * @param event an event from the registry
     * @return      a CSV line
     * @throws ParseException
     */
    private String getLine(JsonObject event) throws ParseException {
        String line = "";
        line += getLineFromJSON(event,"lastName");
        line += getLineFromJSON(event,"firstName");
        line += SPACE + getLineFromJSON(event,"className");
        line += getType(event.getString("type"));
        line += getLineFromJSON(event,"reason");

        //Start date and time
        if (event.getString("start_date") != null && EventType.valueOf(event.getString("type")) != LATENESS) {
            line += DateHelper.getDateString(event.getString("start_date"), DateHelper.SQL_FORMAT, DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
            line += DateHelper.getTimeString(event.getString("start_date"), DateHelper.SQL_FORMAT) + SEPARATOR;
        } else {
            line += " " + SEPARATOR;
            line += " " + SEPARATOR;
        }

        //End date and time
        if (event.getString("end_date") != null && EventType.valueOf(event.getString("type")) != LATENESS) {
            line += DateHelper.getDateString(event.getString("end_date"), DateHelper.SQL_FORMAT, DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
            line += DateHelper.getTimeString(event.getString("end_date"), DateHelper.SQL_FORMAT) + SEPARATOR;
        } else {
            line += " " + SEPARATOR;
            line += " " + SEPARATOR;
        }


        //Date and time of lateness
        if(EventType.valueOf(event.getString("type")) == LATENESS) {
            line += DateHelper.getDateString(event.getString("end_date"), DateHelper.SQL_FORMAT, DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
            line += DateHelper.getTimeString(event.getString("end_date"), DateHelper.SQL_FORMAT) + SEPARATOR;
        } else {
            line += " " + SEPARATOR;
            line += " " + SEPARATOR;
        }

        line += getLineFromJSON(event, "place");
        line += getLineFromJSON(event, "protagonist_type");

        return line + EOL;
    }

    /**
     * Get the CSV format for the event Type parameter
     * @param type  type of the event
     * @return      the parameter value in CSV format
     */
    private String getType(String type) {
        String line = "";
        switch (EventType.valueOf(type)) {
            case ABSENCE:
                line += I18n.getInstance().translate("presences.registry.csv.absence",
                        Renders.getHost(this.request), I18n.acceptLanguage(this.request)) + SEPARATOR;
                break;
            case LATENESS:
                line += I18n.getInstance().translate("presences.registry.csv.lateness",
                        Renders.getHost(this.request), I18n.acceptLanguage(this.request)) + SEPARATOR;
                break;
            case DEPARTURE:
                line += I18n.getInstance().translate("presences.registry.csv.departure",
                        Renders.getHost(this.request), I18n.acceptLanguage(this.request)) + SEPARATOR;
                break;
            case REMARK:
                line += I18n.getInstance().translate("presences.registry.csv.remark",
                        Renders.getHost(this.request), I18n.acceptLanguage(this.request)) + SEPARATOR;
                break;
            case INCIDENT:
                line += I18n.getInstance().translate("presences.registry.csv.incident",
                        Renders.getHost(this.request), I18n.acceptLanguage(this.request)) + SEPARATOR;
                break;
            case FORGOTTEN_NOTEBOOK:
                line += I18n.getInstance().translate("presences.registry.csv.forgotten.notebook",
                        Renders.getHost(this.request), I18n.acceptLanguage(this.request)) + SEPARATOR;
            default:
                line += " " + SEPARATOR;
        }
        return line;
    }


    /**
     * Get the provided event key value in CSV format.
     * @param event     the event
     * @param key       the JSON parameter of the event
     * @return          the CSV element
     */
    private String getLineFromJSON(JsonObject event, String key) {
        if(event.getString(key) != null) {
            return event.getString(key) + SEPARATOR;
        } else {
            return " " + SEPARATOR;
        }
    }
}
