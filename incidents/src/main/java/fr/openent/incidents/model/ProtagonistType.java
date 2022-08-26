package fr.openent.incidents.model;

import fr.openent.incidents.model.punishmentCategory.PunishmentCategory;

public class ProtagonistType {
    private int id;
    private String structureId;
    private String label;
    private String type;
    private PunishmentCategory punishmentCategory;
    private boolean hidden;
    private String created;

    public ProtagonistType(int id, String structureId, String label, String type, PunishmentCategory punishmentCategory,
                           boolean hidden, String created) {
        this.id = id;
        this.structureId = structureId;
        this.label = label;
        this.type = type;
        this.punishmentCategory = punishmentCategory;
        this.hidden = hidden;
        this.created = created;
    }

    public ProtagonistType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public ProtagonistType setId(int id) {
        this.id = id;
        return this;
    }

    public String getStructureId() {
        return structureId;
    }

    public ProtagonistType setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public ProtagonistType setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getType() {
        return type;
    }

    public ProtagonistType setType(String type) {
        this.type = type;
        return this;
    }

    public PunishmentCategory getPunishmentCategory() {
        return punishmentCategory;
    }

    public ProtagonistType setPunishmentCategory(PunishmentCategory punishmentCategory) {
        this.punishmentCategory = punishmentCategory;
        return this;
    }

    public boolean isHidden() {
        return hidden;
    }

    public ProtagonistType setHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public ProtagonistType setCreated(String created) {
        this.created = created;
        return this;
    }
}
