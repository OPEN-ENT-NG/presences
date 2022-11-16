package fr.openent.statistics_presences.bean.monthly;

import fr.openent.statistics_presences.bean.Stat;
import fr.openent.statistics_presences.utils.EventType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @deprecated  Replaced by {@link fr.openent.statistics_presences.bean.statistics.StatisticsData}
 */
@Deprecated
public class MonthlyStat implements Stat {
    private String indicator;
    private EventType type;
    private String user;
    private String structure;
    private String startDate;
    private String endDate;
    private String month;
    private Integer total;
    private Integer slots;
    private String className;
    private String name;
    private JsonArray audiences = new JsonArray();
    private Long reason;
    private Long punishmentType;
    private String groupedPunishmentId;


    public MonthlyStat setIndicator(String indicator) {
        this.indicator = indicator;
        return this;
    }

    public MonthlyStat setUser(String user) {
        this.user = user;
        return this;
    }

    public MonthlyStat setStructure(String structure) {
        this.structure = structure;
        return this;
    }

    public MonthlyStat setAudiences(JsonArray audiences) {
        this.audiences = audiences;
        return this;
    }

    public MonthlyStat setType(EventType type) {
        this.type = type;
        return this;
    }

    public MonthlyStat setStartDate(String date) {
        this.startDate = date;
        return this;
    }

    public MonthlyStat setEndDate(String date) {
        this.endDate = date;
        return this;
    }

    public MonthlyStat setSlots(Integer count) {
        this.slots = count;
        return this;
    }

    public Integer getSlots() {
        return slots;
    }

    public MonthlyStat setMonth(String month) {
        this.month = month;
        return this;
    }

    public String getMonth() {
        return month;
    }

    public MonthlyStat setTotal(Integer total) {
        this.total = total;
        return this;
    }

    public MonthlyStat setReason(Long reason) {
        this.reason = reason;
        return this;
    }


    public Long getReason() {
        return reason;
    }

    public MonthlyStat setName(String name) {
        this.name = name;
        return this;
    }

    public MonthlyStat setClassName(String className) {
        this.className = className;
        return this;
    }

    public Long getPunishmentType() {
        return punishmentType;
    }

    public MonthlyStat setPunishmentType(Long punishmentType) {
        this.punishmentType = punishmentType;
        return this;
    }

    public String getGroupedPunishmentId() {
        return groupedPunishmentId;
    }

    public MonthlyStat setGroupedPunishmentId(String groupedPunishmentId) {
        this.groupedPunishmentId = groupedPunishmentId;
        return this;
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("indicator", this.indicator)
                .put("user", this.user)
                .put("name", this.name)
                .put("class_name", this.className)
                .put("type", this.type.name())
                .put("reason", this.reason)
                .put("punishment_type", this.punishmentType)
                .put("grouped_punishment_id", this.groupedPunishmentId)
                .put("start_date", this.startDate)
                .put("end_date", this.endDate)
                .put("structure", this.structure)
                .put("audiences", this.audiences);
    }
}
