package fr.openent.presences.model;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Absence implements Cloneable {
    public static List<String> MANDATORY_ATTRIBUTE = new ArrayList<>();

    private Integer id;
    private Boolean absence;
    private String studentId;
    private Integer reasonId;
    private String startDate;
    private String endDate;
    private Boolean counsellorRegularisation;
    private Boolean followed;

    public Absence(JsonObject absence, List<String> mandatoryAttributes) {
        for (String attribute : mandatoryAttributes) {
            if (!absence.containsKey(attribute) || absence.getValue(attribute) == null) {
                throw new IllegalArgumentException("[Presences@AbsenceModel] Mandatory attribute not present " + attribute);
            }
        }
        this.id = absence.getInteger("id", null);
        this.absence = null;
        this.studentId = absence.getString("student_id", "");
        this.reasonId = absence.getInteger("reason_id", null);
        this.startDate = absence.getString("start_date", "");
        this.endDate = absence.getString("end_date", "");
        this.counsellorRegularisation = absence.getBoolean("counsellor_regularisation", false);
        this.followed = absence.getBoolean("followed", false);
    }

    @Override
    public Absence clone() {
        try {
            return (Absence) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("id", this.id)
                .put("absence", this.absence)
                .put("student_id", this.studentId)
                .put("reason_id", this.reasonId)
                .put("start_date", this.startDate)
                .put("end_date", this.endDate)
                .put("counsellor_regularisation", this.counsellorRegularisation)
                .put("followed", this.followed)
                .put("type", "absence");
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Boolean isAbsence() {
        return absence;
    }

    public void setAbsence(Boolean absence) {
        this.absence = absence;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Integer getReasonId() {
        return reasonId;
    }

    public void setReasonId(Integer reasonId) {
        this.reasonId = reasonId;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Boolean isCounsellorRegularisation() {
        return counsellorRegularisation;
    }

    public void setCounsellorRegularisation(Boolean counsellorRegularisation) {
        this.counsellorRegularisation = counsellorRegularisation;
    }

    public Boolean isFollowed() {
        return followed;
    }

    public void setFollowed(Boolean followed) {
        this.followed = followed;
    }
}
