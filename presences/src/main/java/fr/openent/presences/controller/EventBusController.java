package fr.openent.presences.controller;

import fr.openent.presences.common.bus.BusResultHandler;
import fr.openent.presences.service.AbsenceService;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.ReasonService;
import fr.openent.presences.service.SettingsService;
import fr.openent.presences.service.impl.DefaultAbsenceService;
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
import org.entcore.common.user.UserInfos;

import java.util.List;

public class EventBusController extends ControllerHelper {

    private final EventService eventService;
    private final ReasonService reasonService = new DefaultReasonService();
    private final SettingsService settingsService = new DefaultSettingsService();
    private final AbsenceService absenceService;
    private UserInfos user;

    public EventBusController(EventBus eb) {
        this.eventService = new DefaultEventService(eb);
        this.absenceService = new DefaultAbsenceService(eb);
    }

    @BusAddress("fr.openent.presences")
    public void bus(final Message<JsonObject> message) {
        JsonObject body = message.body();
        String action = body.getString("action");
        String userId;
        Integer eventType;
        Boolean justified;
        List<String> students;
        String structure;
        Integer startAt;
        List<Integer> reasonsId;
        Boolean massmailed;
        Boolean compliance;
        String startDate;
        String endDate;
        boolean noReasons;
        String recoveryMethod;
        Boolean regularized;
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
                recoveryMethod = body.getString("recoveryMethod");
                regularized = body.getBoolean("regularized");
                this.eventService.getCountEventByStudent(eventType, students, structure, justified, startAt, reasonsId,
                        massmailed, startDate, endDate, noReasons, recoveryMethod, regularized, BusResponseHandler.busArrayHandler(message));
                break;
            case "get-events-by-student":
                eventType = body.getInteger("eventType");
                justified = body.getBoolean("justified");
                students = body.getJsonArray("students", new JsonArray()).getList();
                structure = body.getString("structure");
                reasonsId = body.getJsonArray("reasonsId", new JsonArray()).getList();
                massmailed = body.getBoolean("massmailed");
                compliance = body.getBoolean("compliance");
                startDate = body.getString("startDate");
                endDate = body.getString("endDate");
                noReasons = body.getBoolean("noReasons");
                recoveryMethod = body.getString("recoveryMethod");
                regularized = body.getBoolean("regularized");
                this.eventService.getEventsByStudent(eventType, students, structure, justified, reasonsId, massmailed, compliance, startDate, endDate, noReasons, recoveryMethod, regularized, BusResponseHandler.busArrayHandler(message));
                break;
            case "get-absences":
                students = body.getJsonArray("studentIds", new JsonArray()).getList();
                startDate = body.getString("startDate");
                endDate = body.getString("endDate");
                absenceService.getAbsencesBetweenDates(startDate, endDate, students, BusResponseHandler.busArrayHandler(message));
                break;
            case "create-absences":
                students = body.getJsonArray("studentIds", new JsonArray()).getList();
                startDate = body.getString("start_date");
                endDate = body.getString("end_date");
                userId = body.getString("userId");
                absenceService.create(body, students, userId, null, result -> {
                    if (result.failed()) {
                        message.reply(new JsonObject()
                                .put("status", "error")
                                .put("message", result.cause().getMessage()));
                        return;
                    }
                    absenceService.afterPersist(students, body.getString("structure_id"), startDate, endDate, userId,
                            body.getBoolean("editEvents", false), BusResultHandler.busResponseHandler(message));
                });
                break;
            case "update-absence":
                userId = body.getString("userId");
                absenceService.update(body.getLong("absenceId"), body, userId, body.getBoolean("editEvents", false),
                        BusResponseHandler.busResponseHandler(message));
                break;
            case "delete-absence":
                absenceService.delete(body.getInteger("absenceId"), BusResponseHandler.busResponseHandler(message));
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
