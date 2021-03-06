package fr.openent.incidents.controller;

import fr.openent.incidents.security.StudentEventsViewRight;
import fr.openent.incidents.service.EventStudentService;
import fr.openent.incidents.service.impl.DefaultEventStudentService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

public class StudentController extends ControllerHelper {

    private EventStudentService eventStudentService;

    public StudentController(EventBus eb) {
        super();
        this.eventStudentService = new DefaultEventStudentService(eb);
    }

    @Get("/students/:id/events")
    @ApiDoc("get events by types for a student")
    @ResourceFilter(StudentEventsViewRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getEventsStudent(HttpServerRequest request) {
        eventStudentService.get(request.params(), result -> {
            if (result.failed()) {
                log.error(result.cause().getMessage());
                renderError(request);
                return;
            }
            renderJson(request, result.result());
        });
    }
}
