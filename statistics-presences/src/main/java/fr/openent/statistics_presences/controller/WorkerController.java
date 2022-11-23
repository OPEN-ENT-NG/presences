package fr.openent.statistics_presences.controller;

import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.indicator.IProcessingScheduled;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;

public class WorkerController extends ControllerHelper {

    public WorkerController() {
    }

    @Get("/worker/reset")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void resetWorker(final HttpServerRequest request) {
        IProcessingScheduled.ProcessingScheduledHolder.finish();
        Renders.ok(request);
    }
}
