package fr.openent.presences.controller;

import fr.openent.presences.common.bus.BusResultHandler;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.*;
import fr.openent.presences.service.*;
import fr.openent.presences.service.impl.*;
import fr.wseduc.bus.BusAddress;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.bus.BusResponseHandler;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.request.*;
import org.entcore.common.user.UserInfos;

import java.util.*;

public class EventBusController extends ControllerHelper {

    private final EventService eventService;
    private final ReasonService reasonService = new DefaultReasonService();
    private final SettingsService settingsService = new DefaultSettingsService();
    private final AbsenceService absenceService;
    private final RegisterService registerService;

    private final InitService initService;
    private UserInfos user;

    public EventBusController(EventBus eb, CommonPresencesServiceFactory commonPresencesServiceFactory) {
        this.eventService = new DefaultEventService(eb);
        this.absenceService = new DefaultAbsenceService(eb);
        this.registerService = new DefaultRegisterService(commonPresencesServiceFactory);
        this.initService = commonPresencesServiceFactory.initService();
    }

    @BusAddress("fr.openent.presences")
    @SuppressWarnings("unchecked")
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
        List<Integer> registerIds;
        List<Integer> stateIds;
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
                this.eventService.getEventsByStudent(eventType, students, structure, reasonsId, massmailed, compliance, startDate, endDate, noReasons, recoveryMethod, regularized, BusResponseHandler.busArrayHandler(message));
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
                Integer reason = body.getInteger(Field.REASONTYPE, EventType.ABSENCE.getType());

                this.reasonService.fetchReason(structure, reason)
                        .onSuccess(res -> BusResponseHandler.busArrayHandler(message).handle(new Either.Right<>(res)))
                        .onFailure(err -> BusResponseHandler.busArrayHandler(message).handle(new Either.Left<>(err.getMessage())));
                break;
            case "get-settings":
                structure = body.getString("structure");
                this.settingsService.retrieve(structure, BusResponseHandler.busResponseHandler(message));
                break;
            case "get-registers-with-groups":
                structure = body.getString(Field.STRUCTUREID);
                registerIds = body.getJsonArray(Field.REGISTERIDS) == null ? Collections.emptyList() :
                        body.getJsonArray(Field.REGISTERIDS).getList();
                startDate = body.getString(Field.STARTAT);
                stateIds = body.getJsonArray(Field.STATEIDS) == null ? Collections.emptyList() :
                        body.getJsonArray(Field.STATEIDS).getList();
                endDate = body.getString(Field.ENDAT);
                FutureHelper.busArrayHandler(this.registerService.listWithGroups(structure, registerIds, stateIds, startDate, endDate), message);
                break;
            case "init-presences":
                structure = body.getString(Field.STRUCTUREID);
                userId = body.getString(Field.USERID);
                JsonHttpServerRequest request = new JsonHttpServerRequest(body.getJsonObject(Field.REQUEST, new JsonObject()));

                this.initService.retrieveInitializationStatus(structure)
                        .compose(status -> {
                           if (status) {
                               return Future.succeededFuture();
                           } else {
                               return this.initService.initPresences(request, structure, userId, Optional.of(InitTypeEnum.ONE_D));
                           }
                        })
                        .onFailure(err -> BusResponseHandler.busResponseHandler(message).handle(new Either.Left<>(err.getMessage())))
                        .onSuccess(res -> BusResponseHandler.busResponseHandler(message).handle(new Either.Right<>(res)));

                break;
            case "update-settings":
                structure = body.getString(Field.STRUCTUREID);
                this.settingsService.put(structure, body.getJsonObject(Field.SETTINGS))
                        .onFailure(err -> BusResponseHandler.busResponseHandler(message).handle(new Either.Left<>(err.getMessage())))
                        .onSuccess(res -> BusResponseHandler.busResponseHandler(message).handle(new Either.Right<>(res)));
                break;
            default:
                message.reply(new JsonObject()
                        .put("status", "error")
                        .put("message", "Invalid action."));
        }
    }
}
