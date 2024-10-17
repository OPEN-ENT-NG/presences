package fr.openent.presences.model;

import fr.openent.presences.helper.EventHelper;
import fr.openent.presences.model.Event.Event;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Register {
    public static List<String> MANDATORY_ATTRIBUTE = new ArrayList<>();

    private Integer id;
    private String personnelId;
    private String courseId;
    private Integer stateId;
    private List<Event> events;
    private String studentId;
    private Integer proofId;
    private Boolean counsellorInput;
    private String owner;
    private String created;
    private String subjectId;
    private String startDate;
    private String endDate;
    private String structureId;
    private Boolean notified;
    private Boolean splitSlot;

    public Register(JsonObject register, List<String> mandatoryAttributes) {
        for (String attribute : mandatoryAttributes) {
            if (!register.containsKey(attribute) || register.getValue(attribute) == null) {
                throw new IllegalArgumentException("[Presences@SlotModel] mandatory attribute not present " + attribute);
            }
        }
        this.id = register.getInteger("id", null);
        this.personnelId = register.getString("personnel_id", null);
        this.courseId = register.getString("course_id", null);
        this.stateId = register.getInteger("state_id", null);
        this.events = EventHelper.getEventListFromJsonArray(register.getJsonArray("events", new JsonArray()), Event.MANDATORY_ATTRIBUTE);
        this.proofId = register.getInteger("proof_id", null);
        this.counsellorInput = register.getBoolean("counsellor_input", null);
        this.owner = register.getString("owner", null);
        this.created = register.getString("created", null);
        this.subjectId = register.getString("subject_id", null);
        this.startDate = register.getString("start_date", null);
        this.endDate = register.getString("end_date", null);
        this.structureId = register.getString("structure_id", null);
        this.notified = register.getBoolean("notified", null);
        this.splitSlot = register.getBoolean("split_slot", null);
    }

    public Register(JsonObject register) {
        this.studentId = register.getString("student_id", null);
        this.events = EventHelper.getEventListFromJsonArray(new JsonArray(register.getString("events", null)),
                Event.MANDATORY_ATTRIBUTE);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPersonnelId() {
        return personnelId;
    }

    public void setPersonnelId(String personnelId) {
        this.personnelId = personnelId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public Integer getStateId() {
        return stateId;
    }

    public void setStateId(Integer stateId) {
        this.stateId = stateId;
    }

    public Integer getProofId() {
        return proofId;
    }

    public void setProofId(Integer proofId) {
        this.proofId = proofId;
    }

    public Boolean isCounsellorInput() {
        return counsellorInput;
    }

    public void setCounsellorInput(Boolean counsellorInput) {
        this.counsellorInput = counsellorInput;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
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

    public String getStructureId() {
        return structureId;
    }

    public void setStructureId(String structureId) {
        this.structureId = structureId;
    }

    public Boolean isNotified() {
        return notified;
    }

    public void setNotified(Boolean notified) {
        this.notified = notified;
    }

    public Boolean isSplitSlot() {
        return splitSlot;
    }

    public void setSplitSlot(Boolean splitSlot) {
        this.splitSlot = splitSlot;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}
