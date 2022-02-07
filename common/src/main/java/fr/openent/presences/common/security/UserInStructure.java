package fr.openent.presences.common.security;

import fr.openent.presences.core.constants.Field;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

public class UserInStructure implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String structure = request.getParam(Field.STRUCTURE);
        handler.handle(isInStructure(structure, user));
    }

    private static boolean isInStructure(String structureId, UserInfos userInfos) {
        return userInfos != null && userInfos.getStructures().contains(structureId);
    }


    /**
     * Return true if user is in structure
     *
     * @param structureId user structureId
     * @param userId userId
     * @param eb event bus to find user
     * @param handler handler
     */
    public static void authorize(String structureId, String userId, EventBus eb, Handler<Boolean> handler) {
        UserUtils.getUserInfos(eb, userId, userInfos -> handler.handle(isInStructure(structureId, userInfos)));
    }
}
