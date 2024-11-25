package fr.openent.presences.controller;

import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.security.UserInStructure;
import fr.openent.presences.service.CommonPresencesServiceFactory;
import fr.openent.presences.service.GroupingService;
import fr.openent.presences.service.impl.DefaultGroupingService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

public class GroupingController extends ControllerHelper {
    private GroupingService groupingService;

    public GroupingController(CommonPresencesServiceFactory commonPresencesServiceFactory) {
        groupingService = commonPresencesServiceFactory.groupingService();
    }

    @Get("/grouping/structure/:id/list")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ApiDoc("List groupings")
    @ResourceFilter(UserInStructure.class)
    public void listGroupings(HttpServerRequest request) {
        String structureId = request.getParam(Field.ID);
        String searchValue = request.getParam(Field.SEARCHVALUE) == null ? "" : request.getParam(Field.SEARCHVALUE);

        this.groupingService.searchGrouping(structureId, searchValue)
                .onSuccess(groupingList -> renderJson(request, IModelHelper.toJsonArray(groupingList)))
                .onFailure(error -> renderError(request));
    }
}
