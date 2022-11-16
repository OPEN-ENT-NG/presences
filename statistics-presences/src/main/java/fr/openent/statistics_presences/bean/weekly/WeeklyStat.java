package fr.openent.statistics_presences.bean.weekly;

import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.bean.Stat;
import fr.openent.statistics_presences.utils.EventType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @deprecated  Replaced by {@link fr.openent.statistics_presences.bean.statistics.StatisticsData}
 */
@Deprecated
public class WeeklyStat implements Stat {
    private String indicator;
    private EventType type;
    private String user;
    private String structure;
    private String startDate;
    private String endDate;
    private Integer slots;
    private String slotId;
    private String className;
    private String name;
    private JsonArray audiences = new JsonArray();
    private Long reason;
    private Long punishmentType;
    private String groupedPunishmentId;


    public WeeklyStat setIndicator(String indicator) {
        this.indicator = indicator;
        return this;
    }

    public WeeklyStat setUser(String user) {
        this.user = user;
        return this;
    }

    public WeeklyStat setStructure(String structure) {
        this.structure = structure;
        return this;
    }

    public WeeklyStat setAudiences(JsonArray audiences) {
        this.audiences = audiences;
        return this;
    }

    public WeeklyStat setType(EventType type) {
        this.type = type;
        return this;
    }

    public WeeklyStat setStartDate(String date) {
        this.startDate = date;
        return this;
    }

    public WeeklyStat setEndDate(String date) {
        this.endDate = date;
        return this;
    }

    public WeeklyStat setSlots(Integer count) {
        this.slots = count;
        return this;
    }

    public Integer getSlots() {
        return slots;
    }

    public WeeklyStat setSlotId(String slotId) {
        this.slotId = slotId;
        return this;
    }

    public String getSlotId() {
        return slotId;
    }

    public WeeklyStat setReason(Long reason) {
        this.reason = reason;
        return this;
    }


    public Long getReason() {
        return reason;
    }

    public WeeklyStat setName(String name) {
        this.name = name;
        return this;
    }

    public WeeklyStat setClassName(String className) {
        this.className = className;
        return this;
    }

    public Long getPunishmentType() {
        return punishmentType;
    }

    public WeeklyStat setPunishmentType(Long punishmentType) {
        this.punishmentType = punishmentType;
        return this;
    }

    public String getGroupedPunishmentId() {
        return groupedPunishmentId;
    }

    public WeeklyStat setGroupedPunishmentId(String groupedPunishmentId) {
        this.groupedPunishmentId = groupedPunishmentId;
        return this;
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put(Field.INDICATOR, this.indicator)
                .put(Field.USER, this.user)
                .put(Field.NAME, this.name)
                .put(Field.CLASS_NAME, this.className)
                .put(Field.TYPE, this.type.name())
                .put(Field.REASON, this.reason)
                .put(Field.PUNISHMENT_TYPE, this.punishmentType)
                .put(Field.GROUPED_PUNISHMENT_ID, this.groupedPunishmentId)
                .put(Field.START_DATE, this.startDate)
                .put(Field.END_DATE, this.endDate)
                .put(Field.STRUCTURE, this.structure)
                .put(Field.AUDIENCES, this.audiences)
                .put(Field.SLOT_ID, this.slotId);
    }
}
