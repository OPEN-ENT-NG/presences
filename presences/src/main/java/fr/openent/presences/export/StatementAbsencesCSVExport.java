package fr.openent.presences.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.model.StatementAbsence;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.ParseException;

public class StatementAbsencesCSVExport extends CSVExport {
    private JsonArray statementAbsences;

    public StatementAbsencesCSVExport(JsonArray statementAbsences) {
        super();
        this.statementAbsences = statementAbsences;
        this.filename = "presences.statements.absence.csv.filename";
    }

    @Override
    public void generate() {
        for (int i = 0; i < this.statementAbsences.size(); i++) {
            try {
                JsonObject statement = this.statementAbsences.getJsonObject(i);
                this.value.append(getLine(statement));
            } catch (ParseException e) {
                LOGGER.error("[Presences@StatementAbsencesCSVExport::generate()] Failed to parse line. Skipped", e);
            }
        }
    }

    private String getLine(JsonObject statementAbsence) throws ParseException {
        String treated_at = statementAbsence.getString("treated_at");
        JsonObject parent = statementAbsence.getJsonObject("parent");
        String line = statementAbsence.getJsonObject("student").getString("lastName") + SEPARATOR;
        line += statementAbsence.getJsonObject("student").getString("firstName") + SEPARATOR;
        line += (parent != null ? parent.getString("lastName") : "") + SEPARATOR;
        line += (parent != null ? parent.getString("firstName") : "") + SEPARATOR;
        line += DateHelper.getDateString(statementAbsence.getString("start_at"), DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
        line += DateHelper.getDateString(statementAbsence.getString("start_at"), DateHelper.SAFE_HOUR_MINUTES) + SEPARATOR;
        line += DateHelper.getDateString(statementAbsence.getString("end_at"), DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
        line += DateHelper.getDateString(statementAbsence.getString("end_at"), DateHelper.SAFE_HOUR_MINUTES) + SEPARATOR;
        line += statementAbsence.getString("description") + SEPARATOR;
        line += (treated_at != null ? DateHelper.getDateString(treated_at, DateHelper.DAY_MONTH_YEAR) : "") + SEPARATOR;
        line += (treated_at != null ? DateHelper.getDateString(treated_at, DateHelper.SAFE_HOUR_MINUTES) : "") + SEPARATOR;
        line += DateHelper.getDateString(statementAbsence.getString("created_at"), DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
        line += DateHelper.getDateString(statementAbsence.getString("created_at"), DateHelper.SAFE_HOUR_MINUTES) + SEPARATOR;
        return line + EOL;
    }

}
