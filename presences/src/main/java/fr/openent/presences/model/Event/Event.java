package fr.openent.presences.model.Event;

import fr.openent.presences.model.Person.Student;
import fr.openent.presences.model.Reason;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Event {

    public static List<String> MANDATORY_ATTRIBUTE = new ArrayList<>();

    private Integer id;
    private String startDate;
    private String endDate;

    private String comment;
    private Boolean counsellorInput;
    private Student student;
    private Integer registerId;
    private EventType eventType;
    private Reason reason;
    private String owner;
    private String created;
    private Boolean counsellorRegularisation;
    private Boolean massmailed;

    public Event(JsonObject event, List<String> mandatoryAttributes) {
        for (String attribute : mandatoryAttributes) {
            if (!event.containsKey(attribute) || event.getValue(attribute) == null) {
                throw new IllegalArgumentException("[Presences@EventModel] mandatory attribute not present " + attribute);
            }
        }
        this.id = event.getInteger("id", null);
        this.startDate = event.getString("start_date", null);
        this.endDate = event.getString("end_date", null);
        this.comment = event.getString("comment", null);
        this.counsellorInput = event.getBoolean("counsellor_input", false);
        this.student = new Student(event.getString("student_id", null));
        this.registerId = event.getInteger("register_id", null);
        if (event.getString("event_type") != null) {
            this.eventType = new EventType(new JsonObject(event.getString("event_type", null)), EventType.MANDATORY_ATTRIBUTE);
        } else {
            this.eventType = new EventType(event.getJsonObject("event_type", new JsonObject()), EventType.MANDATORY_ATTRIBUTE);
        }
        this.reason = new Reason(event.getInteger("reason_id", null));
        this.owner = event.getString("owner", null);
        this.created = event.getString("created", null);
        this.counsellorRegularisation = event.getBoolean("counsellor_regularisation", false);
        this.massmailed = event.getBoolean("massmailed", false);

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

}
