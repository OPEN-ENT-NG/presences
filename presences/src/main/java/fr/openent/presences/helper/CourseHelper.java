package fr.openent.presences.helper;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

public class CourseHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourseHelper.class);
    private final EventBus eb;

    public CourseHelper(EventBus eb) {
        this.eb = eb;
    }

    /**
     * Parameters validation
     *
     * @param params parameters list
     * @return if parameters are valids or not
     */
    public boolean checkParams(MultiMap params) {
        if (!params.contains("structure")
                || !params.contains("start") || !params.contains("end")
                || !params.get("start").matches("\\d{4}-\\d{2}-\\d{2}")
                || !params.get("end").matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }

        return true;
    }

    public void getCourses(String structure, List<String> teachers, List<String> groups, String start, String end, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "course.getCoursesOccurences")
                .put("structureId", structure)
                .put("teacherId", new JsonArray(teachers))
                .put("group", new JsonArray(groups))
                .put("begin", start)
                .put("end", end);

        eb.send("viescolaire", action, event -> {
            JsonObject body = (JsonObject) event.result().body();
            if (event.failed() || "error".equals(body.getString("status"))) {
                String err = "[CourseHelper@getCourses] Failed to retrieve courses";
                LOGGER.error(err);
                handler.handle(new Either.Left<>(err));
            } else {
                handler.handle(new Either.Right<>(body.getJsonArray("results")));
            }
        });
    }
}
