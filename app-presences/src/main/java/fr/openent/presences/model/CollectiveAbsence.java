package fr.openent.presences.model;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class CollectiveAbsence {

    private Long id;
    private String startDate;
    private String endDate;
    private String createdAt;
    private Boolean counsellorRegularisation;
    private Long reasonId;
    private String structureId;
    private String comment;
    private String ownerId;

    private List<Audience> audiences;

    public CollectiveAbsence(JsonObject collectiveAbsence) {
        this.id = collectiveAbsence.getLong("id", null);
        this.startDate = collectiveAbsence.getString("start_date", collectiveAbsence.getString("startDate", ""));
        this.endDate = collectiveAbsence.getString("end_date", collectiveAbsence.getString("endDate", ""));
        this.createdAt = collectiveAbsence.getString("created_at", collectiveAbsence.getString("createdAt", ""));
        this.counsellorRegularisation = collectiveAbsence.getBoolean("counsellor_regularisation", collectiveAbsence.getBoolean("counsellorRegularisation", false));
        this.reasonId = collectiveAbsence.getLong("reason_id", collectiveAbsence.getLong("reasonId", null));
        this.structureId = collectiveAbsence.getString("structure_id", collectiveAbsence.getString("structureId", null));
        this.comment = collectiveAbsence.getString("comment", "");
        this.ownerId = collectiveAbsence.getString("owner_id", collectiveAbsence.getString("ownerId", null));
        this.audiences = Audience.audiences(collectiveAbsence.getJsonArray("audiences", new JsonArray()));
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("id", this.id)
                .put("start_date", this.startDate)
                .put("end_date", this.endDate)
                .put("created_at", this.endDate)
                .put("counsellor_regularisation", this.counsellorRegularisation)
                .put("reason_id", this.reasonId)
                .put("structure_id", this.structureId)
                .put("comment", this.comment)
                .put("owner_id", this.ownerId);
    }

    public JsonObject toCamelJSON() {
        return new JsonObject()
                .put("id", this.id)
                .put("startDate", this.startDate)
                .put("endDate", this.endDate)
                .put("createdAt", this.endDate)
                .put("counsellorRegularisation", this.counsellorRegularisation)
                .put("reasonId", this.reasonId)
                .put("structureId", this.structureId)
                .put("comment", this.comment)
                .put("ownerId", this.ownerId);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean isCounsellorRegularisation() {
        return counsellorRegularisation;
    }

    public void setCounsellorRegularisation(Boolean counsellorRegularisation) {
        this.counsellorRegularisation = counsellorRegularisation;
    }

    public Long getReasonId() {
        return reasonId;
    }

    public void setReasonId(Long reasonId) {
        this.reasonId = reasonId;
    }

    public String getStructureId() {
        return structureId;
    }

    public void setStructureId(String structureId) {
        this.structureId = structureId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public List<Audience> getAudiences() {
        return audiences;
    }

    public void addStudent(Audience audience) {
        this.audiences.add(audience);
    }
}
