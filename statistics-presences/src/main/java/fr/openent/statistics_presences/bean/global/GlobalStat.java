package fr.openent.statistics_presences.bean.global;

import fr.openent.statistics_presences.bean.Stat;
import fr.openent.statistics_presences.indicator.impl.Global;
import fr.openent.statistics_presences.utils.EventType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GlobalStat implements Stat {
    private EventType type;
    private String user;
    private String structure;
    private String startDate;
    private String endDate;
    private String className;
    private String name;
    private JsonArray audiences = new JsonArray();
    private Long reason;
    private Long punishmentType;

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

    public GlobalStat setStartDate(String date) {
        this.startDate = date;
        return this;
    }

    public GlobalStat setEndDate(String date) {
        this.endDate = date;
        return this;
    }

    public GlobalStat setReason(Long reason) {
        this.reason = reason;
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

    public Long getPunishmentType() {
        return punishmentType;
    }

    public GlobalStat setPunishmentType(Long punishmentType) {
        this.punishmentType = punishmentType;
        return this;
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("indicator", Global.class.getName())
                .put("user", this.user)
                .put("name", this.name)
                .put("class_name", this.className)
                .put("type", this.type.name())
                .put("reason", this.reason)
                .put("punishment_type", this.punishmentType)
                .put("start_date", this.startDate)
                .put("end_date", this.endDate)
                .put("structure", this.structure)
                .put("audiences", this.audiences);
    }
}
