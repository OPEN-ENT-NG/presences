package fr.openent.incidents.controller;

import fr.openent.incidents.service.IncidentsService;
import fr.openent.incidents.service.impl.DefaultIncidentsService;
import fr.wseduc.bus.BusAddress;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.entcore.common.bus.BusResponseHandler;
import org.entcore.common.controller.ControllerHelper;

import java.util.List;

public class EventBusController extends ControllerHelper {
    private IncidentsService incidentsService;

    public EventBusController(EventBus eb) {
        incidentsService = new DefaultIncidentsService(eb);
    }

    @BusAddress("fr.openent.incidents")
    public void bus(final Message<JsonObject> message) {
        JsonObject body = message.body();
        String action = body.getString("action");
        switch (action) {
            case "get-incidents-users-range":
                String startDate = body.getString("startDate");
                String endDate = body.getString("endDate");
                List<String> users = body.getJsonArray("users").getList();
                incidentsService.get(startDate, endDate, users, BusResponseHandler.busArrayHandler(message));
                break;
            default:
                message.reply(new JsonObject()
                        .put("status", "error")
                        .put("message", "Invalid action."));
        }
    }
}
