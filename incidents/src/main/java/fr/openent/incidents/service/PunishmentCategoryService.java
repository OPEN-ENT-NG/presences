package fr.openent.incidents.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface PunishmentCategoryService {

    /**
     * get punishment category
     *
     * @param handler Function handler returning data
     */
    void get(Handler<Either<String, JsonArray>> handler);
}
