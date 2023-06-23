package fr.openent.presences.model;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.EventTypeEnum;
import fr.openent.presences.model.Person.Student;
import fr.openent.presences.model.Person.User;
import io.vertx.core.json.JsonObject;

public class EventModel implements IModel<EventModel> {
    private Integer id;
    private String startDate;
    private String endDate;
    private String comment;
    private Boolean counsellorInput;
    private Boolean counsellorRegularisation;
    private Boolean followed;
    private Boolean massmailed;
    private Student student;
    private Integer registerId;
    private EventTypeEnum type;
    private ReasonModel reason;
    private User owner;
    private String created;

    public EventModel() {
    }

    public EventModel(JsonObject event) {
        this.id = event.getInteger(Field.ID);
        this.startDate = event.getString(Field.START_DATE);
        this.endDate = event.getString(Field.END_DATE);
        this.comment = event.getString(Field.COMMENT);
        this.counsellorInput = event.getBoolean(Field.COUNSELLOR_INPUT);
        this.student = new Student(event.getString(Field.STUDENT_ID));
        this.registerId = event.getInteger(Field.REGISTER_ID);
        this.type = EventTypeEnum.getEventType(event.getInteger(Field.TYPE_ID, 1));
        this.reason = new ReasonModel().setId(event.getInteger(Field.REASON_ID));
        this.owner = event.getValue(Field.OWNER) instanceof String ? new User(event.getString(Field.OWNER)) :
                new User(event.getJsonObject(Field.OWNER, new JsonObject()));
        this.created = event.getString(Field.CREATED);
        this.counsellorRegularisation = event.getBoolean(Field.COUNSELLOR_REGULARISATION);
        this.followed = event.getBoolean(Field.FOLLOWED);
        this.massmailed = event.getBoolean(Field.MASSMAILED);
    }

    public Integer getId() {
        return id;
    }

    public EventModel setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public EventModel setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public EventModel setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public EventModel setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public Boolean getCounsellorInput() {
        return counsellorInput;
    }

    public EventModel setCounsellorInput(Boolean counsellorInput) {
        this.counsellorInput = counsellorInput;
        return this;
    }

    public Boolean getCounsellorRegularisation() {
        return counsellorRegularisation;
    }

    public EventModel setCounsellorRegularisation(Boolean counsellorRegularisation) {
        this.counsellorRegularisation = counsellorRegularisation;
        return this;
    }

    public Boolean getFollowed() {
        return followed;
    }

    public EventModel setFollowed(Boolean followed) {
        this.followed = followed;
        return this;
    }

    public Boolean getMassmailed() {
        return massmailed;
    }

    public EventModel setMassmailed(Boolean massmailed) {
        this.massmailed = massmailed;
        return this;
    }

    public Student getStudent() {
        return student;
    }

    public EventModel setStudentId(Student student) {
        this.student = student;
        return this;
    }

    public Integer getRegisterId() {
        return registerId;
    }

    public EventModel setRegisterId(Integer registerId) {
        this.registerId = registerId;
        return this;
    }

    public EventTypeEnum getType() {
        return type;
    }

    public EventModel setType(EventTypeEnum type) {
        this.type = type;
        return this;
    }

    public ReasonModel getReason() {
        return reason;
    }

    public EventModel setReasonId(ReasonModel reason) {
        this.reason = reason;
        return this;
    }

    public User getOwner() {
        return owner;
    }

    public EventModel setOwner(User owner) {
        this.owner = owner;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public EventModel setCreated(String created) {
        this.created = created;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(Field.ID, this.id)
                .put(Field.START_DATE, this.startDate)
                .put(Field.END_DATE, this.endDate)
                .put(Field.COMMENT, this.comment)
                .put(Field.COUNSELLOR_INPUT, this.counsellorInput)
                .put(Field.STUDENT_ID, (this.student == null) ? null : this.student.getId())
                .put(Field.REGISTER_ID, this.registerId)
                .put(Field.TYPE_ID, (this.type == null ? null : this.type.getType()))
                .put(Field.REASON_ID, (this.reason == null) ? null : this.reason.getId())
                .put(Field.OWNER, (this.owner == null) ? null : this.owner.getId())
                .put(Field.CREATED, this.created)
                .put(Field.COUNSELLOR_REGULARISATION, this.counsellorRegularisation)
                .put(Field.FOLLOWED, this.followed)
                .put(Field.MASSMAILED, this.massmailed);
    }

    @Override
    public boolean validate() {
        return this.id != null
                && this.startDate != null && !this.startDate.isEmpty()
                && this.endDate != null && !this.endDate.isEmpty()
                && this.comment != null && !this.comment.isEmpty()
                && this.counsellorInput != null
                && this.student != null && this.student.getId() != null && !this.student.getId().isEmpty()
                && this.registerId != null
                && this.type != null
                && this.reason != null
                && this.owner != null
                && this.created != null && !this.created.isEmpty()
                && this.counsellorRegularisation != null
                && this.followed != null
                && this.massmailed != null;
    }

    public int compareStartDate(EventModel eventModel) {
        return this.startDate.compareTo(eventModel.startDate);
    }
}
