package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.service.AbsenceService;
import fr.openent.presences.service.impl.DefaultAbsenceService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Post;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserUtils;

public class AbsenceController extends ControllerHelper {

    private EventBus eb;
    private AbsenceService absenceService;

    public AbsenceController(EventBus eb) {
        super();
        this.eb = eb;
        this.absenceService = new DefaultAbsenceService(eb);

    }

    @Post("/absence")
    @ApiDoc("Create absence")
    @SecuredAction(Presences.CREATE_EVENT)
    public void postEvent(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, event -> {
            if (!isValidAbsenceBody(event)) {
                badRequest(request);
                return;
            }

            UserUtils.getUserInfos(eb, request, user -> {
                absenceService.create(event, user, either -> {
                    if (either.isLeft()) {
                        log.error("[Presences@AbsenceController] failed to create absent or events", either.left().getValue());
                        renderError(request);
                    } else {
                        JsonObject res = new JsonObject().put("events", either.right().getValue());
                        renderJson(request, res, 201);
                    }
                });
            });
        });
    }

    private Boolean isValidAbsenceBody(JsonObject event) {
        return event.containsKey("student_id") && event.containsKey("start_date") && event.containsKey("end_date");
    }
}
