package fr.openent.presences.service.impl;

import fr.openent.presences.service.CourseService;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.mongodb.MongoDbResult;

public class DefaultCourseService implements CourseService {
    @Override
    public void getCourse(String courseId, Handler<Either<String, JsonObject>> handler) {
        JsonObject courseQuery = new JsonObject()
                .put("_id", courseId);

        MongoDb.getInstance().findOne("courses", courseQuery, message -> handler.handle(MongoDbResult.validResult(message)));
    }
}
