package fr.openent.incidents.controller;

import fr.openent.incidents.constants.Actions;
import fr.openent.incidents.enums.*;
import fr.openent.incidents.export.PunishmentsCSVExport;
import fr.openent.incidents.security.punishment.PunishmentsManageRight;
import fr.openent.incidents.security.punishment.PunishmentsViewRight;
import fr.openent.incidents.service.*;
import fr.openent.incidents.service.impl.DefaultPunishmentService;
import fr.openent.incidents.worker.*;
import fr.openent.presences.common.export.*;
import fr.openent.presences.common.helper.*;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.*;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.*;
import fr.wseduc.webutils.http.*;
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
    private final ExportData exportData;

    public PunishmentController(CommonIncidentsServiceFactory serviceFactory) {
        super();
        this.punishmentService = serviceFactory.punishmentService();
        this.exportData = serviceFactory.exportData();
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
            String id = request.params().get(Field.ID);
            String groupedPunishmentId = request.params().get(Field.GROUPED_PUNISHMENT_ID);
            String structureId = request.params().get(Field.STRUCTURE_ID);
            String startAt = request.params().get(Field.START_AT);
            String endAt = request.params().get(Field.END_AT);
            List<String> studentIds = request.params().getAll(Field.STUDENT_ID);
            List<String> groupIds = request.params().getAll(Field.GROUP_ID);
            List<String> typeIds = request.params().getAll(Field.TYPEID);
            List<String> processStates = request.params().getAll(Field.PROCESS);
            String order = request.params().contains(Field.ORDER) ? request.getParam(Field.ORDER) : Field.DATE;
            boolean reverse = request.params().contains(Field.REVERSE) && Boolean.parseBoolean(request.getParam(Field.REVERSE));
            String page = request.params().get(Field.PAGE);
            String limit = request.params().get(Field.LIMIT);
            String offset = request.params().get(Field.OFFSET);

            punishmentService.get(user, id, groupedPunishmentId, structureId, startAt, endAt, studentIds, groupIds, typeIds,
                            processStates, false, order, reverse, page, limit, offset)
                    .onFailure(error -> renderError(request))
                    .onSuccess(result -> renderJson(request, result));
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
        if ((!request.params().contains("id") || !request.params().contains("grouped_punishment_id")) && !request.params().contains("structureId")) {
            badRequest(request);
            return;
        }

        String structureId = request.params().get(Field.STRUCTUREID);
        String punishmentId = request.params().get(Field.ID);
        String groupedPunishmentId = request.params().get(Field.GROUPED_PUNISHMENT_ID);

        UserUtils.getUserInfos(eb, request, user -> {
            request.pause();
            punishmentService.delete(user, structureId, punishmentId, groupedPunishmentId)
                    .onSuccess(result -> {
                        request.resume();
                        renderJson(request, result);
                    })
                    .onFailure(error -> {
                        request.resume();
                        log.error(error.getMessage());
                        renderError(request);
                    });
        });
    }

    @Get("/punishments/export")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PunishmentsViewRight.class)
    @ApiDoc("Export punishments")
    public void exportPunishments(HttpServerRequest request) {
        if (!request.params().contains(Field.STRUCTURE_ID)) {
            badRequest(request);
            return;
        }
        UserUtils.getUserInfos(eb, request, user -> {

            String domain = Renders.getHost(request);
            String locale = I18n.acceptLanguage(request);

            JsonObject params = new JsonObject()
                    .put(Field.STRUCTURE_ID, request.params().get(Field.STRUCTURE_ID))
                    .put(Field.START_AT, request.params().get(Field.START_AT))
                    .put(Field.END_AT, request.params().get(Field.END_AT))
                    .put(Field.STUDENT_ID, request.params().getAll(Field.STUDENT_ID))
                    .put(Field.GROUP_ID, request.params().getAll(Field.GROUP_ID))
                    .put(Field.TYPE_ID, request.params().getAll(Field.TYPE_ID))
                    .put(Field.PROCESS, request.params().getAll(Field.PROCESS))
                    .put(Field.ORDER, request.params().get(Field.ORDER))
                    .put(Field.REVERSE, request.params().get(Field.REVERSE))
                    .put(Field.USER, UserInfosHelper.toJSON(user))
                    .put(Field.LOCALE, locale)
                    .put(Field.DOMAIN, domain);

            exportData.export(IncidentsExportWorker.class.getName(), ExportActions.EXPORT_PUNISHMENTS,
                    ExportType.CSV.type(), params);

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
                PunishmentsCSVExport pce = new PunishmentsCSVExport(punishments, domain, locale);
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
