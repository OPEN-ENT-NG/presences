package fr.openent.massmailing.model.Mailing;

import fr.openent.presences.model.Person.Metadata;
import fr.openent.presences.model.Person.Student;
import fr.openent.presences.model.Person.User;
import io.vertx.core.json.JsonObject;

public class Mailing implements Cloneable {

    private Integer id;
    private Student student;
    private MailingEvent mailingEvent;
    private User recipient;
    private String structureId;
    private String type;
    private String content;
    private String fileId;
    private Metadata metadata;
    private String created;

    public Mailing(JsonObject mailing) {
        this.id = mailing.getInteger("id", null);
        this.student = new Student(mailing.getString("student_id", null));
        this.mailingEvent = new MailingEvent(new JsonObject(mailing.getString("mailing_event")));
        this.recipient = new User(mailing.getString("recipient_id", null), mailing.getString("recipient", null));
        this.structureId = mailing.getString("structure_id", null);
        this.type = mailing.getString("type", null);
        this.content = mailing.getString("content", null);
        this.fileId = mailing.getString("file_id", null);
        this.metadata = new Metadata(mailing.getString("metadata"));
        this.created = mailing.getString("created", null);
    }

    public Mailing(Integer id) {
        this.id = id;
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("id", this.id)
                .put("student", this.student.toJSON())
                .put("mailing_event", this.mailingEvent.toJSON())
                .put("recipient", this.recipient.toJSON())
                .put("structure_id", this.structureId)
                .put("type", this.type)
                .put("content", this.content)
                .put("file_id", this.fileId)
                .put("metadata", this.metadata.toJsonObject())
                .put("created", created);
    }

    @Override
    public Mailing clone() {
        try {
            return (Mailing) super.clone();
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

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public MailingEvent getMailingEvent() {
        return mailingEvent;
    }

    public void setMailingEvent(MailingEvent mailingEvent) {
        this.mailingEvent = mailingEvent;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public String getStructureId() {
        return structureId;
    }

    public void setStructureId(String structureId) {
        this.structureId = structureId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }
}