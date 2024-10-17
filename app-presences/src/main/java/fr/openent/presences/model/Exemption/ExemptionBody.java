package fr.openent.presences.model.Exemption;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class ExemptionBody extends Exemption {
    private String structure_id;
    private Boolean is_recursive;
    private List<String> student_id;
    private String startDateRecursive;
    private String endDateRecursive;
    private List<String> day_of_weeks; // should be ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]

    @SuppressWarnings("unchecked")
    public ExemptionBody(JsonObject exemptionBody) {
        this.structure_id = exemptionBody.getString("structure_id", null);
        this.subject_id = exemptionBody.getString("subject_id", null);
        this.start_date = exemptionBody.getString("start_date", null);
        this.end_date = exemptionBody.getString("end_date", null);
        this.attendance = exemptionBody.getBoolean("attendance", false);
        this.is_recursive = exemptionBody.getBoolean("is_recursive", false);
        this.is_every_two_weeks = exemptionBody.getBoolean("is_every_two_weeks", false);
        this.comment = exemptionBody.getString("comment", null);
        this.student_id = exemptionBody.getJsonArray("student_id", new JsonArray()).getList();
        this.startDateRecursive = exemptionBody.getString("startDateRecursive", null);
        this.endDateRecursive = exemptionBody.getString("endDateRecursive", null);
        this.day_of_weeks = exemptionBody.getJsonArray("day_of_week", new JsonArray()).getList();
    }

    public String getStructureId() {
        return structure_id;
    }

    public String getSubjectId() {
        return subject_id;
    }

    public String getStartDate() {
        return start_date;
    }

    public String getEndDate() {
        return end_date;
    }

    public Boolean getAttendance() {
        return attendance;
    }

    public Boolean isRecursive() {
        return is_recursive;
    }

    public Boolean isEveryTwoWeeks() {
        return is_every_two_weeks;
    }

    @Override
    public String getComment() {
        return comment;
    }

    public List<String> getListStudentId() {
        return student_id;
    }

    public String getStartDateRecursive() {
        return startDateRecursive;
    }

    public String getEndDateRecursive() {
        return endDateRecursive;
    }

    public List<String> getDayOfWeeks() {
        return day_of_weeks;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}