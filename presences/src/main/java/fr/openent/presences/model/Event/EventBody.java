package fr.openent.presences.model.Event;

import io.vertx.core.json.JsonObject;

public class EventBody {

    private String start_date;
    private String end_date;
    private String comment;
    private Boolean counsellor_input;
    private String student_id;
    private Integer register_id;
    private Integer type_id;
    private Integer reason_id;
    private String owner;
    private String created;
    private Boolean counsellor_regularisation;
    private Boolean followed;
    private Boolean massmailed;

    public EventBody(JsonObject eventBody) {
        this.start_date = eventBody.getString("start_date", null);
        this.end_date = eventBody.getString("end_date", null);
        this.comment = eventBody.getString("comment", null);
        this.counsellor_input = eventBody.getBoolean("counsellor_input", null);
        this.student_id = eventBody.getString("student_id", null);
        this.register_id = eventBody.getInteger("register_id", null);
        this.type_id = eventBody.getInteger("type_id", null);
        this.reason_id = eventBody.getInteger("reason_id", null);
        this.owner = eventBody.getString("owner", null);
        this.created = eventBody.getString("created", null);
        this.counsellor_regularisation = eventBody.getBoolean("counsellor_regularisation", null);
        this.followed = eventBody.getBoolean("followed", false);
        this.massmailed = eventBody.getBoolean("massmailed", null);
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("start_date", this.start_date)
                .put("end_date", this.end_date)
                .put("comment", this.comment)
                .put("counsellor_input", this.counsellor_input)
                .put("student_id", this.student_id)
                .put("register_id", this.register_id)
                .put("type_id", this.type_id)
                .put("reason_id", this.reason_id)
                .put("owner", this.owner)
                .put("created", this.created)
                .put("counsellor_regularisation", this.counsellor_regularisation)
                .put("followed", this.followed)
                .put("massmailed", this.massmailed);
    }

    public String getStartDate() {
        return start_date;
    }

    public void setStartDate(String start_date) {
        this.start_date = start_date;
    }

    public String getEndDate() {
        return end_date;
    }

    public void setEndDate(String end_date) {
        this.end_date = end_date;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean isCounsellorInput() {
        return counsellor_input;
    }

    public void setCounsellorInput(Boolean counsellor_input) {
        this.counsellor_input = counsellor_input;
    }

    public String getStudentId() {
        return student_id;
    }

    public void setStudentId(String student_id) {
        this.student_id = student_id;
    }

    public Integer getRegisterId() {
        return register_id;
    }

    public void setRegisterId(Integer register_id) {
        this.register_id = register_id;
    }

    public Integer getTypeId() {
        return type_id;
    }

    public void setTypeId(Integer type_id) {
        this.type_id = type_id;
    }

    public Integer getReasonId() {
        return reason_id;
    }

    public void setReasonId(Integer reason_id) {
        this.reason_id = reason_id;
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

    public Boolean isCounsellorRegularisation() {
        return counsellor_regularisation;
    }

    public void setCounsellorRegularisation(Boolean counsellor_regularisation) {
        this.counsellor_regularisation = counsellor_regularisation;
    }

    public Boolean isFollowed() {
        return followed;
    }

    public void setFollowed(Boolean followed) {
        this.followed = followed;
    }

    public Boolean isMassmailed() {
        return massmailed;
    }

    public void setMassmailed(Boolean massmailed) {
        this.massmailed = massmailed;
    }
}

