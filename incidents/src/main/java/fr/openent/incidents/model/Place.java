package fr.openent.incidents.model;

public class Place {
    private int id;
    private String structureId;
    private String label;
    private String created;

    public Place(int id, String structureId, String label, String created) {
        this.id = id;
        this.structureId = structureId;
        this.label = label;
        this.created = created;
    }

    public Place(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Place setId(int id) {
        this.id = id;
        return this;
    }

    public String getStructureId() {
        return structureId;
    }

    public Place setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public Place setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public Place setCreated(String created) {
        this.created = created;
        return this;
    }
}
