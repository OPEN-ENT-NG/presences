package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.constants.Alerts;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.export.AlertsCSVExport;
import fr.openent.presences.security.Alert.AlertStudentNumber;
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
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class AlertController extends ControllerHelper {
    private AlertService alertService = new DefaultAlertService();
    private GroupService groupService;

    public AlertController(EventBus eb) {
        groupService = new DefaultGroupService(eb);
    }

    @Delete("/structures/:id/alerts")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(DeleteAlertFilter.class)
    @Trace(Actions.ALERT_DELETION)
    @ApiDoc("reset alerts")
    public void delete(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "alertDelete", body -> {
            final Map<String, List<String>> deletedAlertMap = body.getJsonArray(Field.DELETED_ALERT).stream().map(JsonObject.class::cast)
                    .collect(Collectors.groupingBy(jsonObject -> jsonObject.getString(Field.STUDENT_ID)))
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, stringListEntry -> stringListEntry.getValue().stream()
                            .map(jsonObject -> jsonObject.getString(Field.TYPE))
                            .filter(Alerts.ALERT_LIST::contains)
                            .collect(Collectors.toList())));

            alertService.delete(request.getParam(Field.ID), deletedAlertMap, body.getString(Field.START_AT), body.getString(Field.END_AT), "00:00:00", "23:59:59")
                    .onSuccess(result -> renderJson(request, result))
                    .onFailure(err -> renderError(request));
        });
    }

    @Get("/structures/:id/alerts/summary")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AlertFilter.class)
    @ApiDoc("Get given structure")
    public void get(HttpServerRequest request) {
        alertService.getSummary(request.getParam(Field.ID))
                .onSuccess(result -> renderJson(request, result))
                .onFailure(err -> renderError(request));
    }

    @Get("/structures/:id/alerts")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AlertFilter.class)
    @ApiDoc("Get given structure")
    public void getStudentsAlerts(HttpServerRequest request) {
        List<String> types = request.params().getAll(Field.TYPE);
        List<String> students = request.params().getAll(Field.STUDENT_ID);
        List<String> classes = request.params().getAll(Field.CLASS);
        String startAt = request.params().get(Field.START_AT);
        String endAt = request.params().get(Field.END_AT);
        if (types.size() == 0) {
            badRequest(request);
            return;
        }
        getAlerts(request, types, students, classes, startAt, endAt, arrayResponseHandler(request));
    }

    private void getAlerts(HttpServerRequest request, List<String> types, List<String> students, List<String> classes,
                           String startDate, String endDate, Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> groupStudentFuture = (classes.isEmpty()) ? Future.succeededFuture(new JsonArray()) : groupService.getGroupStudents(classes);
        groupStudentFuture.compose(users -> {
                    users.stream()
                            .map(o -> (JsonObject)o)
                            .map(jsonObject -> jsonObject.getString(Field.ID))
                            .forEach(students::add);
                    return alertService.getAlertsStudents(request.getParam(Field.ID), types, students, startDate, endDate, "00:00:00", "23:59:59");
                })
                .onSuccess(alert -> handler.handle(new Either.Right<>(alert)))
                .onFailure(error -> {
                    log.error(String.format("[Presences@AlertController::getAlerts] Failed to retrieve alerts info. %s", error.getMessage()));
                    handler.handle(new Either.Left<>(error.getMessage()));
                });
    }

    @Get("/structures/:id/students/:studentId/alerts")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AlertStudentNumber.class)
    @ApiDoc("Get student alert number by given type with the corresponding threshold")
    public void getStudentAlertNumberWithThreshold(HttpServerRequest request) {
        String type = request.params().get(Field.TYPE);
        if (type == null || !Alerts.ALERT_LIST.contains(type)) {
            badRequest(request);
            return;
        }

        alertService.getStudentAlertNumberWithThreshold(request.getParam(Field.ID), request.getParam(Field.STUDENTID),
                type, defaultResponseHandler(request)
        );
    }

    @Delete("/structures/:id/students/:studentId/alerts/reset")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AlertFilter.class)
    @Trace(Actions.ALERT_DELETION)
    @ApiDoc("Reset student alert count")
    public void resetStudentAlertsCount(HttpServerRequest request) {
        String type = request.params().get(Field.TYPE);
        if (type == null || !Alerts.ALERT_LIST.contains(type)) {
            badRequest(request);
            return;
        }

        alertService.resetStudentAlertsCount(request.getParam(Field.ID), request.getParam(Field.STUDENTID), type)
                .onSuccess(res -> renderJson(request, res))
                .onFailure(unused -> renderError(request));
    }


    @Get("/structures/:id/alerts/export")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AlertFilter.class)
    @ApiDoc("Export alerts")
    public void exportAlerts(HttpServerRequest request) {
        List<String> types = request.params().getAll(Field.TYPE);
        List<String> students = request.params().getAll(Field.STUDENT);
        List<String> classes = request.params().getAll(Field.CLASS);
        if (types.size() == 0) {
            badRequest(request);
            return;
        }
        getAlerts(request, types, students, classes, null, null, event -> {
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

    @SecuredAction(value = Presences.ALERTS_STUDENT_NUMBER, type = ActionType.WORKFLOW)
    public void getAlertsStudentsNumberRight(final HttpServerRequest request) {
    }
}


