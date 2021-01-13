package fr.openent.presences.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;

public class PresencesCSVExport extends CSVExport {
    private JsonArray presences;

    public PresencesCSVExport(JsonArray presences) {
        super();
        this.presences = sort(presences);
        this.filename = "presences.presences.csv.filename";
    }

    @Override
    public void generate() {
        for (int i = 0; i < this.presences.size(); i++) {
            JsonObject presence = this.presences.getJsonObject(i);
            this.value.append(getLine(presence));
        }
    }

    private String getLine(JsonObject presence) {
        String line = presence.getJsonObject("owner").getString("displayName") + SEPARATOR;
        line += presence.getJsonObject("discipline").getString("label") + SEPARATOR;
        line += DateHelper.getDateString(presence.getString("startDate"), DateHelper.SQL_FORMAT, DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
        line += DateHelper.getDateString(presence.getString("startDate"), DateHelper.SQL_FORMAT, DateHelper.HOUR_MINUTES) + SEPARATOR;
        line += DateHelper.getDateString(presence.getString("endDate"), DateHelper.SQL_FORMAT, DateHelper.HOUR_MINUTES) + SEPARATOR;
        line += presence.getJsonArray("markedStudents").size() + SEPARATOR;
        return line + EOL;
    }

    private JsonArray sort(JsonArray presences) {
        List<JsonObject> list = presences.getList();
        list.sort((o1, o2) -> o1.getJsonObject("owner").getString("displayName").compareToIgnoreCase(o2.getJsonObject("owner").getString("displayName")));
        return new JsonArray(list);
    }
}
