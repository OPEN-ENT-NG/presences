package fr.openent.incidents.controller;

import fr.openent.incidents.service.IncidentsService;
import fr.openent.incidents.service.InitService;
import fr.openent.incidents.service.PunishmentService;
import fr.openent.incidents.service.PunishmentTypeService;
import fr.openent.incidents.service.impl.DefaultIncidentsService;
import fr.openent.incidents.service.impl.DefaultInitService;
import fr.openent.incidents.service.impl.DefaultPunishmentService;
import fr.openent.incidents.service.impl.DefaultPunishmentTypeService;
import fr.wseduc.bus.BusAddress;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.bus.BusResponseHandler;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.request.JsonHttpServerRequest;

import java.util.List;

public class EventBusController extends ControllerHelper {
    private final IncidentsService incidentsService;
    private final InitService initService = new DefaultInitService();
    private final PunishmentTypeService punishmentTypeService = new DefaultPunishmentTypeService();
    private final PunishmentService punishmentService;

    public EventBusController(EventBus eb) {
        incidentsService = new DefaultIncidentsService(eb);
        punishmentService = new DefaultPunishmentService(eb);
    }

    @BusAddress("fr.openent.incidents")
    @SuppressWarnings("unchecked")
    public void bus(final Message<JsonObject> message) {
        JsonObject body = message.body();
        String action = body.getString("action");
        String startAt;
        String endAt;
        List<String> studentIds;
        List<String> punishmentsIds;
        List<Integer> punishmentTypeIds;
        String eventType;
        Boolean processed;
        Boolean massmailed;
        String structure;
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
            case "get-count-punishment-by-student":
                structure = body.getString("structure");
                startAt = body.getString("start_at");
                endAt = body.getString("end_at");
                studentIds = body.getJsonArray("studentIds", new JsonArray()).getList();
                punishmentTypeIds = body.getJsonArray("punishmentTypeIds", new JsonArray()).getList();
                processed = body.getBoolean("processed");
                massmailed = body.getBoolean("massmailed");
                this.punishmentService.getPunishmentCountByStudent(structure, startAt, endAt, studentIds, punishmentTypeIds,
                        processed, massmailed, BusResponseHandler.busArrayHandler(message));
                break;
            case "get-punishment-by-student":
                structure = body.getString("structure");
                startAt = body.getString("start_at");
                endAt = body.getString("end_at");
                studentIds = body.getJsonArray("students", new JsonArray()).getList();
                punishmentTypeIds = (body.getJsonArray("punishmentTypeIds") != null) ?
                        body.getJsonArray("punishmentTypeIds", new JsonArray()).getList() : null;
                eventType = body.getString("eventType");
                processed = body.getBoolean("processed");
                massmailed = body.getBoolean("massmailed");
                this.punishmentService.getPunishmentByStudents(structure, startAt, endAt, studentIds, punishmentTypeIds,
                        eventType, processed, massmailed, BusResponseHandler.busArrayHandler(message));
                break;
            case "update-punishments-massmailing":
                punishmentsIds = body.getJsonArray("punishmentsIds", new JsonArray()).getList();
                massmailed = body.getBoolean("massmailed");
                this.punishmentService.updatePunishmentMassmailing(punishmentsIds, massmailed, BusResponseHandler.busResponseHandler(message));
                break;
            case "get-punishment-type":
                structure = body.getString("structure");
                this.punishmentTypeService.get(structure, BusResponseHandler.busArrayHandler(message));
                break;
            default:
                message.reply(new JsonObject()
                        .put("status", "error")
                        .put("message", "Invalid action."));
        }
    }
}
