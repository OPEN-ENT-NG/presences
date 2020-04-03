package fr.openent.presences.model.Exemption;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ExemptionView extends Exemption {
    private Integer exemption_id;
    private Integer exemption_recursive_id;
    private String student_id;
    private Integer recursive_id;
    private JsonArray day_of_week;
    private String type; // should be ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
    private JsonObject subject;
    private JsonObject student;

    public ExemptionView(JsonObject exemptionView) {
        this.exemption_id = exemptionView.getInteger("exemption_id", null);
        this.exemption_recursive_id = exemptionView.getInteger("exemption_recursive_id", exemptionView.getInteger("id"));
        this.structure_id = exemptionView.getString("structure_id", null);
        this.start_date = exemptionView.getString("start_date", null);
        this.end_date = exemptionView.getString("end_date", null);
        this.student_id = exemptionView.getString("student_id", null);
        this.comment = exemptionView.getString("comment", null);
        this.subject_id = exemptionView.getString("subject_id", null);
        this.recursive_id = exemptionView.getInteger("recursive_id", null);
        this.day_of_week = new JsonArray();
        if (exemptionView.getJsonArray("day_of_week") != null && !exemptionView.getJsonArray("day_of_week").isEmpty()) {
            for (int i = 0; i < exemptionView.getJsonArray("day_of_week").size(); i++) {
                this.day_of_week.add(exemptionView.getJsonArray("day_of_week").getJsonArray(i).getString(1));
            }
        }
        this.attendance = exemptionView.getBoolean("attendance", false);
        this.is_every_two_weeks = exemptionView.getBoolean("is_every_two_weeks", false);
        this.type = exemptionView.getString("type", null);
        this.subject = exemptionView.getJsonObject("subject", new JsonObject());
        this.student = exemptionView.getJsonObject("student", new JsonObject());
    }

    @Override
    public JsonObject toJsonObject() {
        return new JsonObject()
                .put("exemption_id", this.exemption_id)
                .put("exemption_recursive_id", this.exemption_recursive_id)
                .put("structure_id", this.structure_id)
                .put("start_date", this.start_date)
                .put("end_date", this.end_date)
                .put("student_id", this.student_id)
                .put("comment", this.comment)
                .put("subject_id", this.subject_id)
                .put("recursive_id", this.recursive_id)
                .put("day_of_week", this.day_of_week)
                .put("attendance", this.attendance)
                .put("is_every_two_weeks", this.is_every_two_weeks)
                .put("type", this.type)
                .put("subject", this.subject)
                .put("student", this.student);
    }

    public Integer getExemptionId() {
        return exemption_id;
    }

    public void setExemptionId(Integer exemption_id) {
        this.exemption_id = exemption_id;
    }

    public Integer getExemptionRecursiveId() {
        return exemption_recursive_id;
    }

    public void setExemptionRecursiveId(Integer exemption_recursive_id) {
        this.exemption_recursive_id = exemption_recursive_id;
    }

    public String getStructureId() {
        return structure_id;
    }

    public void setStructureId(String structure_id) {
        this.structure_id = structure_id;
    }

    public String getStartDate() {
        return start_date;
    }

    public void setStartDate(String start_date) {
        this.start_date = start_date;
    }

    public String getEndDate() {
        return end_date;
    }

    public void setEndDate(String end_date) {
        this.end_date = end_date;
    }

    public String getStudentId() {
        return student_id;
    }

    public void setStudentId(String student_id) {
        this.student_id = student_id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSubjectId() {
        return subject_id;
    }

    public void setSubjectId(String subject_id) {
        this.subject_id = subject_id;
    }

    public Integer getRecursiveId() {
        return recursive_id;
    }

    public void setRecursiveId(Integer recursive_id) {
        this.recursive_id = recursive_id;
    }

    public JsonArray getDayOfWeek() {
        return day_of_week;
    }

    public void setDayOfWeek(JsonArray day_of_week) {
        this.day_of_week = day_of_week;
    }

    public Boolean getAttendance() {
        return attendance;
    }

    public void setAttendance(Boolean attendance) {
        this.attendance = attendance;
    }

    public Boolean isEveryTwoWeeks() {
        return is_every_two_weeks;
    }

    public void setEveryTwoWeeks(Boolean is_every_two_weeks) {
        this.is_every_two_weeks = is_every_two_weeks;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JsonObject getSubject() {
        return subject;
    }

    public void setSubject(JsonObject subject) {
        this.subject = subject;
    }

    public JsonObject getStudent() {
        return student;

    }

    public void setStudent(JsonObject student) {
        this.student = student;
    }
}