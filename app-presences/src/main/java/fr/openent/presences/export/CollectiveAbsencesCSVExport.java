package fr.openent.presences.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollectiveAbsencesCSVExport extends CSVExport {

    private final JsonArray collectiveAbsences;

    public CollectiveAbsencesCSVExport(JsonArray collectiveAbsences) {
        super();
        this.collectiveAbsences = collectiveAbsences;
        this.filename = "presences.collective.absences.csv.filename";
    }

    @Override
    public void generate() {
        setCollectiveAbsencesHeader();

        for (int i = 0; i < this.collectiveAbsences.size(); i++) {
            JsonObject collectiveAbsence = this.collectiveAbsences.getJsonObject(i);
            JsonArray students = collectiveAbsence.getJsonArray("students", new JsonArray());

            this.value.append(getCollectiveLine(collectiveAbsence));

            for (int j = 0; j < students.size(); j++) {
                JsonObject student = students.getJsonObject(j);
                this.value.append(getStudentLine(student));
            }

            this.value.append(EOL).append(EOL);
        }

    }

    private String getCollectiveLine(JsonObject collectiveAbsence) {
        String line = collectiveAbsence.getString("reason") + SEPARATOR;
        line += collectiveAbsence.getString("comment") + SEPARATOR;
        line += DateHelper.getDateString(collectiveAbsence.getString("startDate"), DateHelper.SQL_FORMAT, DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
        line += DateHelper.getDateString(collectiveAbsence.getString("startDate"), DateHelper.SQL_FORMAT, DateHelper.HOUR_MINUTES) + SEPARATOR;
        line += DateHelper.getDateString(collectiveAbsence.getString("endDate"), DateHelper.SQL_FORMAT, DateHelper.DAY_MONTH_YEAR) + SEPARATOR;
        line += DateHelper.getDateString(collectiveAbsence.getString("endDate"), DateHelper.SQL_FORMAT, DateHelper.HOUR_MINUTES) + SEPARATOR;
        line += collectiveAbsence.getLong("countStudent").toString() + SEPARATOR;
        line += collectiveAbsence.getLong("id").toString() + SEPARATOR;

        return line + EOL;
    }

    private String getStudentLine(JsonObject student) {
        String line = "";
        for (int i = 0; i < 8 ; i++) {
            line += SEPARATOR;
        }
        line += student.getString("lastName", "") + SEPARATOR;
        line += student.getString("firstName", "") + SEPARATOR;
        line += SPACE + student.getString("audienceName", "") + SEPARATOR;

        return line + EOL;
    }

    private void setCollectiveAbsencesHeader() {
        List<String> csvHeaders = new ArrayList<>(Arrays.asList(
                "presences.collective.absences.csv.header.reason",
                "presences.collective.absences.csv.header.comment",
                "presences.collective.absences.csv.header.start.date",
                "presences.collective.absences.csv.header.start.time",
                "presences.collective.absences.csv.header.end.date",
                "presences.collective.absences.csv.header.end.time",
                "presences.collective.absences.csv.header.nb.students",
                "presences.collective.absences.csv.header.id",
                "presences.collective.absences.csv.header.lastname",
                "presences.collective.absences.csv.header.firstname",
                "presences.collective.absences.csv.header.audience"));
        this.setHeader(csvHeaders);
    }
}
