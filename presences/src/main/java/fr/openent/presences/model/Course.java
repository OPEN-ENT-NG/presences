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
    private String exceptionnal;
    private JsonArray groups;
    private JsonArray roomLabels;
    private JsonArray events;
    private List<Course> splitCourses;
    private Boolean exempted;
    private JsonObject exemption;
    private JsonObject incident;
    private Integer dayOfWeek;
    private Boolean manual;
    private Boolean locked;
    private String updated;
    private String lastUser;
    private String startDate;
    private String endDate;
    private String startCourse;
    private String endCourse;
    private String startMomentDate;
    private String startMomentTime;
    private String endMomentDate;
    private String endMomentTime;
    private Boolean isRecurrent;
    private String color;
    private Boolean isPeriodic;
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
        this.exceptionnal = course.getString("exceptionnal", "");
        this.groups = course.getJsonArray("groups", new JsonArray());
        this.roomLabels = course.getJsonArray("roomLabels", new JsonArray());
        this.events = course.getJsonArray("events", new JsonArray());
        this.exempted = course.getBoolean("exempted", null);
        this.exemption = course.getJsonObject("exemption", null);
        this.incident = course.getJsonObject("incident", null);
        this.dayOfWeek = course.getInteger("dayOfWeek", null);
        this.manual = course.getBoolean("manual", null);
        this.locked = course.getBoolean("locked", null);
        this.updated = course.getString("updated", "");
        this.lastUser = course.getString("lastUser", "");
        this.startDate = course.getString("startDate", "");
        this.endDate = course.getString("endDate", "");
        this.startCourse = course.getString("startCourse", "");
        this.endCourse = course.getString("endCourse", "");
        this.startMomentDate = course.getString("startMomentDate", "");
        this.startMomentTime = course.getString("startMomentTime", "");
        this.endMomentDate = course.getString("endMomentDate", "");
        this.endMomentTime = course.getString("endMomentTime", "");
        this.isRecurrent = course.getBoolean("is_recurrent", null);
        this.color = course.getString("color", "");
        this.isPeriodic = course.getBoolean("is_periodic", null);
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
                .put("exceptionnal", this.exceptionnal)
                .put("groups", this.groups)
                .put("roomLabels", this.roomLabels)
                .put("events", this.events)
                .put("exemption", this.events)
                .put("incident", this.events)
                .put("dayOfWeek", this.dayOfWeek)
                .put("manual", this.manual)
                .put("locked", this.locked)
                .put("updated", this.updated)
                .put("lastUser", this.lastUser)
                .put("startDate", this.startDate)
                .put("endDate", this.endDate)
                .put("startCourse", this.startCourse)
                .put("endCourse", this.endCourse)
                .put("startMomentDate", this.startMomentDate)
                .put("startMomentTime", this.startMomentTime)
                .put("endMomentDate", this.endMomentDate)
                .put("endMomentTime", this.endMomentTime)
                .put("is_recurrent", this.isRecurrent)
                .put("color", this.color)
                .put("is_periodic", this.isPeriodic)
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
        return classes != null ? classes : new JsonArray();
    }

    public void setClasses(JsonArray classes) {
        this.classes = classes;
    }

    public JsonArray getGroups() {
        return groups != null ? groups : new JsonArray();
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

    public Boolean isManual() {
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
        return subjectName != null ? subjectName : "";
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

    public String getStartCourse() {
        return startCourse;
    }

    public void setStartCourse(String startCourse) {
        this.startCourse = startCourse;
    }

    public String getEndCourse() {
        return endCourse;
    }

    public void setEndCourse(String endCourse) {
        this.endCourse = endCourse;
    }

    public Boolean isRecurrent() {
        return isRecurrent;
    }

    public void setRecurrent(Boolean recurrent) {
        isRecurrent = recurrent;
    }

    public Boolean isPeriodic() {
        return isPeriodic;
    }

    public void setPeriodic(Boolean periodic) {
        isPeriodic = periodic;
    }

    public String getExceptionnal() {
        return exceptionnal;
    }

    public void setExceptionnal(String exceptionnal) {
        this.exceptionnal = exceptionnal;
    }

    public Boolean isLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public String getStartMomentDate() {
        return startMomentDate;
    }

    public void setStartMomentDate(String startMomentDate) {
        this.startMomentDate = startMomentDate;
    }

    public String getStartMomentTime() {
        return startMomentTime;
    }

    public void setStartMomentTime(String startMomentTime) {
        this.startMomentTime = startMomentTime;
    }

    public String getEndMomentDate() {
        return endMomentDate;
    }

    public void setEndMomentDate(String endMomentDate) {
        this.endMomentDate = endMomentDate;
    }

    public String getEndMomentTime() {
        return endMomentTime;
    }

    public void setEndMomentTime(String endMomentTime) {
        this.endMomentTime = endMomentTime;
    }

    public JsonArray getEvents() {
        return events;
    }

    public void setEvents(JsonArray events) {
        this.events = events;
    }

    public Boolean isExempted() {
        return exempted;
    }

    public void setExempted(Boolean exempted) {
        this.exempted = exempted;
    }

    public JsonObject getExemption() {
        return exemption;
    }

    public void setExemption(JsonObject exemption) {
        this.exemption = exemption;
    }

    public JsonObject getIncident() {
        return incident;
    }

    public void setIncident(JsonObject incident) {
        this.incident = incident;
    }

    public List<Course> getSplitCourses() {
        return splitCourses;
    }

    public void setSplitCourses(List<Course> splitCourses) {
        this.splitCourses = splitCourses;
    }
}