package fr.openent.incidents.model;

public class Partner {
    private int id;
    private String structureId;
    private String label;
    private boolean hidden;
    private String created;

    public Partner(int id, String structureId, String label, boolean hidden, String created) {
        this.id = id;
        this.structureId = structureId;
        this.label = label;
        this.hidden = hidden;
        this.created = created;
    }

    public Partner(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Partner setId(int id) {
        this.id = id;
        return this;
    }

    public String getStructureId() {
        return structureId;
    }

    public Partner setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public Partner setLabel(String label) {
        this.label = label;
        return this;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Partner setHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public Partner setCreated(String created) {
        this.created = created;
        return this;
    }
}
