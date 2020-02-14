package fr.openent.presences.controller;

import fr.openent.presences.constants.Actions;
import fr.openent.presences.security.Manage;
import fr.openent.presences.service.NotebookService;
import fr.openent.presences.service.impl.DefaultNotebookService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.http.response.DefaultResponseHandler;

public class NotebookController extends ControllerHelper {

    private NotebookService notebookService;

    public NotebookController() {
        super();
        this.notebookService = new DefaultNotebookService();
    }

    @Get("/forgotten/notebook")
    @ApiDoc("Get forgotten notebook")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {
        if (!request.params().contains("studentId")) {
            badRequest(request);
            return;
        }
        String studentId = request.getParam("studentId");
        String startDate = String.valueOf(request.getParam("startDate"));
        String endDate = String.valueOf(request.getParam("endDate"));
        notebookService.get(studentId, startDate, endDate, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/forgotten/notebook")
    @ApiDoc("Create forgotten notebook")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.FORGOTTEN_NOTEBOOK_CREATION)
    public void post(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, notebookBody -> {
            if (!(notebookBody.containsKey("studentId")
                    && notebookBody.containsKey("structureId")
                    && notebookBody.containsKey("date"))) {
                badRequest(request);
                return;
            }
            notebookService.create(notebookBody, DefaultResponseHandler.defaultResponseHandler(request));
        });
    }

    @Put("/forgotten/notebook/:id")
    @ApiDoc("Update forgotten notebook")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.FORGOTTEN_NOTEBOOK_UPDATE)
    public void update(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Integer notebookId = Integer.parseInt(request.getParam("id"));
        RequestUtils.bodyToJson(request, notebookBody -> {
            if (!notebookBody.containsKey("date")) {
                badRequest(request);
                return;
            }
            notebookService.update(notebookId, notebookBody, DefaultResponseHandler.defaultResponseHandler(request));
        });
    }

    @Delete("/forgotten/notebook/:id")
    @ApiDoc("Delete absence")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.FORGOTTEN_NOTEBOOK_DELETION)
    public void delete(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Integer notebookId = Integer.parseInt(request.getParam("id"));
        notebookService.delete(notebookId, DefaultResponseHandler.defaultResponseHandler(request));
    }

}
