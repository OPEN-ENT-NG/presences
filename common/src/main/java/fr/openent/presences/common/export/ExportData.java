package fr.openent.presences.common.export;

import fr.openent.presences.core.constants.*;
import fr.openent.presences.db.*;
import io.vertx.core.*;
import io.vertx.core.eventbus.*;
import io.vertx.core.json.*;

public class ExportData extends DBService {

    private final Vertx vertx;

    public ExportData(Vertx vertx) {
        this.vertx = vertx;
    }

    public void export(String workerName, String action, String type, JsonObject params) {

        JsonObject configWorker = new JsonObject()
                .put(Field.ACTION, action)
                .put(Field.TYPE, type)
                .put(Field.PARAMS, params);

        vertx.eventBus().send(workerName, configWorker,
                new DeliveryOptions().setSendTimeout(1000 * 1000L));
    }

}
