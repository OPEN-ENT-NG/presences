package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.security.CreateEventRight;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.impl.DefaultEventService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class EventController extends ControllerHelper {

    private EventService eventService;

    public EventController() {
        super();
        this.eventService = new DefaultEventService();
    }

    @Post("/events")
    @ApiDoc("Create event")
    @SecuredAction(Presences.CREATE_EVENT)
    public void postEvent(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "event", event -> {
            if (!isValidBody(event)) {
                badRequest(request);
                return;
            }
            UserUtils.getUserInfos(eb, request, user -> {
                eventService.create(event, user, either -> {
                    if (either.isLeft()) {
                        log.error("[Presences@EventController] failed to create event", either.left().getValue());
                        renderError(request);
                    } else {
                        renderJson(request, either.right().getValue(), 201);
                    }
                });
            });
        });
    }

    @Put("/events/:id")
    @ApiDoc("Update given event")
    @ResourceFilter(CreateEventRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void putEvent(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "event", event -> {
            if (!isValidBody(event)
                    && !EventType.LATENESS.getType().equals(event.getInteger("type_id"))
                    && !EventType.DEPARTURE.getType().equals(event.getInteger("type_id"))) {
                badRequest(request);
                return;
            }

            try {
                Integer eventId = Integer.parseInt(request.getParam("id"));
                eventService.update(eventId, event, defaultResponseHandler(request));
            } catch (ClassCastException e) {
                log.error("[Presences@EventController] Failed to cast event identifier");
                badRequest(request);
            }
        });
    }

    private Boolean isValidBody(JsonObject event) {
        boolean valid = event.containsKey("student_id") && event.containsKey("type_id") && event.containsKey("register_id");
        Integer type = event.getInteger("type_id");
        if (!EventType.ABSENCE.getType().equals(type)) {
            valid = valid && event.containsKey("start_date") && event.containsKey("end_date");
        }
        return valid;
    }

    @Delete("/events/:id")
    @ApiDoc("Delete given event")
    @ResourceFilter(CreateEventRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void deleteEvent(HttpServerRequest request) {
        try {
            Integer eventId = Integer.parseInt(request.getParam("id"));
            eventService.delete(eventId, defaultResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("[Presences@EventController] Failed to cast event identifier");
            badRequest(request);
        }
    }
}
