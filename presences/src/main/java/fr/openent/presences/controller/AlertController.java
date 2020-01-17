package fr.openent.presences.controller;

import fr.openent.presences.security.AlertFilter;
import fr.openent.presences.security.DeleteAlertFilter;
import fr.openent.presences.service.AlertService;
import fr.openent.presences.service.impl.DefaultAlertService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class AlertController extends ControllerHelper {
    private AlertService alertService = new DefaultAlertService();

    @Delete("/alerts")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(DeleteAlertFilter.class)
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
        if (types.size() == 0) {
            badRequest(request);
        } else {
            alertService.getAlertsStudents(request.getParam("id"), types, arrayResponseHandler(request));
        }
    }
}


