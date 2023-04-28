package fr.openent.presences.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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
            JsonObject register = this.exemptions.getJsonObject(i);
            this.value.append(getLine(register));
        }
    }

    private String getLine(JsonObject exemption) {
        String line = exemption.getJsonObject("student").getString("firstName") + SEPARATOR;
        line += exemption.getJsonObject("student").getString("lastName") + SEPARATOR;
        line += SPACE + exemption.getJsonObject("student").getString("classeName") + SEPARATOR;
        line += exemption.getJsonObject("subject").getString("name") + SEPARATOR;
        line += DateHelper.getDateString(exemption.getString("start_date"), "dd/MM/yyyy") + SEPARATOR;
        line += DateHelper.getDateString(exemption.getString("end_date"), "dd/MM/yyyy") + SEPARATOR;
        line += exemption.getString("comment") + SEPARATOR;
        line += translate("presences.exemptions.csv.attendance." + exemption.getBoolean("attendance")) + SEPARATOR;
        return line + EOL;
    }
}
