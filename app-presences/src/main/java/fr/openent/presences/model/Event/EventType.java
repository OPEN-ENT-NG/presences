package fr.openent.presences.model.Event;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.IModel;
import fr.openent.presences.model.Settings;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class EventType implements IModel<EventType> {

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

    public EventType(Integer id, String label) {
        this.id = id;
        this.label = label;
    }

    public EventType(JsonObject eventType) {
        this.id = eventType.getInteger(Field.ID);
        this.label = eventType.getString(Field.LABEL);
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("id", this.id)
                .put("label", this.label);
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

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(Field.ID, this.id)
                .put(Field.LABEL, this.label);
    }

    @Override
    public boolean validate() {
        return this.id != null && this.label != null;
    }
}
