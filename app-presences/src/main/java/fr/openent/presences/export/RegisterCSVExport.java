package fr.openent.presences.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.model.Course;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class RegisterCSVExport extends CSVExport {
    private final JsonArray registers;
    private final boolean forgotten;
    private final String NAMES_SEPARATOR = "-";

    public RegisterCSVExport(JsonArray registers, boolean forgotten) {
        super();
        this.registers = registers;
        this.forgotten = forgotten;
        this.filename = "presences.register.csv.filename";
    }

    @Override
    public void generate() {
        List<Course> courses = this.registers.getList();
        for (Course course : courses) {
            try {
                this.value.append(getLine(course));
            } catch (ParseException e) {
                LOGGER.error("[Presences@RegisterCSVExport] Failed to parse line. Skipped", e);
            }
        }
    }

    private String getLine(Course register) throws ParseException {
        String line = getRegisterDate(register.getStartDate()) + SEPARATOR;
        line += getRegisterTime(register.getStartDate(), register.getEndDate()) + SEPARATOR;
        line += getTeachersNames(register.getTeachers()) + SEPARATOR;
        line += SPACE + getGroupsNames(register.getClasses(), register.getGroups()) + SEPARATOR;
        line += register.getSubjectName() + SEPARATOR;
        line += getForgottenRegister();
        return line + EOL;
    }

    private String getRegisterDate(String start) throws ParseException {
        SimpleDateFormat df = DateHelper.getMongoSimpleDateFormat();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date startDate = df.parse(start);
        return dateFormat.format(startDate);
    }

    private String getRegisterTime(String start, String end) throws ParseException {
        SimpleDateFormat df = DateHelper.getMongoSimpleDateFormat();
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm");
        Date startDate = df.parse(start);
        Date endDate = df.parse(end);
        return hourFormat.format(startDate) + "-" + hourFormat.format(endDate);
    }

    private String getTeachersNames(JsonArray teachers) {
        StringBuilder names = new StringBuilder();
        if (teachers.isEmpty()) return names.toString();
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

    private String getForgottenRegister() {
        if (this.forgotten) {
            return I18n.getInstance().translate("presences.exemptions.csv.attendance.true",
                    Renders.getHost(this.request), I18n.acceptLanguage(this.request));
        } else {
            return I18n.getInstance().translate("presences.exemptions.csv.attendance.false",
                    Renders.getHost(this.request), I18n.acceptLanguage(this.request));
        }
    }

}
