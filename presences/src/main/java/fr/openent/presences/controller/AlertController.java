package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.service.AlertService;
import fr.openent.presences.service.impl.DefaultAlertService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.sql.Sql;

import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class AlertController extends ControllerHelper {
    private AlertService alertService = new DefaultAlertService();

    @Get("/structures/:id/alerts/summary")
    @SecuredAction(Presences.ALERTS_WIDGET)
    @ApiDoc("Get given structure")
    public void get(HttpServerRequest request) {
        alertService.getSummary(request.getParam("id"), defaultResponseHandler(request));
    }

    @Get("/structures/:id/alerts")
//    @SecuredAction(Presences.ALERTS_WIDGET)
    @ApiDoc("Get given structure")
    public void getStudentAlert(HttpServerRequest request) {
        List<String> types = request.params().getAll("type");
        Sql.listPrepared(types);
        new JsonArray().addAll(new JsonArray(types));
        alertService.getAlertsStudents(request.getParam("id"), types, arrayResponseHandler(request));
    }
}


