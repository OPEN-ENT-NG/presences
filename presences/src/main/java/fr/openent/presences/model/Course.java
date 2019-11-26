package fr.openent.presences.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Course implements Cloneable {
    public static List<String> MANDATORY_ATTRIBUTE = new ArrayList<>();

    private String _id;
    private String structureId;
    private String subjectId;
    private JsonArray classes;
    private JsonArray groups;
    private JsonArray roomLabels;
    private Integer dayOfWeek;
    private Boolean manual;
    private String updated;
    private String lastUser;
    private String startDate;
    private String endDate;
    private String color;
    private String subjectName;
    private JsonArray teachers;
    private Integer registerId;
    private Integer registerStateId;
    private Boolean notified;
    private Boolean splitSlot;

    public Course(JsonObject course, List<String> mandatoryAttributes) {
        for (String attribute : mandatoryAttributes) {
            if (!course.containsKey(attribute) || course.getValue(attribute) == null) {
                throw new IllegalArgumentException("[Presences@CourseModel] Mandatory attribute not present " + attribute);
            }
        }
        this._id = course.getString("_id", "");
        this.structureId = course.getString("structureId", "");
        this.subjectId = course.getString("subjectId", "");
        this.classes = course.getJsonArray("classes", new JsonArray());
        this.groups = course.getJsonArray("groups", new JsonArray());
        this.roomLabels = course.getJsonArray("roomLabels", new JsonArray());
        this.dayOfWeek = course.getInteger("dayOfWeek", null);
        this.manual = course.getBoolean("manual", null);
        this.updated = course.getString("updated", "");
        this.lastUser = course.getString("lastUser", "");
        this.startDate = course.getString("startDate", "");
        this.endDate = course.getString("endDate", "");
        this.color = course.getString("color", "");
        this.subjectName = course.getString("subjectName", "");
        this.teachers = course.getJsonArray("teachers", new JsonArray());
        this.registerId = course.getInteger("register_id", null);
        this.registerStateId = course.getInteger("register_state_id", null);
        this.notified = course.getBoolean("notified", null);
        this.splitSlot = course.getBoolean("split_slot", false);
    }

    @Override
    public Course clone() {
        try {
            return (Course) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }

    public JsonObject toJSON() {
        JsonObject thisJsonObject = new JsonObject()
                .put("_id", this._id)
                .put("structureId", this.structureId)
                .put("subjectId", this.subjectId)
                .put("classes", this.classes)
                .put("groups", this.groups)
                .put("roomLabels", this.roomLabels)
                .put("dayOfWeek", this.dayOfWeek)
                .put("manual", this.manual)
                .put("updated", this.updated)
                .put("lastUser", this.lastUser)
                .put("startDate", this.startDate)
                .put("endDate", this.endDate)
                .put("color", this.color)
                .put("subjectName", this.subjectName)
                .put("teachers", this.teachers)
                .put("split_slot", this.splitSlot);
        if (this.registerId != null && this.registerStateId != null && this.notified != null) {
            thisJsonObject
                    .put("register_id", this.registerId)
                    .put("state_id", this.registerStateId)
                    .put("notified", this.notified);
        }
        return thisJsonObject;
    }

    public String getId() {
        return _id;
    }

    public void setId(String _id) {
        this._id = _id;
    }

    public String getStructureId() {
        return structureId;
    }

    public void setStructureId(String structureId) {
        this.structureId = structureId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public JsonArray getClasses() {
        return classes;
    }

    public void setClasses(JsonArray classes) {
        this.classes = classes;
    }

    public JsonArray getGroups() {
        return groups;
    }

    public void setGroups(JsonArray groups) {
        this.groups = groups;
    }

    public JsonArray getRoomLabels() {
        return roomLabels;
    }

    public void setRoomLabels(JsonArray roomLabels) {
        this.roomLabels = roomLabels;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Boolean getManual() {
        return manual;
    }

    public void setManual(Boolean manual) {
        this.manual = manual;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public String getLastUser() {
        return lastUser;
    }

    public void setLastUser(String lastUser) {
        this.lastUser = lastUser;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public JsonArray getTeachers() {
        return teachers;
    }

    public void setTeachers(JsonArray teachers) {
        this.teachers = teachers;
    }

    public Integer getRegisterId() {
        return registerId;
    }

    public void setRegisterId(Integer registerId) {
        this.registerId = registerId;
    }

    public Integer getRegisterStateId() {
        return registerStateId;
    }

    public void setRegisterStateId(Integer registerStateId) {
        this.registerStateId = registerStateId;
    }

    public Boolean getNotified() {
        return notified;
    }

    public void setNotified(Boolean notified) {
        this.notified = notified;
    }

    public Boolean getSplitSlot() {
        return splitSlot;
    }

    public void setSplitSlot(Boolean splitSlot) {
        this.splitSlot = splitSlot;
    }
}