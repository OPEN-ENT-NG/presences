package fr.openent.presences.helper;

import fr.openent.presences.common.message.MessageResponseHandler;
import fr.openent.presences.model.Slot;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class SlotHelper {

    private final EventBus eb;

    public SlotHelper(EventBus eb) {
        this.eb = eb;
    }

    public static List<Slot> getSlotListFromJsonArray(JsonArray slotsJsonArray, List<String> mandatoryAttributes) {
        List<Slot> slots = new ArrayList<>();
        for (Object o : slotsJsonArray) {
            if (!(o instanceof JsonObject)) continue;
            Slot slot = new Slot((JsonObject) o, mandatoryAttributes);
            slots.add(slot);
        }
        return slots;
    }

    public static List<Slot> getSlotListFromJsonArray(JsonArray slotsJsonArray) {
        List<Slot> slots = new ArrayList<>();
        for (Object o : slotsJsonArray) {
            if (!(o instanceof JsonObject)) continue;
            Slot slot = new Slot((JsonObject) o);
            slots.add(slot);
        }
        return slots;
    }

    public static JsonArray getSlotJsonArrayFromList(List<Slot> slotsList) {
        JsonArray slots = new JsonArray();
        for (Slot slot : slotsList) {
            JsonObject slotObject = new JsonObject();
            slotObject.put("id", slot.getId());
            slotObject.put("name", slot.getName());
            slotObject.put("startHour", slot.getStartHour());
            slotObject.put("endHour", slot.getEndHour());
            slots.add(slotObject);
        }
        return slots;
    }

    public void getTimeSlots(String structureId, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "timeslot.getSlotProfiles")
                .put("structureId", structureId);

        eb.send("viescolaire", action, MessageResponseHandler.messageJsonObjectHandler(handler));
    }

}
