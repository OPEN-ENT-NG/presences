package fr.openent.presences.model;

import fr.openent.presences.core.constants.Field;
import io.vertx.core.json.JsonObject;

public class SlotModel implements IModel<SlotModel>, Comparable<SlotModel>{
    private String id;
    private String name;
    private String startHour;
    private String endHour;

    public SlotModel() {
    }

    public SlotModel(JsonObject slot) {
        this.id = slot.getString(Field.ID);
        this.name = slot.getString(Field.NAME);
        this.startHour = slot.getString(Field.STARTHOUR);
        this.endHour = slot.getString(Field.ENDHOUR);
    }

    public String getId() {
        return id;
    }

    public SlotModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public SlotModel setName(String name) {
        this.name = name;
        return this;
    }

    public String getStartHour() {
        return startHour;
    }

    public SlotModel setStartHour(String startHour) {
        this.startHour = startHour;
        return this;
    }

    public String getEndHour() {
        return endHour;
    }

    public SlotModel setEndHour(String endHour) {
        this.endHour = endHour;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(Field.ID, this.id)
                .put(Field.NAME, this.name)
                .put(Field.STARTHOUR, this.startHour)
                .put(Field.ENDHOUR, this.endHour);
    }

    @Override
    public boolean validate() {
        return this.id != null && !this.id.isEmpty()
                && this.name != null && !this.name.isEmpty()
                && this.endHour != null && !this.endHour.isEmpty()
                && this.startHour != null && !this.startHour.isEmpty();
    }

    @Override
    public int compareTo(SlotModel slotModel) {
        return this.startHour.compareTo(slotModel.startHour);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SlotModel && ((SlotModel) obj).getId().equals(this.getId());
    }
}
