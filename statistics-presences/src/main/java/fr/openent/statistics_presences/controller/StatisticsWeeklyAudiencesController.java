package fr.openent.statistics_presences.controller;

import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import fr.openent.statistics_presences.service.StatisticsWeeklyAudiencesService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import java.util.List;

public class StatisticsWeeklyAudiencesController extends ControllerHelper {
    private final StatisticsWeeklyAudiencesService weeklyAudiencesService;

    public StatisticsWeeklyAudiencesController(CommonServiceFactory serviceFactory) {
        this.weeklyAudiencesService = serviceFactory.statisticsWeeklyAudiencesService();
    }

    @Post("/process/weekly/audiences/tasks")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdminFilter.class)
    @ApiDoc("Generate notebook archives")
    @SuppressWarnings("unchecked")
    public void processStatisticsPrefetch(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "processWeeklyAudiencesPrefetch", body -> {
            List<String> structureIds = body.getJsonArray(Field.STRUCTURE_ID).getList();
            String startAt = body.getString(Field.START_AT);
            String endAt = body.getString(Field.END_AT);
            weeklyAudiencesService.processWeeklyAudiencesPrefetch(structureIds, startAt, endAt)
                    .onSuccess(res -> renderJson(request, res))
                    .onFailure(unused -> renderError(request));
        });
    }
}
