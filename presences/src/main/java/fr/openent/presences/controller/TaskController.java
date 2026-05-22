package fr.openent.presences.controller;

import fr.openent.presences.cron.CreateDailyRegistersTask;
import fr.openent.presences.cron.UpdateEventRegularizationTask;
import fr.wseduc.rs.Post;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TaskController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final CreateDailyRegistersTask createDailyRegistersTask;
    private final UpdateEventRegularizationTask updateEventRegularizationTask;

    public TaskController(CreateDailyRegistersTask createDailyRegistersTask, UpdateEventRegularizationTask updateEventRegularizationTask) {
        this.createDailyRegistersTask = createDailyRegistersTask;
        this.updateEventRegularizationTask = updateEventRegularizationTask;
    }

    @Post("api/internal/create-daily-registers")
    public void createDailyRegisters(final HttpServerRequest request) {
        log.info("Triggered create daily registers task");
        createDailyRegistersTask.handle(0L);
        render(request, 202);
    }

    @Post("api/internal/update-event-regularization")
    public void updateEventRegularization(final HttpServerRequest request) {
        log.info("Triggered update event regularization task");
        updateEventRegularizationTask.handle(0L);
        render(request, 202);
    }

}
