package fr.openent.presences.helper;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

public class SubjectHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectHelper.class);
    private final EventBus eb;

    public SubjectHelper(EventBus eb) {
        this.eb = eb;
    }

    public void getSubjects(List<String> subjects, Handler<Either<String, JsonArray>> handler) {
        getSubjects(new JsonArray(subjects), handler);
    }

    public void getSubjects(JsonArray subjects, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "matiere.getMatieres")
                .put("idMatieres", subjects);

        eb.request("viescolaire", action, event -> {
            JsonObject body = (JsonObject) event.result().body();
            if (event.failed() || "error".equals(body.getString("status"))) {
                String err = "[SubjectHelper@getSubjects] Failed to retrieve subjects";
                LOGGER.error(err);
                handler.handle(new Either.Left<>(err));
            } else {
                handler.handle(new Either.Right<>(body.getJsonArray("results")));
            }
        });
    }
}
