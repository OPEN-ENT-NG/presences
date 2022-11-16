package fr.openent.statistics_presences.bean.statistics;

import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.model.IModel;
import fr.openent.presences.model.SlotModel;
import fr.openent.statistics_presences.utils.EventType;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class StatisticsData implements IModel<StatisticsData> {
    private EventType type;
    private String user;
    private String name;
    private String className;
    private String structure;
    private List<String> audiences = new ArrayList<>();
    private Long reason;
    private Long punishmentType;
    private String groupedPunishmentId;
    private String startDate;
    private String endDate;
    private List<SlotModel> slots = new ArrayList<>();

    public StatisticsData() {
    }

    public StatisticsData(JsonObject jsonObject) {
        throw new UnsupportedOperationException("Scheduled for issue #253");
    }

    public EventType getType() {
        return type;
    }

    public StatisticsData setType(EventType type) {
        this.type = type;
        return this;
    }

    public String getUser() {
        return user;
    }

    public StatisticsData setUser(String user) {
        this.user = user;
        return this;
    }

    public String getName() {
        return name;
    }

    public StatisticsData setName(String name) {
        this.name = name;
        return this;
    }

    public String getClassName() {
        return className;
    }

    public StatisticsData setClassName(String className) {
        this.className = className;
        return this;
    }

    public String getStructure() {
        return structure;
    }

    public StatisticsData setStructure(String structure) {
        this.structure = structure;
        return this;
    }

    public List<String> getAudiences() {
        return audiences;
    }

    public StatisticsData setAudiences(List<String> audiences) {
        this.audiences = audiences;
        return this;
    }

    public Long getReason() {
        return reason;
    }

    public StatisticsData setReason(Long reason) {
        this.reason = reason;
        return this;
    }

    public Long getPunishmentType() {
        return punishmentType;
    }

    public StatisticsData setPunishmentType(Long punishmentType) {
        this.punishmentType = punishmentType;
        return this;
    }

    public String getGroupedPunishmentId() {
        return groupedPunishmentId;
    }

    public StatisticsData setGroupedPunishmentId(String groupedPunishmentId) {
        this.groupedPunishmentId = groupedPunishmentId;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public StatisticsData setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public StatisticsData setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public List<SlotModel> getSlots() {
        return slots;
    }

    public StatisticsData setSlots(List<SlotModel> slots) {
        this.slots = slots;
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