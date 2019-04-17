package fr.openent.presences.service;

import fr.openent.presences.enums.GroupType;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface GroupService {

    /**
     * Get all group identifiers
     *
     * @param structureId structure identifier
     * @param groups      group list
     * @param classes     class list
     * @param handler     Function handler returning data
     */
    void getGroupsId(String structureId, JsonArray groups, JsonArray classes, Handler<Either<String, JsonObject>> handler);

    /**
     * Get group users
     *
     * @param id      group identifier
     * @param type    group type
     * @param handler Function handler returning data
     */
    void getGroupUsers(String id, GroupType type, Handler<Either<String, JsonArray>> handler);
}
