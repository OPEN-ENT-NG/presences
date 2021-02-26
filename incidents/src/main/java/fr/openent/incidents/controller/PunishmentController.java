package fr.openent.incidents.controller;

import fr.openent.incidents.constants.Actions;
import fr.openent.incidents.export.PunishmentsCSVExport;
import fr.openent.incidents.security.punishment.PunishmentsManageRight;
import fr.openent.incidents.security.punishment.PunishmentsViewRight;
import fr.openent.incidents.service.PunishmentService;
import fr.openent.incidents.service.impl.DefaultPunishmentService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PunishmentController extends ControllerHelper {

    private final PunishmentService punishmentService;

    public PunishmentController() {
        super();
        punishmentService = new DefaultPunishmentService(eb);
    }

    @Get("/punishments")
    @ApiDoc("Retrieve punishments types")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PunishmentsViewRight.class)
    public void get(final HttpServerRequest request) {
        if (!request.params().contains("structure_id")) {
            badRequest(request);
            return;
        }
        UserUtils.getUserInfos(eb, request, user -> {
            punishmentService.get(user, request.params(), false, result -> {
                if (result.failed()) {
                    log.error(result.cause().getMessage());
                    renderError(request);
                    return;
                }
                renderJson(request, result.result());
            });
        });
    }

    @Post("/punishments")
    @ApiDoc("Create punishment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PunishmentsManageRight.class)
    @Trace(Actions.INCIDENT_PUNISHMENT_CREATION)
    public void post(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, body -> {
            UserUtils.getUserInfos(eb, request, user -> {
                punishmentService.create(user, body, result -> {
                    if (result.failed()) {
                        log.error(result.cause().getMessage());
                        renderError(request);
                        return;
                    }
                    renderJson(request, result.result());
                });
            });
        });
    }

    @Put("/punishments")
    @ApiDoc("Update punishment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PunishmentsManageRight.class)
    @Trace(Actions.INCIDENT_PUNISHMENT_UPDATE)
    public void put(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, body -> {
            UserUtils.getUserInfos(eb, request, user -> {
                punishmentService.update(user, body, result -> {
                    if (result.failed()) {
                        log.error(result.cause().getMessage());
                        renderError(request);
                        return;
                    }
                    renderJson(request, result.result());
                });
            });
        });
    }

    @Delete("/punishments")
    @ApiDoc("Delete punishment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PunishmentsManageRight.class)
    @Trace(Actions.INCIDENT_PUNISHMENT_DELETE)
    public void delete(final HttpServerRequest request) {
        if (!request.params().contains("id") && !request.params().contains("structureId")) {
            badRequest(request);
            return;
        }
        UserUtils.getUserInfos(eb, request, user -> {
            punishmentService.delete(user, request.params(), result -> {
                if (result.failed()) {
                    log.error(result.cause().getMessage());
                    renderError(request);
                    return;
                }
                renderJson(request, result.result());
            });
        });
    }

    @Get("/punishments/export")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PunishmentsViewRight.class)
    @ApiDoc("Export punishments")
    public void exportPunishments(HttpServerRequest request) {
        if (!request.params().contains("structure_id")) {
            badRequest(request);
            return;
        }
        UserUtils.getUserInfos(eb, request, user -> {
            punishmentService.get(user, request.params(), false, result -> {
                if (result.failed()) {
                    log.error("[PunishmentsController@exportPunishments] Failed to export Punishments" + result.cause().getMessage());
                    renderError(request);
                    return;
                }
                JsonArray punishments = result.result().getJsonArray("all");


                List<String> csvHeaders = new ArrayList<>(Arrays.asList(
                        "incidents.punishments.csv.header.student.lastName",
                        "incidents.punishments.csv.header.student.firstName",
                        "incidents.punishments.csv.header.classname",
                        "incidents.punishments.csv.header.type",
                        "incidents.punishments.csv.header.start.date",
                        "incidents.punishments.csv.header.end.date",
                        "incidents.punishments.csv.header.slots",
                        "incidents.punishments.csv.header.description",
                        "incidents.punishments.csv.header.owner",
                        "incidents.punishments.csv.header.processed"));
                PunishmentsCSVExport pce = new PunishmentsCSVExport(punishments);
                pce.setRequest(request);
                pce.setHeader(csvHeaders);
                pce.export();
            });
        });
    }

    @Post("/punishments/students/absences")
    @SuppressWarnings("unchecked")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PunishmentsViewRight.class)
    @ApiDoc("Get students absences in a period")
    public void getStudentsAbsences(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "punishmentAbsence", event -> {
            String starDate = event.getString("startAt");
            String endDate = event.getString("endAt");
            List<String> studentIds = event.getJsonArray("studentIds", new JsonArray()).getList();
            punishmentService.getAbsencesByStudentIds(studentIds, starDate, endDate, result -> {
                if (result.failed()) {
                    renderError(request);
                    return;
                }

                renderJson(request, new JsonObject().put("all", result.result()));
            });
        });
    }


}
