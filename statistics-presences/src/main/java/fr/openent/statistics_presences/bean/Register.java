package fr.openent.statistics_presences.bean;

import fr.openent.presences.core.constants.Field;
import io.vertx.core.json.JsonObject;

public class Register {

    private Integer id;
    private String audienceId;
    private Integer stateId;
    private String startAt;
    private String endAt;
    private String structureId;

    public Register(JsonObject register) {
        this.id = register.getInteger(Field.ID);
        this.audienceId = register.getString(Field.GROUP_ID);
        this.stateId = register.getInteger(Field.STATE_ID);
        this.startAt = register.getString(Field.START_DATE);
        this.endAt = register.getString(Field.END_DATE);
        this.structureId = register.getString(Field.STRUCTURE_ID);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAudienceId() {
        return audienceId;
    }

    public void setAudienceId(String audienceId) {
        this.audienceId = audienceId;
    }

    public Integer getStateId() {
        return stateId;
    }

    public void setStateId(Integer stateId) {
        this.stateId = stateId;
    }

    public String getStartAt() {
        return startAt;
    }

    public void setStartAt(String startAt) {
        this.startAt = startAt;
    }

    public String getEndAt() {
        return endAt;
    }

    public void setEndAt(String endAt) {
        this.endAt = endAt;
    }

    public String getStructureId() {
        return structureId;
    }

    public void setStructureId(String structureId) {
        this.structureId = structureId;
    }
}
