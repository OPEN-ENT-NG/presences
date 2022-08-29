package fr.openent.incidents.model;

public class Seriousness {
    private int id;
    private String structureId;
    private String label;
    private int level;
    private boolean hidden;
    private String created;
    private boolean excludeAlertSeriousness;

    public Seriousness(int id, String structureId, String label, int level, boolean hidden, String created,
                       boolean excludeAlertSeriousness) {
        this.id = id;
        this.structureId = structureId;
        this.label = label;
        this.level = level;
        this.hidden = hidden;
        this.created = created;
        this.excludeAlertSeriousness = excludeAlertSeriousness;
    }

    public Seriousness(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Seriousness setId(int id) {
        this.id = id;
        return this;
    }

    public String getStructureId() {
        return structureId;
    }

    public Seriousness setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public Seriousness setLabel(String label) {
        this.label = label;
        return this;
    }

    public int getLevel() {
        return level;
    }

    public Seriousness setLevel(int level) {
        this.level = level;
        return this;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Seriousness setHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public Seriousness setCreated(String created) {
        this.created = created;
        return this;
    }

    public boolean isExcludeAlertSeriousness() {
        return excludeAlertSeriousness;
    }

    public Seriousness setExcludeAlertSeriousness(boolean excludeAlertSeriousness) {
        this.excludeAlertSeriousness = excludeAlertSeriousness;
        return this;
    }
}
