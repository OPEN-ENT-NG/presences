package fr.openent.presences.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RegisterCSVExport extends CSVExport {
    private JsonArray registers;
    private String NAMES_SEPARATOR = "-";

    public RegisterCSVExport(JsonArray registers) {
        super();
        this.registers = registers;
        this.filename = "presences.register.csv.filename";
    }

    @Override
    public void generate() {
        for (int i = 0; i < this.registers.size(); i++) {
            try {
                JsonObject register = this.registers.getJsonObject(i);
                this.value.append(getLine(register));
            } catch (ParseException e) {
                LOGGER.error("[Presences@RegisterCSVExport] Failed to parse line. Skipped", e);
            }
        }
    }

    private String getLine(JsonObject register) throws ParseException {
        String line = getRegisterDate(register.getString("startDate"), register.getString("endDate")) + SEPARATOR;
        line += getTeachersNames(register.getJsonArray("teachers")) + SEPARATOR;
        line += getGroupsNames(register.getJsonArray("classes"), register.getJsonArray("groups")) + SEPARATOR;
        line += register.getString("subjectName", "");
        return line + EOL;
    }

    private String getRegisterDate(String start, String end) throws ParseException {
        SimpleDateFormat df = DateHelper.getMongoSimpleDateFormat();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm");
        Date startDate = df.parse(start);
        Date endDate = df.parse(end);
        return dateFormat.format(startDate) + " " + hourFormat.format(startDate) + "-" + hourFormat.format(endDate);
    }

    private String getTeachersNames(JsonArray teachers) {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < teachers.size(); i++) {
            JsonObject teacher = teachers.getJsonObject(i);
            names.append(teacher.getString("displayName"))
                    .append(NAMES_SEPARATOR);
        }
        return names.toString().substring(0, names.length() - NAMES_SEPARATOR.length());
    }

    private String getGroupsNames(JsonArray classes, JsonArray groups) {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < classes.size(); i++) {
            names.append(classes.getString(i))
                    .append(NAMES_SEPARATOR);
        }

        for (int i = 0; i < groups.size(); i++) {
            names.append(groups.getString(i))
                    .append(NAMES_SEPARATOR);
        }

        return names.toString().substring(0, names.length() - NAMES_SEPARATOR.length());
    }
}
