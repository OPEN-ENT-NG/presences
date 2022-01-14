package fr.openent.presences.security;

import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.enums.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class StatisticsAccessData implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest httpServerRequest, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        handler.handle(WorkflowHelper.hasRight(userInfos, WorkflowActions.STATISTICS_ACCESS_DATA.toString()));
    }
}
