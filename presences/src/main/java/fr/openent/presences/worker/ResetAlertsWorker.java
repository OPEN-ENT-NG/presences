package fr.openent.presences.worker;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.service.AlertService;
import fr.openent.presences.service.impl.DefaultAlertService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.vertx.java.busmods.BusModBase;

public class ResetAlertsWorker extends BusModBase implements Handler<Message<JsonObject>> {
    private final Logger log = LoggerFactory.getLogger(ResetAlertsWorker.class);
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_CYAN = "\u001B[36m";
    private static final String RESET_ALERTS_FLAG = "[PRESENCES:RESET_ALERTS]";


    private final AlertService alertService = new DefaultAlertService();

    @Override
    public void start() {
        super.start();
        eb.consumer(this.getClass().getName(), this);
    }

    @Override
    public void handle(Message<JsonObject> eventMessage) {
        log.info(String.format("[<worker>Presences@%s] receiving", this.getClass().getSimpleName()));
        log.info(String.format("%s%s Begin to process %s", ANSI_CYAN, RESET_ALERTS_FLAG, ANSI_RESET));
        eventMessage.reply(new JsonObject().put(Field.STATUS, Field.OK));
        JsonObject structure = eventMessage.body().getJsonObject(Field.STRUCTURE, new JsonObject());
        String structureId = structure.getString(Field.ID);
        String structureName = structure.getString(Field.NAME);

        resetAlerts(structureId, structureName);
    }

    private void resetAlerts(String structureId, String structureName) {
        alertService.delete(structureId, null, null, null)
                .onSuccess(resultArchive -> log.info(String.format("%s%s Reset alerts end and process complete for structure %s %s",
                        ANSI_CYAN, RESET_ALERTS_FLAG, structureName, ANSI_RESET)))
                .onFailure(resultArchive -> log.error(String.format("[<worker>Presences@%s] Reset alerts end with a failure process for structure %s: %s",
                        this.getClass().getSimpleName(),
                        structureName,
                        resultArchive.getMessage())));
    }


}
