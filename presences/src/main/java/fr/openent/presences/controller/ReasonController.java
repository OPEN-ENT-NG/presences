package fr.openent.presences.controller;

import fr.openent.presences.constants.Actions;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.ReasonType;
import fr.openent.presences.security.*;
import fr.openent.presences.service.ReasonService;
import fr.openent.presences.service.impl.DefaultReasonService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.http.response.DefaultResponseHandler;

public class ReasonController extends ControllerHelper {

    private ReasonService reasonService;

    public ReasonController() {
        super();
        this.reasonService = new DefaultReasonService();
    }

    @Get("/reasons")
    @ApiDoc("Get reasons")
    @ResourceFilter(AbsenceRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {
        String structureId = request.getParam(Field.STRUCTUREID);
        Integer reasonTypeId = request.params().contains(Field.REASONTYPEID) ?
                Integer.parseInt(request.getParam(Field.REASONTYPEID)) : ReasonType.ABSENCE.getValue();

        if (!request.params().contains(Field.STRUCTUREID)) {
            badRequest(request);
            return;
        }
        reasonService.get(structureId, reasonTypeId)
                .onSuccess(res -> renderJson(request, res))
                .onFailure(error -> renderError(request));
    }

    @Post("/reason")
    @ApiDoc("Create reason")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.REASON_CREATION)
    public void post(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "reasonCreate", reasonBody -> {
            if (request.params().contains(Field.REASONTYPEID))
                reasonBody.put(Field.REASONTYPEID, Integer.parseInt(request.getParam(Field.REASONTYPEID)));

            reasonService.create(reasonBody)
                    .onSuccess(res -> renderJson(request, res))
                    .onFailure(error -> {
                        log.error(String.format("[Presences@ReasonController] failed to create reason %s", error.getMessage()));
                        renderError(request);
                    });
        });
    }

    private boolean isReasonBodyInvalid(JsonObject reasonBody) {
        return !reasonBody.containsKey("structureId") &&
                !reasonBody.containsKey("label") &&
                !reasonBody.containsKey("absenceCompliance") &&
                !reasonBody.containsKey("regularisable");
    }

    @Put("/reason")
    @ApiDoc("Update reason")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.REASON_UPDATE)
    public void put(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request,pathPrefix + "reasonUpdate", reasonBody -> {
            if (isReasonBodyInvalid(reasonBody) && !reasonBody.containsKey("hidden") &&
                    !reasonBody.containsKey("id")) {
                badRequest(request);
                return;
            }
            reasonService.put(reasonBody)
                    .onSuccess(reason ->  renderJson(request, reason))
                    .onFailure(error -> {
                        log.error(String.format("[Presences@ReasonController] failed to update reason %s", error.getMessage()));
                        renderError(request);
                    });
        });
    }

    @Delete("/reason")
    @ApiDoc("Delete reason")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.REASON_DELETION)
    public void delete(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Integer reasonId = Integer.parseInt(request.getParam("id"));
        reasonService.delete(reasonId, either -> {
            if (either.isLeft()) {
                log.error("[Presences@ReasonController] failed to delete reason", either.left().getValue());
                renderError(request);
            } else {
                renderJson(request, either.right().getValue());
            }
        });
    }
}
