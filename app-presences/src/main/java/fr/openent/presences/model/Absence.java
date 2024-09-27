package fr.openent.presences.model;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.Person.Student;
import fr.openent.presences.model.Person.User;
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
    private String regularized;
    private Boolean followed;
    private Reason reason;
    private Student student;
    private User owner;

    public Absence(JsonObject absence, List<String> mandatoryAttributes) {
        for (String attribute : mandatoryAttributes) {
            if (!absence.containsKey(attribute) || absence.getValue(attribute) == null) {
                throw new IllegalArgumentException("[Presences@AbsenceModel] Mandatory attribute not present " + attribute);
            }
        }
        this.id = absence.getInteger(Field.ID, null);
        this.absence = null;
        this.studentId = absence.getString(Field.STUDENT_ID, "");
        this.student = absence.getValue(Field.STUDENT) instanceof String ? new Student(absence.getString(Field.STUDENT)) :
                new Student(absence.getJsonObject(Field.STUDENT, new JsonObject()));
        this.owner = absence.getValue(Field.OWNER) instanceof String ? new User(absence.getString(Field.OWNER)) :
                new User(absence.getJsonObject(Field.OWNER, new JsonObject()));
        this.reasonId = absence.getInteger(Field.REASON_ID, null);
        this.reason = new Reason(reasonId);
        this.startDate = absence.getString(Field.START_DATE, "");
        this.endDate = absence.getString(Field.END_DATE, "");
        this.counsellorRegularisation = absence.getBoolean(Field.COUNSELLOR_REGULARISATION, false);
        this.regularized = absence.getString(Field.REGULARIZED, null);
        this.followed = absence.getBoolean(Field.FOLLOWED, false);
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

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Integer getReasonId() {
        return reasonId;
    }

    public void setReasonId(Integer reasonId) {
        this.reasonId = reasonId;
    }

    public Reason getReason() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
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

    public String getRegularized() {
        return regularized;
    }

    public void setRegularized(String regularized) {
        this.regularized = regularized;
    }

    public Boolean isFollowed() {
        return followed;
    }

    public void setFollowed(Boolean followed) {
        this.followed = followed;
    }
}
