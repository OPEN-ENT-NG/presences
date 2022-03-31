package fr.openent.statistics_presences.bean.weekly;

import fr.openent.presences.core.constants.Field;
import io.vertx.core.json.JsonObject;

public class WeeklyAudience {
    private String structureId;
    private String audienceId;
    private Integer registerId;
    private String slotId;
    private String startAt;
    private String endAt;
    private Integer studentCount;

    public WeeklyAudience setStructureId(String structure) {
        this.structureId = structure;
        return this;
    }

    public WeeklyAudience setAudienceId(String audienceId) {
        this.audienceId = audienceId;
        return this;
    }

    public WeeklyAudience setRegisterId(Integer registerId) {
        this.registerId = registerId;
        return this;
    }

    public WeeklyAudience setStartAt(String date) {
        this.startAt = date;
        return this;
    }

    public WeeklyAudience setEndAt(String date) {
        this.endAt = date;
        return this;
    }

    public WeeklyAudience setSlotId(String slotId) {
        this.slotId = slotId;
        return this;
    }

    public WeeklyAudience setStudentCount(Integer studentCount) {
        this.studentCount = studentCount;
        return this;
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put(Field._ID, new JsonObject()
                        .put(Field.REGISTER_ID, this.registerId)
                        .put(Field.AUDIENCE_ID, this.audienceId)
                        .put(Field.START_AT, this.startAt)
                        .put(Field.END_AT, this.endAt)
                )
                .put(Field.STRUCTURE_ID, this.structureId)
                .put(Field.SLOT_ID, this.slotId)
                .put(Field.STUDENT_COUNT, this.studentCount);
    }
}