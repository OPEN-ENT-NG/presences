package fr.openent.presences.security;

import fr.openent.presences.core.constants.Field;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class UserInStructure implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String structureId = request.getParam(Field.ID);

        if (structureId == null) {
            handler.handle(false);
            return;
        }
        handler.handle(user.getStructures().contains(structureId));
    }
}
