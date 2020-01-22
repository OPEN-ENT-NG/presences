package fr.openent.presences.controller;

import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.security.AlertFilter;
import fr.openent.presences.security.DeleteAlertFilter;
import fr.openent.presences.service.AlertService;
import fr.openent.presences.service.impl.DefaultAlertService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;

import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class AlertController extends ControllerHelper {
    private AlertService alertService = new DefaultAlertService();
    private GroupService groupService;

    public AlertController(EventBus eb) {
        groupService = new DefaultGroupService(eb);
    }

    @Delete("/alerts")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(DeleteAlertFilter.class)
    @Trace(Actions.ALERT_DELETION)
    @ApiDoc("Get given structure")
    public void delete(HttpServerRequest request) {
        List<String> alerts = request.params().getAll("id");
        if (alerts.size() == 0) {
            badRequest(request);
        } else {
            alertService.delete(alerts, defaultResponseHandler(request));
        }
    }

    @Get("/structures/:id/alerts/summary")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AlertFilter.class)
    @ApiDoc("Get given structure")
    public void get(HttpServerRequest request) {
        alertService.getSummary(request.getParam("id"), defaultResponseHandler(request));
    }

    @Get("/structures/:id/alerts")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AlertFilter.class)
    @ApiDoc("Get given structure")
    public void getStudentAlert(HttpServerRequest request) {
        List<String> types = request.params().getAll("type");
        List<String> students = request.params().getAll("student");
        List<String> classes = request.params().getAll("class");
        if (types.size() == 0) {
            badRequest(request);
            return;
        }

        if (classes.isEmpty())
            alertService.getAlertsStudents(request.getParam("id"), types, students, arrayResponseHandler(request));
        else {
            groupService.getGroupStudents(classes, handler -> {
                if (handler.isLeft()) {
                    renderError(request);
                    return;
                }

                JsonArray users = handler.right().getValue();
                for (int i = 0; i < users.size(); i++) students.add(users.getJsonObject(i).getString("id"));
                alertService.getAlertsStudents(request.getParam("id"), types, students, arrayResponseHandler(request));
            });
        }
    }
}


