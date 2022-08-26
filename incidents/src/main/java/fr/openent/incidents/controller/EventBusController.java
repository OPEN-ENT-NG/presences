package fr.openent.incidents.controller;

import fr.openent.incidents.service.IncidentsService;
import fr.openent.incidents.service.InitService;
import fr.openent.incidents.service.PunishmentService;
import fr.openent.incidents.service.PunishmentTypeService;
import fr.openent.incidents.service.impl.DefaultIncidentsService;
import fr.openent.incidents.service.impl.DefaultInitService;
import fr.openent.incidents.service.impl.DefaultPunishmentService;
import fr.openent.incidents.service.impl.DefaultPunishmentTypeService;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.InitTypeEnum;
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
        String action = body.getString(Field.ACTION);
        String startAt;
        String endAt;
        List<String> studentIds;
        List<String> punishmentsIds;
        List<Integer> punishmentTypeIds;
        String eventType;
        Boolean processed;
        Boolean massmailed;
        String structure = body.getString(Field.STRUCTURE, null);
        JsonHttpServerRequest request = new JsonHttpServerRequest(body.getJsonObject(Field.REQUEST, new JsonObject()));
        InitTypeEnum initTypeEnum = InitTypeEnum.getInitType(body.getInteger(Field.INITTYPE, 2));
        switch (action) {
            case "get-incidents-users-range":
                String startDate = body.getString(Field.STARTDATE);
                String endDate = body.getString(Field.ENDDATE);
                List<String> users = body.getJsonArray(Field.USERS).getList();
                incidentsService.get(startDate, endDate, users, BusResponseHandler.busArrayHandler(message));
                break;
            case "init-get-incident-type-statement":
                initService.getInitIncidentTypesStatement(request, structure, initTypeEnum, BusResponseHandler.busResponseHandler(message));
                break;
            case "init-get-incident-places-statement":
                initService.getInitIncidentPlacesStatement(request, structure, initTypeEnum, BusResponseHandler.busResponseHandler(message));
                break;
            case "init-get-incident-protagonist-type-statement":
                initService.getInitIncidentProtagonistsStatement(request, structure, initTypeEnum, BusResponseHandler.busResponseHandler(message));
                break;
            case "init-get-incident-seriousness-statement":
                initService.getInitIncidentSeriousnessStatement(request, structure, initTypeEnum, BusResponseHandler.busResponseHandler(message));
                break;
            case "init-get-incident-partner-statement":
                initService.getInitIncidentPartnerStatement(request, structure, initTypeEnum, BusResponseHandler.busResponseHandler(message));
                break;
            case "init-get-incident-punishment-type":
                initService.getInitIncidentPunishmentType(request, structure, initTypeEnum, BusResponseHandler.busResponseHandler(message));
                break;
            case "get-count-punishment-by-student":
                startAt = body.getString(Field.START_AT);
                endAt = body.getString(Field.END_AT);
                studentIds = body.getJsonArray(Field.STUDENTIDS, new JsonArray()).getList();
                punishmentTypeIds = body.getJsonArray(Field.PUNISHMENTTYPEIDS, new JsonArray()).getList();
                processed = body.getBoolean(Field.PROCESSED);
                massmailed = body.getBoolean(Field.MASSMAILED);
                this.punishmentService.getPunishmentCountByStudent(structure, startAt, endAt, studentIds, punishmentTypeIds,
                        processed, massmailed, BusResponseHandler.busArrayHandler(message));
                break;
            case "get-punishment-by-student":
                startAt = body.getString(Field.START_AT);
                endAt = body.getString(Field.END_AT);
                studentIds = body.getJsonArray(Field.STUDENTS, new JsonArray()).getList();
                punishmentTypeIds = (body.getJsonArray(Field.PUNISHMENTTYPEIDS) != null) ?
                        body.getJsonArray(Field.PUNISHMENTTYPEIDS, new JsonArray()).getList() : null;
                eventType = body.getString(Field.EVENTTYPE);
                processed = body.getBoolean(Field.PROCESSED);
                massmailed = body.getBoolean(Field.MASSMAILED);
                this.punishmentService.getPunishmentByStudents(structure, startAt, endAt, studentIds, punishmentTypeIds,
                        eventType, processed, massmailed, BusResponseHandler.busArrayHandler(message));
                break;
            case "update-punishments-massmailing":
                punishmentsIds = body.getJsonArray(Field.PUNISHMENTTYPEIDS, new JsonArray()).getList();
                massmailed = body.getBoolean(Field.MASSMAILED);
                this.punishmentService.updatePunishmentMassmailing(punishmentsIds, massmailed, BusResponseHandler.busResponseHandler(message));
                break;
            case "get-punishment-type":
                this.punishmentTypeService.get(structure, BusResponseHandler.busArrayHandler(message));
                break;
            default:
                message.reply(new JsonObject()
                        .put(Field.STATUS, Field.ERROR)
                        .put(Field.MESSAGE, "Invalid action."));
        }
    }
}
