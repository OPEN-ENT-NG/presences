package fr.openent.massmailing.model.Mailing;

import io.vertx.core.json.JsonObject;

public class MailingEvent implements Cloneable {

    private Integer id;
    private Integer mailingId;
    private String eventId;
    private String eventType;

    public MailingEvent(JsonObject mailingEvent) {
        this.id = mailingEvent.getInteger("id", null);
        this.mailingId = mailingEvent.getInteger("mailing_id", null);
        this.eventId = mailingEvent.getString("event_id", null);
        this.eventType = mailingEvent.getString("event_type", null);
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("id", this.id)
                .put("mailing_id", this.mailingId)
                .put("event_id", this.eventId)
                .put("event_type", this.eventType);
    }

    @Override
    public MailingEvent clone() {
        try {
            return (MailingEvent) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMailingId() {
        return mailingId;
    }

    public void setMailingId(Integer mailingId) {
        this.mailingId = mailingId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}