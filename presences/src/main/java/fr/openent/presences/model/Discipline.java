package fr.openent.presences.model;

import io.vertx.core.json.JsonObject;

public class Discipline implements Cloneable {

    private Integer id;
    private String structureId;
    private String label;
    private Boolean isUsed;
    private Boolean isHidden;

    public Discipline(JsonObject discipline) {
        this.id = discipline.getInteger("id", null);
        this.structureId = discipline.getString("structure_id", discipline.getString("structureId", null));
        this.label = discipline.getString("label", null);
        this.isUsed = discipline.getBoolean("used", null);
        this.isHidden = discipline.getBoolean("hidden", null);
    }

    public Discipline(Integer id) {
        this.id = id;
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("id", this.id)
                .put("structureId", this.structureId)
                .put("label", this.label)
                .put("used", this.isUsed)
                .put("hidden", this.isHidden);
    }

    @Override
    public Discipline clone() {
        try {
            return (Discipline) super.clone();
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

    public Boolean isUsed() {
        return isUsed;
    }

    public void setUsed(Boolean used) {
        isUsed = used;
    }

    public Boolean getHidden() {
        return isHidden;
    }

    public void setHidden(Boolean hidden) {
        isHidden = hidden;
    }
}
