package fr.openent.statistics_presences.bean.timeslot;

import fr.openent.presences.core.constants.Field;
import io.vertx.core.json.JsonObject;

/**
 * @deprecated  Replaced by {@link fr.openent.presences.model.SlotModel}
 */
@Deprecated
public class Slot {
    private String id;
    private String name;
    private String startHour;
    private String endHour;

    public Slot(JsonObject slot) {
        this.id = slot.getString(Field.ID);
        this.name = slot.getString(Field.NAME);
        this.startHour = slot.getString(Field.STARTHOUR);
        this.endHour = slot.getString(Field.ENDHOUR);
    }

    public String getId() {
        return id;
    }

    public String getStartHour() {
        return startHour;
    }

    public String getName() {
        return name;
    }

    public String getEndHour() {
        return endHour;
    }
}
