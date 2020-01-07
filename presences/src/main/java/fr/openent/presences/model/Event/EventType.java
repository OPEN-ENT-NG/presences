package fr.openent.presences.model.Event;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class EventType {

    public static List<String> MANDATORY_ATTRIBUTE = new ArrayList<>();

    private Integer id;
    private String label;

    public EventType(JsonObject slot, List<String> mandatoryAttributes) {
        for (String attribute : mandatoryAttributes) {
            if (!slot.containsKey(attribute) || slot.getValue(attribute) == null) {
                throw new IllegalArgumentException("[Presences@EventTypeModel] mandatory attribute not present " + attribute);
            }
        }
        this.id = slot.getInteger("id", null);
        this.label = slot.getString("label", null);
    }

    public EventType(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
