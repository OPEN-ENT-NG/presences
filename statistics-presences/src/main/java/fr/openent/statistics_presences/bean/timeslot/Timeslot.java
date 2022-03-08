package fr.openent.statistics_presences.bean.timeslot;

import fr.openent.presences.core.constants.Field;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class Timeslot {
    private String id;
    private String structureId;
    private String audienceId;
    private List<Slot> slots;

    @SuppressWarnings("unchecked")
    public Timeslot(JsonObject timeslot) {
        this.id = timeslot.getString(Field._ID);
        this.structureId = timeslot.getString(Field.SCHOOLID);
        this.audienceId = timeslot.getString(Field.AUDIENCEID);
        this.slots = ((List<JsonObject>) timeslot.getJsonArray(Field.SLOTS, new JsonArray()).getList())
                .stream()
                .map(Slot::new)
                .collect(Collectors.toList());
    }

    public String getId() {
        return id;
    }

    public String getStructureId() {
        return structureId;
    }

    public String getAudienceId() {
        return audienceId;
    }

    public List<Slot> getSlots() {
        return slots;
    }
}
