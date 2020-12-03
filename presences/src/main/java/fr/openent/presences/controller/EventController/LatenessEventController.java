package fr.openent.presences.controller.EventController;

import fr.openent.presences.constants.Actions;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.model.Event.EventBody;
import fr.openent.presences.security.CreateEventRight;
import fr.openent.presences.service.LatenessEventService;
import fr.openent.presences.service.impl.DefaultLatenessEventService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.user.UserUtils;

public class LatenessEventController extends ControllerHelper {

    private final LatenessEventService latenessService;

    public LatenessEventController(EventBus eb) {
        super();
        this.latenessService = new DefaultLatenessEventService(eb);
    }

    @Post("/events/lateness")
    @ApiDoc("Create event action")
    @ResourceFilter(CreateEventRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.EVENT_CREATION)
    public void createLatenessEvent(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            RequestUtils.bodyToJson(request, pathPrefix + "event", event -> {
                EventBody eventBody = new EventBody(event);
                this.latenessService.create(eventBody, user, DefaultResponseHandler.defaultResponseHandler(request));
            });
        });

    }

    @Put("/events/:id/lateness")
    @ApiDoc("Get given structure")
    @ResourceFilter(CreateEventRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.EVENT_UPDATE)
    public void updateLatenessEvent(final HttpServerRequest request) {
        try {
            Integer eventId = Integer.parseInt(request.getParam("id"));
            RequestUtils.bodyToJson(request, pathPrefix + "event", event -> {
                EventBody eventBody = new EventBody(event);
                if (!EventType.DEPARTURE.getType().equals(eventBody.getTypeId())) {
                    badRequest(request);
                    return;
                }
                this.latenessService.update(eventId, eventBody, DefaultResponseHandler.defaultResponseHandler(request));
            });
        } catch (ClassCastException e) {
            log.error("[Presences@LatenessController::updateLatenessEvent] Failed to cast lateness event identifier");
            badRequest(request);
        }
    }

}
