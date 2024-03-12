package fr.openent.presences.cron;

import fr.openent.presences.service.EventService;
import fr.openent.presences.service.impl.DefaultEventService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

    public class UpdateEventRegularizationTask implements Handler<Long> {

    private final EventBus eb;
    private EventService eventService;

    private final Logger log = LoggerFactory.getLogger(AbsenceRemovalTask.class);

    public UpdateEventRegularizationTask(EventBus eb) {
        this.eb = eb;
        this.eventService = new DefaultEventService(this.eb);
    }

    @Override
    public void handle(Long event) {
        log.info("[Presences@UpdateEventRegularizationCron] Update event regularization task launched");
        eventService.updateEventRegularization()
                .onSuccess(res -> log.info("[Presences@UpdateEventRegularizationCron] events regularized"))
                .onFailure(err -> log.error("[Presences@UpdateEventRegularizationCron] failed to regulate events : " + err.getMessage()));
    }
}