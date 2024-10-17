package fr.openent.presences.model;

public class Action {
    private Integer id;
    private String structureId;
    private String label;
    private String abbreviation;
    private boolean hidden;
    private String created;

    public Action(Integer id, String structure, String text, String abbreviation, boolean hidden, String created) {
        this.id = id;
        this.structureId = structure;
        this.label = text;
        this.abbreviation = abbreviation;
        this.hidden = hidden;
        this.created = created;
    }

    public Action(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public Action setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getStructureId() {
        return structureId;
    }

    public Action setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public Action setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public Action setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
        return this;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Action setHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public Action setCreated(String created) {
        this.created = created;
        return this;
    }
}
