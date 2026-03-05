package fr.openent.presences.controller;

import fr.openent.presences.cron.CreateDailyRegistersTask;
import fr.openent.presences.cron.UpdateEventRegularizationTask;
import fr.wseduc.rs.Post;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

public class TaskController extends BaseController {
	protected static final Logger log = LoggerFactory.getLogger(TaskController.class);

	final CreateDailyRegistersTask createDailyRegistersTask;
	final UpdateEventRegularizationTask updateEventRegularizationTask;

	public TaskController(CreateDailyRegistersTask createDailyRegistersTask, UpdateEventRegularizationTask updateEventRegularizationTask) {
		this.createDailyRegistersTask = createDailyRegistersTask;
		this.updateEventRegularizationTask = updateEventRegularizationTask;
	}

	@Post("api/internal/create-daily-registers")
	public void createDailyRegisters(HttpServerRequest request) {
		log.info("triggered daily registers creation task");
		createDailyRegistersTask.handle(0L);
		render(request, null, 202);
	}

	@Post("api/internal/update-event-regularization")
	public void setUpdateEventRegularization(HttpServerRequest request) {
		log.info("Triggered event regularization update task");
		updateEventRegularizationTask.handle(0L);
		render(request, null, 202);
	}
}
