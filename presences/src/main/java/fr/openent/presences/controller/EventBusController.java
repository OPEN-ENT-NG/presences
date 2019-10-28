package fr.openent.presences.controller;

import fr.openent.presences.service.EventService;
import fr.openent.presences.service.impl.DefaultEventService;
import fr.wseduc.bus.BusAddress;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.bus.BusResponseHandler;
import org.entcore.common.controller.ControllerHelper;

import java.util.List;

public class EventBusController extends ControllerHelper {

    private EventService eventService;

    public EventBusController(EventBus eb) {
        this.eventService = new DefaultEventService(eb);
    }

    @BusAddress("fr.openent.presences")
    public void bus(final Message<JsonObject> message) {
        JsonObject body = message.body();
        String action = body.getString("action");
        switch (action) {
            case "get-count-event-by-student":
                Integer eventType = body.getInteger("eventType");
                Boolean justified = body.getBoolean("justified");
                List<String> students = body.getJsonArray("students", new JsonArray()).getList();
                String structure = body.getString("structure");
                Integer startAt = body.getInteger("startAt", 1);
                List<Integer> reasonsId = body.getJsonArray("reasonsId", new JsonArray()).getList();
                Boolean massmailed = body.getBoolean("massmailed");
                String startDate = body.getString("startDate");
                String endDate = body.getString("endDate");
                this.eventService.getCountEventByStudent(eventType, students, structure, justified, startAt, reasonsId, massmailed, startDate, endDate, BusResponseHandler.busArrayHandler(message));
                break;
            default:
                message.reply(new JsonObject()
                        .put("status", "error")
                        .put("message", "Invalid action."));
        }
    }
}
