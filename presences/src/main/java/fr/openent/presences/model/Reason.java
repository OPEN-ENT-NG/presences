package fr.openent.presences.model;

import fr.openent.presences.core.constants.Field;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated  Replaced by {@link fr.openent.presences.model.ReasonModel}
 */
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

    public Reason setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getStructureId() {
        return structureId;
    }

    public Reason setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public Reason setLabel(String label) {
        this.label = label;
        return this;
    }

    public Boolean isProving() {
        return isProving;
    }

    public Reason setProving(Boolean proving) {
        isProving = proving;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public Reason setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public Reason setDefault(Boolean aDefault) {
        isDefault = aDefault;
        return this;
    }

    public Boolean isGroup() {
        return isGroup;
    }

    public Reason setGroup(Boolean group) {
        isGroup = group;
        return this;
    }

    public Boolean isHidden() {
        return isHidden;
    }

    public Reason setHidden(Boolean hidden) {
        isHidden = hidden;
        return this;
    }

    public Boolean isAbsenceCompliance() {
        return isAbsenceCompliance;
    }

    public Reason setAbsenceCompliance(Boolean absenceCompliance) {
        isAbsenceCompliance = absenceCompliance;
        return this;
    }

    public Integer getReasonTypeId() {
        return reasonTypeId;
    }

    public Reason setReasonTypeId(Integer reasonTypeId) {
        this.reasonTypeId = reasonTypeId;
        return this;
    }
}
