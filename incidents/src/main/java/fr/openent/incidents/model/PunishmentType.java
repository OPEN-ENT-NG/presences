package fr.openent.incidents.model;

import fr.openent.presences.model.Model;
import io.vertx.core.json.JsonObject;

public class PunishmentType extends Model {

    private Integer id;
    private String structure_id;
    private String label;
    private String type;
    private Integer punishment_category_id;
    private Boolean isHidden;
    private Boolean isUsed;

    public PunishmentType(String structure_id, String label, String type, Integer punishment_category_id, Boolean hidden) {
        this.structure_id = structure_id;
        this.label = label;
        this.type = type;
        this.punishment_category_id = punishment_category_id;
        this.isHidden = hidden;
    }

    public PunishmentType(JsonObject punishmentType) {
        this.id = punishmentType.getInteger("id", null);
        this.structure_id = punishmentType.getString("structure_id", null);
        this.label = punishmentType.getString("label", null);
        this.type = punishmentType.getString("type", null);
        this.punishment_category_id = punishmentType.getInteger("punishment_category_id", null);
        this.isHidden = punishmentType.getBoolean("hidden", null);
    }

    @Override
    public JsonObject toJsonObject() {
        return new JsonObject()
                .put("id", this.id)
                .put("structure_id", this.structure_id)
                .put("label", this.label)
                .put("type", this.type)
                .put("punishment_category_id", this.punishment_category_id)
                .put("used", this.isUsed)
                .put("hidden", this.isHidden);
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

    public Boolean isHidden() {
        return isHidden;
    }

    public void setHidden(Boolean hidden) {
        isHidden = hidden;
    }

    public void setUsed(Boolean used) {
        isUsed = used;
    }
}
