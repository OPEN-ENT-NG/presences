package fr.openent.presences.model;

import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.core.constants.Field;
import io.vertx.core.json.JsonObject;

public class StatisticsUser implements IModel<StatisticsUser> {
    private String id; //StudentId
    private String structureId;
    private String modified;

    public StatisticsUser() {
    }

    public StatisticsUser(JsonObject jsonObject) {
        this.id = jsonObject.getString(Field.ID, null);
        this.structureId = jsonObject.getString(Field.STRUCTURE_ID, null);
        this.modified = jsonObject.getString(Field.MODIFIED, null);
    }

    public String getId() {
        return id;
    }

    public StatisticsUser setId(String id) {
        this.id = id;
        return this;
    }

    public String getStructureId() {
        return structureId;
    }

    public StatisticsUser setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    public String getModified() {
        return modified;
    }

    public StatisticsUser setModified(String modified) {
        this.modified = modified;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(false, this);
    }

    @Override
    public boolean validate() {
        return false;
    }
}
