package fr.openent.presences.cron;

import fr.openent.presences.worker.CreateDailyPresenceWorker;
import fr.wseduc.webutils.Utils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class CreateDailyRegistersTask implements Handler<Long> {
    private final Logger log = LoggerFactory.getLogger(CreateDailyRegistersTask.class);
    private EventBus eventBus;



    public CreateDailyRegistersTask(EventBus eb) {
        this.eventBus = eb;
    }

    @Override
    public void handle(Long event) {
        eventBus.request(CreateDailyPresenceWorker.class.getName(),
                new JsonObject(),
                new DeliveryOptions().setSendTimeout(1000 * 1000L),
                Utils.handlerToAsyncHandler(eventExport ->{
                    log.info("Ok calling worker " + eventExport.body().toString());
                }));
    }
}
