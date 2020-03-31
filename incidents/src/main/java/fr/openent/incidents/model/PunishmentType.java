package fr.openent.incidents.model;

public class PunishmentType {

    private Integer id;
    private String structure_id;
    private String label;
    private String type;
    private Integer punishment_category_id;

    public PunishmentType(String structure_id, String label, String type, Integer punishment_category_id) {
        this.structure_id = structure_id;
        this.label = label;
        this.type = type;
        this.punishment_category_id = punishment_category_id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStructureId() {
        return structure_id;
    }

    public void setStructureId(String structure_id) {
        this.structure_id = structure_id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getPunishmentCategoryId() {
        return punishment_category_id;
    }

    public void setPunishmentCategoryId(Integer punishment_category_id) {
        this.punishment_category_id = punishment_category_id;
    }
}
