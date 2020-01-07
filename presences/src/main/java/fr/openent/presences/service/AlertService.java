package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface AlertService {
    /**
     * Get alerts count
     *
     * @param structureId structure identifier
     * @param handler function handler return data
     */
    void getSummary(String structureId, Handler<Either<String, JsonObject>> handler);
}
