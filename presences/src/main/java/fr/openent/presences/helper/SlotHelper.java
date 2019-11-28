package fr.openent.presences.helper;

import fr.openent.presences.model.Slot;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class SlotHelper {

    public static List<Slot> getSlotListFromJsonArray(JsonArray slotsJsonArray, List<String> mandatoryAttributes) {
        List<Slot> slots = new ArrayList<>();
        for (Object o : slotsJsonArray) {
            if (!(o instanceof JsonObject)) continue;
            Slot slot = new Slot((JsonObject) o, mandatoryAttributes);
            slots.add(slot);
        }
        return slots;
    }

}
