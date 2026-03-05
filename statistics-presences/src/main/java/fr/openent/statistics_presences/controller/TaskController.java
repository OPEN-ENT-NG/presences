package fr.openent.statistics_presences.controller;

import fr.openent.statistics_presences.indicator.ProcessingScheduledTask;
import fr.wseduc.rs.Post;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

public class TaskController extends BaseController {
	protected static final Logger log = LoggerFactory.getLogger(TaskController.class);

	final ProcessingScheduledTask processingScheduledTask;

	public TaskController(ProcessingScheduledTask processingScheduledTask) {
		this.processingScheduledTask = processingScheduledTask;
	}

	@Post("api/internal/process")
	public void process(HttpServerRequest request) {
		log.info("Triggered processing scheduled task");
		processingScheduledTask.handle(0L);
		render(request, null, 202);
	}
}
