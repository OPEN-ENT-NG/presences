package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.export.CollectiveAbsencesCSVExport;
import fr.openent.presences.security.ManageCollectiveAbsences;
import fr.openent.presences.service.CollectiveAbsenceService;
import fr.openent.presences.service.impl.DefaultCollectiveAbsenceService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.user.UserUtils;

import java.util.List;

public class CollectiveAbsenceController extends ControllerHelper {

    private final CollectiveAbsenceService collectiveService;

    public CollectiveAbsenceController() {
        super();
        this.collectiveService = new DefaultCollectiveAbsenceService(eb);
    }

    @SecuredAction(value = Presences.MANAGE_COLLECTIVE_ABSENCES)
    public void manageCollectiveAbsences(final HttpServerRequest request) {
        request.response().setStatusCode(501).end();
    }

    @Get("/structures/:structureId/absences/collectives")
    @ApiDoc("Get all collective absences")
    @ResourceFilter(ManageCollectiveAbsences.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getCollectives(final HttpServerRequest request) {

        String structureId = request.getParam("structureId");
        String startAt = request.getParam("startDate");
        String endAt = request.getParam("endDate");
        Long reasonId = request.getParam("reasonId") != null ? Long.parseLong(request.getParam("reasonId")) : null;
        Boolean regularized = request.getParam("regularized") != null ? Boolean.getBoolean(request.getParam("regularized")) : null;
        Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : null;
        List<String> audienceNames = request.params().getAll("audienceName");

        collectiveService.getCollectives(structureId, startAt, endAt, reasonId, regularized, audienceNames, page, result -> {
            if (result.failed()) {
                log.error("[Presences@CollectiveAbsenceController::getCollectives] failed to get collectives", result.cause());
                renderError(request);
            } else {
                renderJson(request, result.result());
            }
        });
    }

    @Get("/structures/:structureId/absences/collectives/:id")
    @ApiDoc("Get collective absence from id")
    @ResourceFilter(ManageCollectiveAbsences.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {

        String structureId = request.getParam("structureId");
        Long collectiveId = Long.parseLong(request.getParam("id"));

        collectiveService.get(structureId, collectiveId, result -> {
            if (result.failed()) {
                log.error("[Presences@CollectiveAbsenceController::getCollectives] failed to get collectives", result.cause());
                renderError(request);
            } else {
                renderJson(request, result.result());
            }
        });
    }

    @Post("/structures/:structureId/absences/isAbsent")
    @ApiDoc("Get absence status by concerned students on a period")
    @ResourceFilter(ManageCollectiveAbsences.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @SuppressWarnings("unchecked")
    public void getAbsencesStatus(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, event -> {
            String structureId = request.getParam("structureId");
            String starDate = event.getString("startDate");
            String endDate = event.getString("endDate");
            Long collectiveId = event.getLong("collectiveId");
            List<String> studentIds = event.getJsonArray("studentIds").getList();

            collectiveService.getAbsencesStatus(structureId, studentIds, starDate, endDate,
                    collectiveId,
                    result -> {
                        if (result.failed()) {
                            log.error("[Presences@CollectiveAbsenceController::getAbsencesStatus] failed to get status", result.cause());
                            renderError(request);
                        } else {
                            renderJson(request, result.result());
                        }
                    });
        });
    }


    @Post("/structures/:structureId/absences/collectives")
    @ApiDoc("Create collective absence")
    @ResourceFilter(ManageCollectiveAbsences.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.COLLECTIVE_ABSENCE_CREATION)
    public void post(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "collectiveAbsence", event -> {
            String structureId = request.getParam("structureId");

            UserUtils.getUserInfos(eb, request, user -> collectiveService.create(event, user, structureId, result -> {
                if (result.failed()) {
                    log.error("[Presences@AbsenceCollectiveController] failed to create collective absence", result.cause());
                    renderError(request);
                } else {
                    renderJson(request, result.result(), 201);
                }
            }));
        });
    }

    @Put("/structures/:structureId/absences/collectives/:id")
    @ApiDoc("Update collective absence")
    @ResourceFilter(ManageCollectiveAbsences.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.COLLECTIVE_ABSENCE_UPDATE)
    public void update(final HttpServerRequest request) {
        Long id = Long.parseLong(request.getParam("id"));
        String structureId = request.getParam("structureId");

        RequestUtils.bodyToJson(request, absence -> UserUtils.getUserInfos(eb, request, user -> collectiveService.update(absence, user, structureId, id, result -> {
            if (result.failed()) {
                log.error("[Presences@CollectiveAbsenceController] failed to update collective absence", result.cause());
                renderError(request);
            } else {
                renderJson(request, result.result());
            }
        })));
    }


    @Put("/structures/:structureId/absences/collectives/:id/students")
    @ApiDoc("Delete absence from collective absence")
    @ResourceFilter(ManageCollectiveAbsences.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.COLLECTIVE_ABSENCE_UPDATE)
    public void removeAbsenceFromCollectiveAbsence(final HttpServerRequest request) {

        if (!request.params().contains("structureId") || !request.params().contains("id")) {
            badRequest(request);
            return;
        }

        Long id = Long.parseLong(request.getParam("id"));
        String structureId = request.getParam("structureId");


        RequestUtils.bodyToJson(request, students -> UserUtils.getUserInfos(eb, request,
                user -> collectiveService.removeAbsenceFromCollectiveAbsence(students, structureId, id, result -> {
                    if (result.failed()) {
                        log.error("[Presences@CollectiveAbsenceController] failed to remove absence from collective absence",
                                result.cause());
                        renderError(request);
                    } else {
                        renderJson(request, result.result());
                    }
                })));


    }

    @Delete("/structures/:structureId/absences/collectives/:id")
    @ApiDoc("Delete collective absence")
    @ResourceFilter(ManageCollectiveAbsences.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.COLLECTIVE_ABSENCE_DELETION)
    public void deleteCollectiveAbsence(final HttpServerRequest request) {

        if (!request.params().contains("id") || !request.params().contains("structureId")) {
            badRequest(request);
            return;
        }

        Long id = Long.parseLong(request.getParam("id"));
        String structureId = request.getParam("structureId");

        collectiveService.delete(id, structureId, result -> {
            if (result.failed()) {
                log.error("[Presences@CollectiveAbsenceController::deleteCollectiveAbsence] failed to " +
                        "remove collective absence", result.cause());
                renderError(request);
            } else {
                renderJson(request, result.result());
            }
        });
    }

    @Get("/structures/:structureId/absences/collectives/export")
    @ApiDoc("Export collective absence")
    @ResourceFilter(ManageCollectiveAbsences.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void exportCollectiveAbsences(final HttpServerRequest request) {

        if (!request.params().contains("structureId") || !request.params().contains("startDate") ||
                !request.params().contains("endDate")) {
            badRequest(request);
            return;
        }

        String structureId = request.getParam("structureId");
        String startDate = request.getParam("startDate");
        String endDate = request.getParam("endDate");

        collectiveService.getCSV(structureId, startDate, endDate, result -> {

            if (result.failed()) {
                log.error("[CollectiveAbsenceController@exportCollectiveAbsences] Failed to export " +
                        "collective absences", result.cause());
                renderError(request);
                return;
            }

            CollectiveAbsencesCSVExport cce = new CollectiveAbsencesCSVExport(result.result());
            cce.setRequest(request);
            cce.export();
        });
    }
}
