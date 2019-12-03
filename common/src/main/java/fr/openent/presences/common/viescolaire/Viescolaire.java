package fr.openent.presences.common.viescolaire;

import fr.openent.presences.common.message.MessageResponseHandler;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Viescolaire {
    private static final Logger LOGGER = LoggerFactory.getLogger(Viescolaire.class);

    private String address = "viescolaire";
    private EventBus eb;

    private Viescolaire() {
    }

    public static Viescolaire getInstance() {
        return ViescolaireHolder.instance;
    }

    public void init(EventBus eb) {
        this.eb = eb;
    }

    /**
     * Retrieve all exclusions days (holidays/vacations/day's off...)
     *
     * @param structureId structure identifier
     * @param handler     Function handler returning data
     */
    public void getExclusionDays(String structureId, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "periode.getExclusionDays")
                .put("idEtablissement", structureId);

        eb.send(address, action, result -> {
            JsonObject body = (JsonObject) result.result().body();
            if (result.failed() || "error".equals(body.getString("status"))) {
                String err = "[Common@ViescolaireHelper] Failed to retrieve exclusion days";
                LOGGER.error(err);
                handler.handle(new Either.Left<>(err));
            } else {
                handler.handle(new Either.Right<>(body.getJsonArray("results")));
            }
        });
    }

    public void getSlotProfileSetting(String structureId, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "timeslot.getSlotProfileSettings")
                .put("structureId", structureId);

        eb.send(address, action, MessageResponseHandler.messageJsonObjectHandler(handler));
    }

    public void getDefaultSlots(String structureId, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "timeslot.getDefaultSlots")
                .put("structureId", structureId);

        eb.send(address, action, result -> {
            JsonObject body = (JsonObject) result.result().body();
            if (result.failed() || "error".equals(body.getString("status"))) {
                String err = "[Common@ViescolaireHelper] Failed to retrieve default slots";
                LOGGER.error(err);
                handler.handle(new Either.Left<>(err));
            } else {
                handler.handle(new Either.Right<>(body.getJsonArray("results")));
            }
        });
    }

    private static class ViescolaireHolder {
        private static final Viescolaire instance = new Viescolaire();

        private ViescolaireHolder() {
        }
    }
}
