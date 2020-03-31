package fr.openent.incidents.controller;

import fr.openent.incidents.service.IncidentsService;
import fr.openent.incidents.service.InitService;
import fr.openent.incidents.service.impl.DefaultIncidentsService;
import fr.openent.incidents.service.impl.DefaultInitService;
import fr.wseduc.bus.BusAddress;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.entcore.common.bus.BusResponseHandler;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.request.JsonHttpServerRequest;

import java.util.List;

public class EventBusController extends ControllerHelper {
    private IncidentsService incidentsService;
    private InitService initService = new DefaultInitService();

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
            case "init-get-incident-type-statement":
                initService.getInitIncidentTypesStatement(new JsonHttpServerRequest(body.getJsonObject("request", new JsonObject())), body.getString("structure"), BusResponseHandler.busResponseHandler(message));
                break;
            case "init-get-incident-places-statement":
                initService.getInitIncidentPlacesStatement(new JsonHttpServerRequest(body.getJsonObject("request", new JsonObject())), body.getString("structure"), BusResponseHandler.busResponseHandler(message));
                break;
            case "init-get-incident-protagonist-type-statement":
                initService.getInitIncidentProtagonistsStatement(new JsonHttpServerRequest(body.getJsonObject("request", new JsonObject())), body.getString("structure"), BusResponseHandler.busResponseHandler(message));
                break;
            case "init-get-incident-seriousness-statement":
                initService.getInitIncidentSeriousnessStatement(new JsonHttpServerRequest(body.getJsonObject("request", new JsonObject())), body.getString("structure"), BusResponseHandler.busResponseHandler(message));
                break;
            case "init-get-incident-partner-statement":
                initService.getInitIncidentPartnerStatement(new JsonHttpServerRequest(body.getJsonObject("request", new JsonObject())), body.getString("structure"), BusResponseHandler.busResponseHandler(message));
                break;
            case "init-get-incident-punishment-type":
                initService.getInitIncidentPunishmentType(new JsonHttpServerRequest(body.getJsonObject("request", new JsonObject())), body.getString("structure"), BusResponseHandler.busResponseHandler(message));
                break;
            default:
                message.reply(new JsonObject()
                        .put("status", "error")
                        .put("message", "Invalid action."));
        }
    }
}
