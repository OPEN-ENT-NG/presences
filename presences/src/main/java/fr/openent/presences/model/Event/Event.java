package fr.openent.presences.model.Event;

import fr.openent.presences.model.Person.Student;
import fr.openent.presences.model.Person.User;
import fr.openent.presences.model.Reason;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Event {

    public static List<String> MANDATORY_ATTRIBUTE = new ArrayList<>();

    private Integer id;
    private String startDate;
    private String endDate;
    private String date;

    private String comment;
    private Boolean counsellorInput;
    private Student student;
    private Integer registerId;
    private EventType eventType;
    private Reason reason;
    private User owner;
    private String created;
    private Boolean followed;
    private Boolean counsellorRegularisation;
    private Boolean massmailed;
    private String type;
    private Boolean isExclude;
    private String actionAbbreviation;

    public Event(JsonObject event, List<String> mandatoryAttributes) {
        for (String attribute : mandatoryAttributes) {
            if (!event.containsKey(attribute) || event.getValue(attribute) == null) {
                throw new IllegalArgumentException("[Presences@EventModel] mandatory attribute not present " + attribute);
            }
        }
        this.id = event.getInteger("id", null);
        this.startDate = event.getString("start_date", null);
        this.endDate = event.getString("end_date", null);
        this.date = event.getString("date", null);
        this.comment = event.getString("comment", null);
        this.counsellorInput = event.getBoolean("counsellor_input", false);
        this.student = new Student(event.getString("student_id", null));
        this.registerId = event.getInteger("register_id", null);
        if (event.getLong("type_id") != null) {
            this.eventType = new EventType(event.getInteger("type_id", null));
        } else {
            this.eventType = new EventType(event.getJsonObject("event_type", new JsonObject()), EventType.MANDATORY_ATTRIBUTE);
        }
        this.reason = new Reason(event.getInteger("reason_id", null));
        this.owner = event.getValue("owner") instanceof String ? new User(event.getString("owner")) :
                new User(event.getJsonObject("owner", new JsonObject()));
        this.created = event.getString("created", null);
        this.counsellorRegularisation = event.getBoolean("counsellor_regularisation", false);
        this.followed = event.getBoolean("followed", false);
        this.massmailed = event.getBoolean("massmailed", false);
        this.type = event.getString("type", null);
        this.isExclude = event.getBoolean("exclude", null);
        this.actionAbbreviation = event.getString("action_abbreviation", null);
    }

    public Event() {

    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("id", this.id)
                .put("start_date", this.startDate)
                .put("end_date", this.endDate)
                .put("date", this.date)
                .put("comment", this.comment)
                .put("counsellor_input", this.counsellorInput)
                .put("student", this.student.toJSON())
                .put("register_id", this.registerId)
                .put("event_type", this.eventType.toJSON())
                .put("reason", this.reason.toJSON())
                .put("owner", this.owner.toJSON())
                .put("created", this.created)
                .put("counsellor_regularisation", this.counsellorRegularisation)
                .put("followed", this.followed)
                .put("massmailed", this.massmailed)
                .put("type", this.type)
                .put("action_abbreviation", this.actionAbbreviation)
                .put("exclude", this.isExclude);
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isCounsellorInput() {
        return counsellorInput;
    }

    public void setCounsellorInput(boolean counsellorInput) {
        this.counsellorInput = counsellorInput;
    }

    public boolean isFollowed() {
        return followed;
    }

    public void setFollowed(boolean followed) {
        this.followed = followed;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Integer getRegisterId() {
        return registerId;
    }

    public void setRegisterId(Integer registerId) {
        this.registerId = registerId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Reason getReason() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public boolean isCounsellorRegularisation() {
        return counsellorRegularisation;
    }

    public void setCounsellorRegularisation(boolean counsellorRegularisation) {
        this.counsellorRegularisation = counsellorRegularisation;
    }

    public boolean isMassmailed() {
        return massmailed;
    }

    public void setMassmailed(boolean massmailed) {
        this.massmailed = massmailed;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean isExclude() {
        return isExclude;
    }

    public void setExclude(Boolean exclude) {
        isExclude = exclude;
    }

    public void setActionAbbreviation(String abbreviation) {
        this.actionAbbreviation = abbreviation;
    }

    public String getActionAbbreviation() {
        return this.actionAbbreviation;
    }
}
