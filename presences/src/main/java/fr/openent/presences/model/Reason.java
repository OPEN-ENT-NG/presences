package fr.openent.presences.model;

import fr.openent.presences.core.constants.Field;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Reason {

    public static List<String> MANDATORY_ATTRIBUTE = new ArrayList<>();

    private Integer id;
    private String structureId;
    private String label;
    private Boolean isProving;
    private String comment;
    private Boolean isDefault;
    private Boolean isGroup;
    private Boolean isHidden;
    private Boolean isAbsenceCompliance;
    private Integer reasonTypeId;

    public Reason(JsonObject reason, List<String> mandatoryAttributes) {
        for (String attribute : mandatoryAttributes) {
            if (!reason.containsKey(attribute) || reason.getValue(attribute) == null) {
                throw new IllegalArgumentException("[Presences@ReasonModel] mandatory attribute not present " + attribute);
            }
        }
        this.id = reason.getInteger("id", null);
        this.structureId = reason.getString("structure_id", null);
        this.label = reason.getString("label", null);
        this.isProving = reason.getBoolean("proving", null);
        this.comment = reason.getString("comment", null);
        this.isDefault = reason.getBoolean("default", null);
        this.isGroup = reason.getBoolean("group", null);
        this.isHidden = reason.getBoolean("hidden", null);
        this.isAbsenceCompliance = reason.getBoolean("absence_compliance", null);
        this.reasonTypeId = reason.getInteger("reason_type_id", null);
    }

    public Reason(Integer reasonId) {
        this.id = reasonId;
    }

    public Reason(String label, boolean isProving) {
        this.label = label;
        this.isProving = isProving;
    }

    public Reason() {

    }

    public JsonObject toJSON() {
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
                .put(Field.REASON_TYPE_ID, this.reasonTypeId);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getReasonTypeId() {
        return reasonTypeId;
    }

    public void setReasonTypeId(Integer reasonTypeId) {
        this.reasonTypeId = reasonTypeId;
    }

    public String getStructureId() {
        return structureId;
    }

    public void setStructureId(String structureId) {
        this.structureId = structureId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean isProving() {
        return isProving;
    }

    public void setProving(Boolean proving) {
        isProving = proving;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

    public Boolean isGroup() {
        return isGroup;
    }

    public void setGroup(Boolean group) {
        isGroup = group;
    }

    public Boolean isHidden() {
        return isHidden;
    }

    public void setHidden(Boolean hidden) {
        isHidden = hidden;
    }

    public Boolean isAbsenceCompliance() {
        return isAbsenceCompliance;
    }

    public void setAbsenceCompliance(Boolean absenceCompliance) {
        isAbsenceCompliance = absenceCompliance;
    }
}
