package fr.openent.presences.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PresencesCSVExport extends CSVExport {
    private static final String START_DATE = "startDate";
    private static final String STUDENT = "student";

    private JsonArray presences;

    public PresencesCSVExport(JsonArray presences) {
        super();
        this.presences = sortPresence(presences);
        this.filename = "presences.presences.csv.filename";
    }

    @Override
    public void generate() {
        for (int i = 0; i < this.presences.size(); i++) {
            setPresenceHeader();
            JsonObject presence = this.presences.getJsonObject(i);
            JsonArray markedStudents = presence.getJsonArray("markedStudents");
            this.value.append(getPresenceLine(presence));
            setStudentHeader();
            for (int j = 0; j < markedStudents.size(); j++) {
                JsonObject markedStudent = markedStudents.getJsonObject(j);
                this.value.append(getStudentLine(markedStudent));
            }
            this.value.append(EOL).append(EOL);
        }
    }

    private String getPresenceLine(JsonObject presence) {
        JsonObject owner = presence.getJsonObject("owner");
        String line = owner.getString("firstName") + SEPARATOR;
        line += owner.getString("lastName") + SEPARATOR;
        line += presence.getJsonObject("discipline").getString("label") + SEPARATOR;
        line += DateHelper.getDateString(presence.getString(START_DATE), DateHelper.SQL_FORMAT, DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
        line += DateHelper.getDateString(presence.getString(START_DATE), DateHelper.SQL_FORMAT, DateHelper.HOUR_MINUTES) + SEPARATOR;
        line += DateHelper.getDateString(presence.getString("endDate"), DateHelper.SQL_FORMAT, DateHelper.HOUR_MINUTES) + SEPARATOR;
        line += presence.getJsonArray("markedStudents").size() + SEPARATOR;
        return line + EOL;
    }

    private String getStudentLine(JsonObject markedStudent) {
        JsonObject student = markedStudent.getJsonObject(STUDENT);
        String line = student.getString("firstName") + SEPARATOR;
        line += student.getString("lastName") + SEPARATOR;
        line += SPACE + student.getString("classeName") + SEPARATOR;
        line += markedStudent.getString("comment") + SEPARATOR;
        return line + EOL;
    }

    private JsonArray sortPresence(JsonArray presences) {
        List<JsonObject> list = presences.getList();
        list.sort((p1, p2) -> DateHelper.isDateBeforeOrEqual(p1.getString(START_DATE), p2.getString(START_DATE)) ? 1 : -1);
        return new JsonArray(list);
    }

    private void setPresenceHeader() {
        List<String> csvHeaders = new ArrayList<>(Arrays.asList(
                "presences.presences.csv.header.owner.firstName",
                "presences.presences.csv.header.owner.lastName",
                "presences.presences.csv.header.discipline",
                "presences.presences.csv.header.date",
                "presences.presences.csv.header.start.time",
                "presences.presences.csv.header.end.time",
                "presences.presences.csv.header.student.number"));
        this.setHeader(csvHeaders);
    }

    private void setStudentHeader() {
        List<String> csvHeaders = new ArrayList<>(Arrays.asList(
                "presences.csv.header.student.firstName",
                "presences.csv.header.student.lastName",
                "presences.csv.header.student.className",
                "presences.csv.header.student.comment"));
        this.setHeader(csvHeaders);
    }

}
