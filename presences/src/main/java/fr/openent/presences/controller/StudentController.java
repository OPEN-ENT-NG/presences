package fr.openent.presences.controller;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.security.StudentEventsViewRight;
import fr.openent.presences.security.StudentsEventsViewRight;
import fr.openent.presences.service.CommonPresencesServiceFactory;
import fr.openent.presences.service.EventStudentService;
import fr.openent.presences.service.impl.DefaultEventStudentService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.util.List;

public class StudentController extends ControllerHelper {

    private final EventStudentService eventStudentService;

    public StudentController(CommonPresencesServiceFactory commonPresencesServiceFactory) {
        super();
        this.eventStudentService = commonPresencesServiceFactory.eventStudentService();
    }

    @Get("/students/:id/events")
    @ApiDoc("get events by types for a student")
    @ResourceFilter(StudentEventsViewRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getEventsStudent(HttpServerRequest request) {
        String structureId = request.params().get(Field.STRUCTURE_ID);
        String studentId = request.params().get(Field.ID);
        String limit = request.params().get(Field.LIMIT);
        String offset = request.params().get(Field.OFFSET);
        String start = request.params().get(Field.START_AT);
        String end = request.params().get(Field.END_AT);
        List<String> types = request.params().getAll(Field.TYPE);


        eventStudentService.get(structureId, studentId, types, start, end, limit, offset)
                .onSuccess(result -> renderJson(request, result))
                .onFailure(error -> {
                    log.error(error.getMessage());
                    renderError(request);
                });
    }

    @Post("structures/:structureId/students/events")
    @ApiDoc("get events by types for a student")
    @ResourceFilter(StudentsEventsViewRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @SuppressWarnings("unchecked")
    public void getEventsStudents(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "studentsEvents", body -> {
            String structureId = request.getParam(Field.STRUCTUREID);
            List<String> studentIds = body.getJsonArray(Field.STUDENT_IDS, new JsonArray()).getList();
            String limit = body.getString(Field.LIMIT);
            String offset = body.getString(Field.OFFSET);
            String start = body.getString(Field.START_AT);
            String end = body.getString(Field.END_AT);
            List<String> types = body.getJsonArray(Field.TYPES, new JsonArray()).getList();

            eventStudentService.get(structureId, studentIds, types, start, end, limit, offset)
                    .onSuccess(result -> renderJson(request, result))
                    .onFailure(error -> {
                        log.error(error.getMessage());
                        renderError(request);
                    });
        });

    }
}
