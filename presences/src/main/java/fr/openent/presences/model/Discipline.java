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

    public Discipline setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getStructureId() {
        return structureId;
    }

    public Discipline setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public Discipline setLabel(String label) {
        this.label = label;
        return this;
    }

    public Boolean getUsed() {
        return isUsed;
    }

    public Discipline setUsed(Boolean used) {
        isUsed = used;
        return this;
    }

    public Boolean getHidden() {
        return isHidden;
    }

    public Discipline setHidden(Boolean hidden) {
        isHidden = hidden;
        return this;
    }
}
