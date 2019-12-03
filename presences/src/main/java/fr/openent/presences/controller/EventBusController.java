package fr.openent.presences.controller;

import fr.openent.presences.service.EventService;
import fr.openent.presences.service.ReasonService;
import fr.openent.presences.service.SettingsService;
import fr.openent.presences.service.impl.DefaultEventService;
import fr.openent.presences.service.impl.DefaultReasonService;
import fr.openent.presences.service.impl.DefaultSettingsService;
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
    private ReasonService reasonService = new DefaultReasonService();
    private SettingsService settingsService = new DefaultSettingsService();

    public EventBusController(EventBus eb) {
        this.eventService = new DefaultEventService(eb);
    }

    @BusAddress("fr.openent.presences")
    public void bus(final Message<JsonObject> message) {
        JsonObject body = message.body();
        String action = body.getString("action");
        Integer eventType;
        Boolean justified;
        List<String> students;
        String structure;
        Integer startAt;
        List<Integer> reasonsId;
        Boolean massmailed;
        String startDate;
        String endDate;
        boolean noReasons;
        switch (action) {
            case "get-count-event-by-student":
                eventType = body.getInteger("eventType");
                justified = body.getBoolean("justified");
                students = body.getJsonArray("students", new JsonArray()).getList();
                structure = body.getString("structure");
                startAt = body.getInteger("startAt", 1);
                reasonsId = body.getJsonArray("reasonsId", new JsonArray()).getList();
                massmailed = body.getBoolean("massmailed");
                startDate = body.getString("startDate");
                endDate = body.getString("endDate");
                noReasons = body.getBoolean("noReasons");
                this.eventService.getCountEventByStudent(eventType, students, structure, justified, startAt, reasonsId, massmailed, startDate, endDate, noReasons, BusResponseHandler.busArrayHandler(message));
                break;
            case "get-events-by-student":
                List<Integer> eventTypes = body.getJsonArray("eventType").getList();
                justified = body.getBoolean("justified");
                students = body.getJsonArray("students", new JsonArray()).getList();
                structure = body.getString("structure");
                reasonsId = body.getJsonArray("reasonsId", new JsonArray()).getList();
                massmailed = body.getBoolean("massmailed");
                startDate = body.getString("startDate");
                endDate = body.getString("endDate");
                noReasons = body.getBoolean("noReasons");
                this.eventService.getEventsByStudent(eventTypes, students, structure, justified, reasonsId, massmailed, startDate, endDate, noReasons, BusResponseHandler.busArrayHandler(message));
                break;
            case "get-reasons":
                structure = body.getString("structure");
                this.reasonService.fetchReason(structure, BusResponseHandler.busArrayHandler(message));
                break;
            case "get-settings":
                structure = body.getString("structure");
                this.settingsService.retrieve(structure, BusResponseHandler.busResponseHandler(message));
                break;
            default:
                message.reply(new JsonObject()
                        .put("status", "error")
                        .put("message", "Invalid action."));
        }
    }
}
