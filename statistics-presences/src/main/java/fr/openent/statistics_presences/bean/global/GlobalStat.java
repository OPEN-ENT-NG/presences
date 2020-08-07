package fr.openent.statistics_presences.bean.global;

import fr.openent.statistics_presences.indicator.impl.Global;
import fr.openent.statistics_presences.utils.EventType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class GlobalStat {
    private EventType type;
    private String user;
    private String structure;
    private String startDate;
    private String endDate;
    private Integer slots;
    private String className;
    private String name;
    private JsonArray audiences = new JsonArray();
    private JsonArray reasons = new JsonArray();

    public GlobalStat setUser(String user) {
        this.user = user;
        return this;
    }

    public GlobalStat setStructure(String structure) {
        this.structure = structure;
        return this;
    }

    public GlobalStat setAudiences(JsonArray audiences) {
        this.audiences = audiences;
        return this;
    }

    public GlobalStat setType(EventType type) {
        this.type = type;
        return this;
    }

    public GlobalStat setSlots(Integer count) {
        this.slots = count;
        return this;
    }

    public GlobalStat setStartDate(String date) {
        this.startDate = date;
        return this;
    }

    public GlobalStat setEndDate(String date) {
        this.endDate = date;
        return this;
    }

    public GlobalStat setReasons(List<Integer> reasons) {
        this.reasons = new JsonArray(reasons);
        return this;
    }

    public GlobalStat setName(String name) {
        this.name = name;
        return this;
    }

    public GlobalStat setClassName(String className) {
        this.className = className;
        return this;
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("indicator", Global.class.getName())
                .put("user", this.user)
                .put("name", this.name)
                .put("class_name", this.className)
                .put("type", this.type.name())
                .put("slot", this.slots)
                .put("reasons", this.reasons)
                .put("start_date", this.startDate)
                .put("end_date", this.endDate)
                .put("structure", this.structure)
                .put("audiences", this.audiences);
    }
}
