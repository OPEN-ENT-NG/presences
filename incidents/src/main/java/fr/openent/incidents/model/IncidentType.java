package fr.openent.incidents.model;

public class IncidentType {
    private int id;
    private String structureId;
    private String label;
    private boolean hidden;
    private String created;

    public IncidentType(int id, String structureId, String label, boolean hidden, String created) {
        this.id = id;
        this.structureId = structureId;
        this.label = label;
        this.hidden = hidden;
        this.created = created;
    }

    public IncidentType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public IncidentType setId(int id) {
        this.id = id;
        return this;
    }

    public String getStructureId() {
        return structureId;
    }

    public IncidentType setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public IncidentType setLabel(String label) {
        this.label = label;
        return this;
    }

    public boolean isHidden() {
        return hidden;
    }

    public IncidentType setHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public IncidentType setCreated(String created) {
        this.created = created;
        return this;
    }
}
