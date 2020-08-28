package fr.openent.presences.controller;

import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.constants.Alerts;
import fr.openent.presences.export.AlertsCSVExport;
import fr.openent.presences.security.AlertFilter;
import fr.openent.presences.security.DeleteAlertFilter;
import fr.openent.presences.service.AlertService;
import fr.openent.presences.service.impl.DefaultAlertService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;

import java.util.ArrayList;
import java.util.Arrays;
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
    public void getStudentsAlerts(HttpServerRequest request) {
        List<String> types = request.params().getAll("type");
        List<String> students = request.params().getAll("student");
        List<String> classes = request.params().getAll("class");
        if (types.size() == 0) {
            badRequest(request);
            return;
        }
        getAlerts(request, types, students, classes, arrayResponseHandler(request));
    }

    private void getAlerts(HttpServerRequest request, List<String> types, List<String> students, List<String> classes,
                           Handler<Either<String, JsonArray>> handler) {
        if (classes.isEmpty())
            alertService.getAlertsStudents(request.getParam("id"), types, students, event -> {
                if (event.isLeft()) {
                    String message = "[Presences@AlertController::getAlerts] Failed to retrieve alerts info.";
                    log.error(message);
                    handler.handle(new Either.Left(message));
                } else {
                    handler.handle(new Either.Right<>(event.right().getValue()));
                }
            });
        else {
            groupService.getGroupStudents(classes, resp -> {
                if (resp.isLeft()) {
                    String message = "[Presences@AlertController::getAlerts] Failed to retrieve groupStudents info.";
                    log.error(message);
                    handler.handle(new Either.Left(message));
                    return;
                }

                JsonArray users = resp.right().getValue();
                for (int i = 0; i < users.size(); i++) students.add(users.getJsonObject(i).getString("id"));
                alertService.getAlertsStudents(request.getParam("id"), types, students, event -> {
                    if (event.isLeft()) {
                        String message = "[Presences@AlertsController::getAlerts] Failed to fetch alerts";
                        log.error(message, event.left().getValue());
                        handler.handle(new Either.Left(message));
                    } else {
                        handler.handle(new Either.Right<>(event.right().getValue()));
                    }
                });
            });
        }
    }

    @Get("/structures/:id/students/:studentId/alerts")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AlertFilter.class)
    @ApiDoc("Get student alert number by given type with the corresponding threshold")
    public void getStudentAlertNumberWithThreshold(HttpServerRequest request) {
        String type = request.params().get("type");
        if (type == null || !Alerts.ALERT_LIST.contains(type)) {
            badRequest(request);
            return;
        }

        alertService.getStudentAlertNumberWithThreshold(
                request.getParam("id"),
                request.getParam("studentId"),
                type,
                defaultResponseHandler(request)
        );
    }

    @Get("/structures/:id/alerts/export")
    @ApiDoc("Export alerts")
    public void exportAlerts(HttpServerRequest request) {
        List<String> types = request.params().getAll("type");
        List<String> students = request.params().getAll("student");
        List<String> classes = request.params().getAll("class");
        if (types.size() == 0) {
            badRequest(request);
            return;
        }
        getAlerts(request, types, students, classes, event -> {
            if (event.isLeft()) {
                log.error("[Presences@AlertsController::exportAlerts] Failed to fetch alerts", event.left().getValue());
                renderError(request);
                return;
            }

            JsonArray alerts = event.right().getValue();
            List<String> csvHeader = new ArrayList<>(Arrays.asList(
                    "presences.alerts.csv.header.student.lastName",
                    "presences.alerts.csv.header.student.firstName",
                    "presences.alerts.csv.header.student.className",
                    "presences.alerts.csv.header.type",
                    "presences.alerts.csv.header.count"
            ));
            AlertsCSVExport ace = new AlertsCSVExport(alerts);
            ace.setRequest(request);
            ace.setHeader(csvHeader);
            ace.export();
        });
    }
}


