package fr.openent.presences.controller;

import fr.openent.presences.security.Manage;
import fr.openent.presences.service.DisciplineService;
import fr.openent.presences.service.impl.DefaultDisciplineService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.response.DefaultResponseHandler;

public class DisciplineController extends ControllerHelper {
    private DisciplineService disciplineService;

    public DisciplineController() {
        super();
        this.disciplineService = new DefaultDisciplineService();
    }

    @Get("/disciplines")
    @ApiDoc("Get disciplines")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        if (!request.params().contains("structureId")) {
            badRequest(request);
            return;
        }
        disciplineService.get(structureId, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/discipline")
    @ApiDoc("Create discipline")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void post(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, disciplineBody -> {
            if (isDisciplineBodyInvalid(disciplineBody)) {
                badRequest(request);
                return;
            }
            disciplineService.create(disciplineBody, either -> {
                if (either.isLeft()) {
                    log.error("[Presences@DisciplineController] failed to create discipline", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    private boolean isDisciplineBodyInvalid(JsonObject disciplineBody) {
        return !disciplineBody.containsKey("structureId") &&
                !disciplineBody.containsKey("label") && !disciplineBody.containsKey("hidden");
    }

    @Put("/discipline")
    @ApiDoc("Update discipline")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void put(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, disciplineBody -> {
            if (isDisciplineBodyInvalid(disciplineBody) && !disciplineBody.containsKey("hidden") &&
                    !disciplineBody.containsKey("id")) {
                badRequest(request);
                return;
            }
            disciplineService.put(disciplineBody, either -> {
                if (either.isLeft()) {
                    log.error("[Presences@DisciplineController] failed to update discipline", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    @Delete("/discipline")
    @ApiDoc("Delete discipline")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void delete(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Integer disciplineId = Integer.parseInt(request.getParam("id"));
        disciplineService.delete(disciplineId, either -> {
            if (either.isLeft()) {
                log.error("[Presences@DisciplineController] failed to delete discipline", either.left().getValue());
                renderError(request);
            } else {
                renderJson(request, either.right().getValue());
            }
        });
    }
}
