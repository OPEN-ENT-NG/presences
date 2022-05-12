package fr.openent.presences.controller;

import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.common.security.UserInStructure;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.enums.Markers;
import fr.openent.presences.enums.WorkflowActions;
import fr.openent.presences.export.AbsencesCSVExport;
import fr.openent.presences.security.AbsenceRight;
import fr.openent.presences.security.CreateEventRight;
import fr.openent.presences.security.Manage;
import fr.openent.presences.service.*;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AbsenceController extends ControllerHelper {

    private final EventBus eb;
    private final AbsenceService absenceService;
    private final EventService eventService;
    private final CollectiveAbsenceService collectiveService;
    private final ExportAbsenceService exportAbsencesPdf;

    public AbsenceController(CommonPresencesServiceFactory commonPresencesServiceFactory) {
        super();
        this.eb = commonPresencesServiceFactory.eventBus();
        this.absenceService = commonPresencesServiceFactory.absenceService();
        this.eventService = commonPresencesServiceFactory.eventService();
        this.collectiveService = commonPresencesServiceFactory.collectiveAbsenceService();
        this.exportAbsencesPdf = commonPresencesServiceFactory.exportAbsenceService();
    }

    @Get("/absence/:id")
    @ApiDoc("Get absence from id")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getAbsenceId(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Integer absenceId = Integer.parseInt(request.getParam("id"));
        absenceService.getAbsenceId(absenceId, DefaultResponseHandler.defaultResponseHandler(request));
    }

    @Post("/absence")
    @ApiDoc("Create absence")
    @ResourceFilter(CreateEventRight.class)
    @Trace(Actions.ABSENCE_CREATION)
    public void postEvent(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, event -> {
            String studentId = event.getString(Field.STUDENT_ID, null);
            String structureId = event.getString(Field.STRUCTURE_ID, null);
            if (Boolean.FALSE.equals(isAbsenceBodyValid(event))
                    || structureId == null
                    || studentId == null) {
                badRequest(request);
                return;
            }
            UserUtils.getUserInfos(eb, request, user -> UserInStructure.authorize(structureId, studentId, eb, inStructure -> {
                if (Boolean.TRUE.equals(inStructure)) {
                    absenceService.create(event, user, true, either -> {
                        if (either.isLeft()) {
                            String message = String.format("[Presences@AbsenceController] failed to create absent or events %s", either.left().getValue());
                            log.error(message);
                            renderError(request);
                        } else {
                            JsonObject res = new JsonObject().put(Field.EVENTS, either.right().getValue());
                            renderJson(request, res, 201);
                        }
                    });
                } else {
                    unauthorized(request, "presences.absences.user.not.in.structure");
                }
            }));
        });
    }

    @Put("/absence/:id")
    @ApiDoc("Update absence")
    // TODO Change this right
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.ABSENCE_UPDATE)
    public void update(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Long absenceId = Long.parseLong(request.getParam("id"));
        RequestUtils.bodyToJson(request, absence -> {
            if (!isAbsenceBodyValid(absence)) {
                badRequest(request);
                return;
            }
            UserUtils.getUserInfos(eb, request, user -> absenceService.update(absenceId, absence, user, true, either -> {
                if (either.isLeft()) {
                    log.error("[Presences@AbsenceController] failed to update absence", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            }));
        });
    }

    @Put("/absence/reason")
    @ApiDoc("Update reason in absence")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.ABSENCE_UPDATE_SET_REASON)
    public void changeReasonAbsence(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, absence -> {
            UserUtils.getUserInfos(eb, request, user ->
                    absenceService.changeReasonAbsences(absence, user, DefaultResponseHandler.defaultResponseHandler(request))
            );
        });
    }

    @Put("/absence/regularized")
    @ApiDoc("Update regularized absence")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.ABSENCE_REGULARISATION)
    public void regularizedAbsences(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, absence -> {
            UserUtils.getUserInfos(eb, request, user ->
                    absenceService.changeRegularizedAbsences(absence, user, DefaultResponseHandler.defaultResponseHandler(request))
            );
        });
    }

    @Put("/absences/follow")
    @ApiDoc("Update absence follow state")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.ABSENCE_FOLLOWED)
    public void followAbsence(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "absenceFollow", event -> {
            Boolean followed = event.getBoolean("followed", false);
            JsonArray absenceIds = event.getJsonArray("absenceIds", new JsonArray());
            if (absenceIds.size() == 0) {
                badRequest(request);
                return;
            }
            absenceService.followAbsence(absenceIds, followed, DefaultResponseHandler.defaultResponseHandler(request));
        });
    }


    private Boolean isAbsenceBodyValid(JsonObject absence) {
        return absence.containsKey("start_date") && absence.containsKey("end_date") && absence.containsKey("student_id");
    }

    @Delete("/absence/:id")
    @ApiDoc("Delete absence")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.ABSENCE_DELETION)
    public void delete(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Integer absenceId = Integer.parseInt(request.getParam("id"));
        collectiveService.getCollectiveFromAbsence(Long.valueOf(absenceId), colleciveResult -> {
            if (colleciveResult.failed()) {
                String message = "[Presences@AbsenceController::delete] Failed to retrieve collective from absence.";
                log.error(message);
                renderError(request);
                return;
            }
            Long collectiveId = colleciveResult.result().getLong("id");
            String structureId = colleciveResult.result().getString("structure_id");
            absenceService.delete(absenceId, deleteResult -> {
                if (colleciveResult.failed()) {
                    String message = "[Presences@AbsenceController::delete] Failed to delete absence.";
                    log.error(message);
                    renderError(request);
                    return;
                }

                if (collectiveId != null) {
                    collectiveService.removeAudiencesRelation(structureId, collectiveId, audienceResult -> {
                        if (audienceResult.failed()) {
                            String message = "[Presences@AbsenceController::delete] Failed to delete collectives audiences.";
                            log.error(message);
                            renderError(request);
                            return;
                        }
                        renderJson(request, new JsonObject().put("success", "ok"));
                    });
                    return;
                }

                renderJson(request, new JsonObject().put("success", "ok"));
            });
        });
    }

    @Get("/absences")
    @ApiDoc("Retrieve all absences matching parameters")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AbsenceRight.class)
    public void getAbsences(HttpServerRequest request) {
        String structure = request.getParam(Field.STRUCTURE);
        String start = request.getParam(Field.START);
        String end = request.getParam(Field.END);
        List<String> students = request.params().getAll(Field.STUDENT);
        List<String> classes = request.params().getAll(Field.CLASSES);
        Boolean halfBoarder = request.params().contains(Field.HALFBOARDER) ? Boolean.parseBoolean(request.getParam(Field.HALFBOARDER)) : null;
        Boolean internal = request.params().contains(Field.INTERNAL) ? Boolean.parseBoolean(request.getParam(Field.INTERNAL)) : null;
        Boolean followed = request.params().contains(Field.FOLLOWED) ? Boolean.parseBoolean(request.getParam(Field.FOLLOWED)) : null;
        Boolean regularized = request.params().contains(Field.REGULARIZED) ? Boolean.parseBoolean(request.getParam(Field.REGULARIZED)) : null;
        Boolean noReason = request.params().contains(Field.NOREASON)
                && Boolean.parseBoolean(request.getParam(Field.NOREASON));
        Boolean justified = request.params().contains(Field.JUSTIFIED) ? Boolean.parseBoolean(request.getParam(Field.JUSTIFIED)) : null;

        List<Integer> reasons = request.params().getAll(Field.REASON)
                .stream()
                .mapToInt(Integer::parseInt)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        Integer page = request.getParam(Field.PAGE) != null ? Integer.parseInt(request.getParam(Field.PAGE)) : null;

        UserUtils.getUserInfos(eb, request, user -> {
            String teacherId = (WorkflowHelper.hasRight(user, WorkflowActions.READ_EVENT_RESTRICTED.toString())
                    && "Teacher".equals(user.getType())) ?
                    user.getUserId() : null;

            absenceService
                    .get(structure, teacherId, classes, students, reasons, start, end, (regularized != null ? regularized : justified),
                            noReason, followed, halfBoarder, internal, page)
                    .onFailure(err -> renderError(request))
                    .onSuccess(result -> {
                        if (page != null) renderJson(request, result);
                        else renderJson(request, result.getJsonArray(Field.ALL, new JsonArray()));
                    });
        });
    }

    @Post("/absences/restore")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdminFilter.class)
    @ApiDoc("restore absences with the correct structure")
    public void restoreAbsences(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, String.format("%s%s", pathPrefix, "restoreAbsences"), body -> {
            String structureId = body.getString(Field.STRUCTURE_ID);
            String startAt = body.getString(Field.START_AT);
            String endAt = body.getString(Field.END_AT);

            absenceService.restoreAbsences(structureId, startAt, endAt)
                    .onSuccess(result -> renderJson(request, result))
                    .onFailure(err -> renderError(request));
        });
    }

    @Get("/structures/:structureId/absences/markers")
    @ApiDoc("Get absences counts markers")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AbsenceRight.class)
    public void getAbsentsCounts(final HttpServerRequest request) {
        String structureId = request.getParam(Field.STRUCTUREID);
        String startAt = request.getParam(Field.STARTAT);
        String endAt = request.getParam(Field.ENDAT);

        if (structureId == null || startAt == null || endAt == null) {
            badRequest(request);
            return;
        }

        eventService.getAbsencesCountSummary(structureId, startAt, endAt, Arrays.stream(Markers.values())
                        .filter(value -> !value.equals(Markers.NB_PRESENTS))
                        .map(Enum::name)
                        .collect(Collectors.toList()))
                .onFailure(err -> renderError(request))
                .onSuccess(summary -> renderJson(request, summary));
    }

    @Get("/structures/:structureId/absences/export/pdf")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AbsenceRight.class)
    @ApiDoc("Export absences")
    public void exportAbsencesPdf(HttpServerRequest request) {
        String structure = request.getParam(Field.STRUCTUREID);
        String start = request.getParam(Field.START);
        String end = request.getParam(Field.END);
        List<String> students = request.params().getAll(Field.STUDENT);
        List<String> classes = request.params().getAll(Field.CLASSES);
        Boolean halfBoarder = request.params().contains(Field.HALFBOARDER) ? Boolean.parseBoolean(request.getParam(Field.HALFBOARDER)) : null;
        Boolean internal = request.params().contains(Field.INTERNAL) ? Boolean.parseBoolean(request.getParam(Field.INTERNAL)) : null;
        Boolean followed = request.params().contains(Field.FOLLOWED) ? Boolean.parseBoolean(request.getParam(Field.FOLLOWED)) : null;
        Boolean noReason = request.params().contains(Field.NOREASON)
                && Boolean.parseBoolean(request.getParam(Field.NOREASON));
        Boolean regularized = request.params().contains(Field.REGULARIZED) ? Boolean.parseBoolean(request.getParam(Field.REGULARIZED)) : null;
        Boolean justified = request.params().contains(Field.JUSTIFIED) ? Boolean.parseBoolean(request.getParam(Field.JUSTIFIED)) : null;

        List<Integer> reasons = request.params().getAll(Field.REASON)
                .stream()
                .mapToInt(Integer::parseInt)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        UserUtils.getUserInfos(eb, request, user -> {
            String teacherId = (WorkflowHelper.hasRight(user, WorkflowActions.READ_EVENT_RESTRICTED.toString())
                    && "Teacher".equals(user.getType())) ?
                    user.getUserId() : null;

            String domain = Renders.getHost(request);
            String local = I18n.acceptLanguage(request);
            exportAbsencesPdf.generatePdf(domain, local, structure, teacherId, classes, students, reasons, start, end,
                            (regularized != null ? regularized : justified), noReason, followed, halfBoarder, internal)
                    .onSuccess(pdf -> request.response()
                            .putHeader("Content-type", "application/pdf; charset=utf-8")
                            .putHeader("Content-Disposition", "attachment; filename=" + pdf.getName())
                            .end(pdf.getContent()))
                    .onFailure(err -> {
                        String message = "An error has occurred during export pdf process";
                        String logMessage = String.format("[Presences@%s::exportAbsencesPdf] %s: %s",
                                this.getClass().getSimpleName(), message, err.getMessage());
                        log.error(logMessage);
                        renderError(request, new JsonObject().put(Field.MESSAGE, message));
                    });

        });
    }

    @Get("/structures/:structureId/absences/export/csv")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AbsenceRight.class)
    @ApiDoc("Export absences")
    @SuppressWarnings("unchecked")
    public void exportAbsencesCsv(HttpServerRequest request) {
        String structure = request.getParam(Field.STRUCTUREID);
        String start = request.getParam(Field.START);
        String end = request.getParam(Field.END);
        List<String> students = request.params().getAll(Field.STUDENT);
        List<String> classes = request.params().getAll(Field.CLASSES);
        Boolean halfBoarder = request.params().contains(Field.HALFBOARDER) ? Boolean.parseBoolean(request.getParam(Field.HALFBOARDER)) : null;
        Boolean internal = request.params().contains(Field.INTERNAL) ? Boolean.parseBoolean(request.getParam(Field.INTERNAL)) : null;
        Boolean followed = request.params().contains(Field.FOLLOWED) ? Boolean.parseBoolean(request.getParam(Field.FOLLOWED)) : null;
        Boolean noReason = request.params().contains(Field.NOREASON)
                && Boolean.parseBoolean(request.getParam(Field.NOREASON));
        Boolean regularized = request.params().contains(Field.REGULARIZED) ? Boolean.parseBoolean(request.getParam(Field.REGULARIZED)) : null;
        Boolean justified = request.params().contains(Field.JUSTIFIED) ? Boolean.parseBoolean(request.getParam(Field.JUSTIFIED)) : null;

        List<Integer> reasons = request.params().getAll(Field.REASON)
                .stream()
                .mapToInt(Integer::parseInt)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        UserUtils.getUserInfos(eb, request, user -> {
            String teacherId = (WorkflowHelper.hasRight(user, WorkflowActions.READ_EVENT_RESTRICTED.toString())
                    && "Teacher".equals(user.getType())) ?
                    user.getUserId() : null;

            String domain = Renders.getHost(request);
            String local = I18n.acceptLanguage(request);

            exportAbsencesPdf.getAbsencesData(domain, local, structure, teacherId, classes, students, reasons, start, end,
                            (regularized != null ? regularized : justified), noReason, followed, halfBoarder, internal)
                    .onSuccess(absences -> {
                        AbsencesCSVExport ace =
                                new AbsencesCSVExport(absences.getJsonArray(EventType.ABSENCE.name(), new JsonArray()).getList(), request);
                        ace.export();
                    })
                    .onFailure(err -> {
                        String message = "An error has occurred during export csv process";
                        String logMessage = String.format("[Presences@%s::exportAbsencesCsv] %s: %s",
                                this.getClass().getSimpleName(), message, err.getMessage());
                        log.error(logMessage);
                        renderError(request, new JsonObject().put("message", message));
                    });
        });
    }
}
