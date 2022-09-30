package fr.openent.presences.model;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.ReasonType;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ReasonModel implements IModel<ReasonModel>{
    private Integer id;
    private String structureId;
    private String label;
    private Boolean isProving;
    private String comment;
    private Boolean isDefault;
    private Boolean isGroup;
    private Boolean isHidden;
    private Boolean isAbsenceCompliance;
    private ReasonType reasonType;

    public ReasonModel(JsonObject reason) {
        this.id = reason.getInteger(Field.ID, null);
        this.structureId = reason.getString(Field.STRUCTURE_ID, null);
        this.label = reason.getString(Field.LABEL, null);
        this.isProving = reason.getBoolean(Field.PROVING, null);
        this.comment = reason.getString(Field.COMMENT, null);
        this.isDefault = reason.getBoolean(Field.DEFAULT, null);
        this.isGroup = reason.getBoolean(Field.GROUP, null);
        this.isHidden = reason.getBoolean(Field.HIDDEN, null);
        this.isAbsenceCompliance = reason.getBoolean(Field.ABSENCE_COMPILANCE, null);
        this.reasonType = ReasonType.getReasonTypeFromValue(reason.getInteger(Field.REASON_TYPE_ID, 1));
    }

    public ReasonModel() {
    }

    public Integer getId() {
        return id;
    }

    public ReasonModel setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getStructureId() {
        return structureId;
    }

    public ReasonModel setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public ReasonModel setLabel(String label) {
        this.label = label;
        return this;
    }

    public Boolean isProving() {
        return isProving;
    }

    public ReasonModel setProving(Boolean proving) {
        isProving = proving;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public ReasonModel setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public ReasonModel setDefault(Boolean aDefault) {
        isDefault = aDefault;
        return this;
    }

    public Boolean isGroup() {
        return isGroup;
    }

    public ReasonModel setGroup(Boolean group) {
        isGroup = group;
        return this;
    }

    public Boolean isHidden() {
        return isHidden;
    }

    public ReasonModel setHidden(Boolean hidden) {
        isHidden = hidden;
        return this;
    }

    public Boolean isAbsenceCompliance() {
        return isAbsenceCompliance;
    }

    public ReasonModel setAbsenceCompliance(Boolean absenceCompliance) {
        isAbsenceCompliance = absenceCompliance;
        return this;
    }

    public ReasonType getReasonTypeId() {
        return reasonType;
    }

    public ReasonModel setReasonTypeId(ReasonType reasonType) {
        this.reasonType = reasonType;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(Field.ID, this.id)
                .put(Field.STRUCTURE_ID, this.structureId)
                .put(Field.LABEL, this.label)
                .put(Field.PROVING, this.isProving)
                .put(Field.COMMENT, this.comment)
                .put(Field.DEFAULT, this.isDefault)
                .put(Field.GROUP, this.isGroup)
                .put(Field.HIDDEN, this.isHidden)
                .put(Field.ABSENCE_COMPILANCE, this.isAbsenceCompliance)
                .put(Field.REASON_TYPE_ID, this.reasonType.getValue());
    }

    @Override
    public boolean validate() {
        return false;
    }
}
