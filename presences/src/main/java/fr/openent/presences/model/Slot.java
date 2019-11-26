package fr.openent.presences.model;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Slot {

    public static List<String> MANDATORY_ATTRIBUTE = new ArrayList<>(
            Arrays.asList("id", "structure_id", "name", "start_hour", "end_hour")
    );

    private String id;
    private String structureId;
    private String name;
    private String startHour;
    private String endHour;

    public Slot(JsonObject slot, List<String> mandatoryAttributes) {
        for (String attribute : mandatoryAttributes) {
            if (!slot.containsKey(attribute) || slot.getValue(attribute) == null) {
                throw new IllegalArgumentException("[Presences@SlotModel] mandatory attribute not present " + attribute);
            }
        }
        this.id = slot.getString("id", "");
        this.structureId = slot.getString("structure_id", "");
        this.name = slot.getString("subjectId", "");
        this.startHour = slot.getString("start_hour", "");
        this.endHour = slot.getString("end_hour", "");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStructureId() {
        return structureId;
    }

    public void setStructureId(String structureId) {
        this.structureId = structureId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartHour() {
        return startHour;
    }

    public void setStartHour(String startHour) {
        this.startHour = startHour;
    }

    public String getEndHour() {
        return endHour;
    }

    public void setEndHour(String endHour) {
        this.endHour = endHour;
    }
}