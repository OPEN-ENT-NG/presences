package fr.openent.presences.cron;

import fr.openent.presences.service.AbsenceService;
import fr.openent.presences.service.impl.DefaultAbsenceService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AbsenceRemovalTask implements Handler<Long> {

    private final EventBus eb;
    private AbsenceService absenceService;

    private final Logger log = LoggerFactory.getLogger(AbsenceRemovalTask.class);

    public AbsenceRemovalTask(EventBus eb) {
        this.eb = eb;
        this.absenceService = new DefaultAbsenceService(eb);
    }

    @Override
    public void handle(Long event) {
        log.info("Absence removal task launched");
        absenceService.absenceRemovalTask(result -> {
            if (result.isLeft()) {
                String message = "[Presences@AbsenceRemovalTask] failed to automate Absence Removal CRON Task";
                log.error(message, result.left().getValue());
            } else {
                log.info("Absence Removal CRON Task succeeded");
            }
        });
    }
}
