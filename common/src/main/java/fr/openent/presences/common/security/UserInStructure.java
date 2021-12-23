package fr.openent.presences.common.security;

import fr.openent.presences.core.constants.Field;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class UserInStructure implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String structure = request.getParam(Field.STRUCTURE);
        handler.handle(user.getStructures().contains(structure));
    }
}
