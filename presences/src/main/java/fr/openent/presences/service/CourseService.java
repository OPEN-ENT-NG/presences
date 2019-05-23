package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface CourseService {

    /**
     * Get given course
     *
     * @param courseId course identifier
     * @param handler  FUnction handler returning data
     */
    void getCourse(String courseId, Handler<Either<String, JsonObject>> handler);
}
