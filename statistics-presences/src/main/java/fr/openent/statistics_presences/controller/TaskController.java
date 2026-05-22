package fr.openent.statistics_presences.controller;

import fr.openent.statistics_presences.indicator.ProcessingScheduledTask;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TaskController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final ProcessingScheduledTask processingScheduledTask;

    public TaskController(ProcessingScheduledTask processingScheduledTask) {
        this.processingScheduledTask = processingScheduledTask;
    }

    @Post("api/internal/process")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void process(final HttpServerRequest request) {
        log.info("Triggered process task");
        processingScheduledTask.handle(0L);
        render(request, 202);
    }
}
