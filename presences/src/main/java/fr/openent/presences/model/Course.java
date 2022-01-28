package fr.openent.presences.model;

import fr.openent.presences.core.constants.*;
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
    private JsonArray punishments;
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
    private Long timestamp;
    private Subject subject;
    private Boolean isOpenedByPersonnel;
    private boolean allowRegister;

    public Course(JsonObject course, List<String> mandatoryAttributes) {
        for (String attribute : mandatoryAttributes) {
            if (!course.containsKey(attribute) || course.getValue(attribute) == null) {
                throw new IllegalArgumentException("[Presences@CourseModel] Mandatory attribute not present " + attribute);
            }
        }
        this._id = course.getString(Field._ID, "");
        this.structureId = course.getString(Field.STRUCTUREID, "");
        this.subjectId = course.getString(Field.SUBJECTID, "");
        this.classes = course.getJsonArray(Field.CLASSES, new JsonArray());
        this.exceptionnal = course.getString(Field.EXCEPTIONNAL, "");
        this.groups = course.getJsonArray(Field.GROUPS, new JsonArray());
        this.roomLabels = course.getJsonArray(Field.ROOMLABELS, new JsonArray());
        this.events = course.getJsonArray(Field.EVENTS, new JsonArray());
        this.exempted = course.getBoolean(Field.EXEMPTED, null);
        this.exemption = course.getJsonObject(Field.EXEMPTION, null);
        this.incident = course.getJsonObject(Field.INCIDENT, null);
        this.punishments = course.getJsonArray(Field.PUNISHMENTS, new JsonArray());
        this.dayOfWeek = course.getInteger(Field.DAYOFWEEK, null);
        this.manual = course.getBoolean(Field.MANUAL, null);
        this.locked = course.getBoolean(Field.LOCKED, null);
        this.updated = course.getString(Field.UPDATED, "");
        this.lastUser = course.getString(Field.LASTUSER, "");
        this.startDate = course.getString(Field.STARTDATE, "");
        this.endDate = course.getString(Field.ENDDATE, "");
        this.startCourse = course.getString(Field.STARTCOURSE, "");
        this.endCourse = course.getString(Field.ENDCOURSE, "");
        this.startMomentDate = course.getString(Field.STARTMOMENTDATE, "");
        this.startMomentTime = course.getString(Field.STARTMOMENTTIME, "");
        this.endMomentDate = course.getString(Field.ENDMOMENTDATE, "");
        this.endMomentTime = course.getString(Field.ENDMOMENTTIME, "");
        this.isRecurrent = course.getBoolean(Field.IS_RECURRENT, null);
        this.color = course.getString(Field.COLOR, "");
        this.isPeriodic = course.getBoolean(Field.IS_PERIODIC, null);
        this.subjectName = course.getString(Field.SUBJECTNAME, "");
        this.teachers = course.getJsonArray(Field.TEACHERS, course.getJsonArray(Field.TEACHERIDS, new JsonArray()));
        this.registerId = course.getInteger(Field.REGISTER_ID, null);
        this.registerStateId = course.getInteger(Field.REGISTER_STATE_ID, null);
        this.notified = course.getBoolean(Field.NOTIFIED, null);
        this.splitSlot = course.getBoolean(Field.SPLIT_SLOT, false);
        this.timestamp = course.getLong(Field.TIMESTAMP);
        this.subject = new Subject(course.getJsonObject(Field.SUBJECT, new JsonObject()));
        this.isOpenedByPersonnel = course.getBoolean(Field.ISOPENEDBYPERSONNEL, null);
        this.allowRegister = course.getBoolean(Field.ALLOWREGISTER, true);
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
                .put(Field._ID, this._id)
                .put(Field.STRUCTUREID, this.getStructureId())
                .put(Field.SUBJECTID, this.getSubjectId())
                .put(Field.CLASSES, this.getClasses())
                .put(Field.EXCEPTIONNAL, this.getExceptionnal())
                .put(Field.GROUPS, this.getGroups())
                .put(Field.ROOMLABELS, this.getRoomLabels())
                .put(Field.EVENTS, this.getEvents())
                .put(Field.EXEMPTED, this.isExempted())
                .put(Field.EXEMPTION, this.getExemption())
                .put(Field.INCIDENT, this.getIncident())
                .put(Field.DAYOFWEEK, this.getDayOfWeek())
                .put(Field.MANUAL, this.isManual())
                .put(Field.LOCKED, this.isLocked())
                .put(Field.UPDATED, this.getUpdated())
                .put(Field.LASTUSER, this.getLastUser())
                .put(Field.STARTDATE, this.getStartDate())
                .put(Field.ENDDATE, this.getEndDate())
                .put(Field.STARTCOURSE, this.getStartCourse())
                .put(Field.ENDCOURSE, this.getEndCourse())
                .put(Field.STARTMOMENTDATE, this.getStartMomentDate())
                .put(Field.STARTMOMENTTIME, this.getStartMomentTime())
                .put(Field.ENDMOMENTDATE, this.getEndMomentDate())
                .put(Field.ENDMOMENTTIME, this.getEndMomentTime())
                .put(Field.IS_RECURRENT, this.isRecurrent())
                .put(Field.COLOR, this.getColor())
                .put(Field.IS_PERIODIC, this.isPeriodic())
                .put(Field.SUBJECTNAME, this.getSubjectName())
                .put(Field.TEACHERS, this.getTeachers())
                .put(Field.SPLIT_SLOT, this.isSplitSlot())
                .put(Field.SUBJECT, this.getSubject().toJSON())
                .put(Field.ISOPENEDBYPERSONNEL, this.getIsOpenedByPersonnel())
                .put(Field.ALLOWREGISTER, this.getAllowRegister());
        if (this.registerId != null && this.registerStateId != null && this.notified != null) {
            thisJsonObject
                    .put(Field.REGISTER_ID, this.registerId)
                    .put(Field.STATE_ID, this.registerStateId)
                    .put(Field.NOTIFIED, this.notified);
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
        return subjectId != null ? subjectId : "";
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

    public JsonArray getPunishments() {
        return punishments;
    }

    public void setPunishments(JsonArray punishments) {
        this.punishments = punishments;
    }

    public List<Course> getSplitCourses() {
        return splitCourses;
    }

    public void setSplitCourses(List<Course> splitCourses) {
        this.splitCourses = splitCourses;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String mapId() {
        return String.format("%s$%s$%s", this.getId(), this.getStartDate(), this.getEndDate());
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Boolean getIsOpenedByPersonnel() {
        return this.isOpenedByPersonnel;
    }

    public void setIsOpenedByPersonnel(Boolean isOpenedByPersonnel) {
        this.isOpenedByPersonnel = isOpenedByPersonnel;
    }

    public boolean getAllowRegister() {
        return this.allowRegister;
    }

    public void setAllowRegister(boolean allowRegister) {
        this.allowRegister = allowRegister;
    }

}