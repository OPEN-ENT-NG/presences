package fr.openent.presences.model;

import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.core.constants.Field;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class StructureStatisticsUser implements IModel<StructureStatisticsUser> {
    private List<StatisticsUser> statisticsUsers = new ArrayList<>();
    private String structureId;

    public StructureStatisticsUser() {
    }

    public StructureStatisticsUser(JsonObject jsonObject) {
        this.statisticsUsers = IModelHelper.toList(jsonObject.getJsonArray(Field.STATISTICS_USERS), StatisticsUser.class);
        this.structureId = jsonObject.getString(Field.STRUCTURE_ID);
    }

    public List<StatisticsUser> getStatisticsUsers() {
        return statisticsUsers;
    }

    public StructureStatisticsUser setStatisticsUsers(List<StatisticsUser> statisticsUsers) {
        this.statisticsUsers = statisticsUsers;
        return this;
    }

    public String getStructureId() {
        return structureId;
    }

    public StructureStatisticsUser setStructureId(String structureId) {
        this.structureId = structureId;
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
