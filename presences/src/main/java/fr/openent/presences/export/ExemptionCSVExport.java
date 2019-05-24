package fr.openent.presences.export;

import fr.openent.presences.common.helper.CSVExport;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.ParseException;

public class ExemptionCSVExport extends CSVExport {
    private JsonArray exemptions;

    public ExemptionCSVExport(JsonArray exemptions) {
        super();
        this.exemptions = exemptions;
        this.filename = "presences.exemptions.csv.filename";
    }

    @Override
    public void generate() {
        for (int i = 0; i < this.exemptions.size(); i++) {
            try {
                JsonObject register = this.exemptions.getJsonObject(i);
                this.value.append(getLine(register));
            } catch (ParseException e) {
                LOGGER.error("[Presences@RegisterCSVExport] Failed to parse line. Skipped", e);
            }
        }
    }

    private String getLine(JsonObject exemption) throws ParseException {
        String line = exemption.getJsonObject("student").getString("firstName") + SEPARATOR;
        line += exemption.getJsonObject("student").getString("lastName") + SEPARATOR;
        line += exemption.getJsonObject("student").getString("classeName") + SEPARATOR;
        line += exemption.getJsonObject("subject").getString("name") + SEPARATOR;
        line += exemption.getString("start_date") + SEPARATOR;
        line += exemption.getString("end_date") + SEPARATOR;
        line += exemption.getString("comment") + SEPARATOR;
        line += exemption.getBoolean("attendance") + SEPARATOR;
        return line + EOL;
    }
}
