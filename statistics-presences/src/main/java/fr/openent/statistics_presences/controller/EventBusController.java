package fr.openent.statistics_presences.controller;

import fr.openent.presences.common.bus.BusResultHandler;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.StatisticsUser;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.indicator.Indicator;
import fr.openent.statistics_presences.model.StatisticsFilter;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import fr.openent.statistics_presences.service.StatisticsPresencesService;
import fr.openent.statistics_presences.service.StatisticsWeeklyAudiencesService;
import fr.openent.statistics_presences.service.impl.DefaultStatisticsPresencesService;
import fr.openent.statistics_presences.service.impl.DefaultStatisticsWeeklyAudiencesService;
import fr.wseduc.bus.BusAddress;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import java.util.Arrays;
import java.util.List;

public class EventBusController extends ControllerHelper {

    private final StatisticsPresencesService statisticsService;
    private final StatisticsWeeklyAudiencesService weeklyAudiencesService;

    public EventBusController(CommonServiceFactory commonServiceFactory) {
        statisticsService = new DefaultStatisticsPresencesService(commonServiceFactory);
        weeklyAudiencesService = new DefaultStatisticsWeeklyAudiencesService(commonServiceFactory);
    }

    @BusAddress("fr.openent.statistics.presences")
    @SuppressWarnings("unchecked")
    public void bus(final Message<JsonObject> message) {
        JsonObject body = message.body();
        String action = body.getString(Field.ACTION);
        switch (action) {
            case "post-users":
                String structure = body.getString(Field.STRUCTUREID);
                if (body.containsKey(Field.STATISTICS_USERS)) {
                    List<StatisticsUser> statisticsUserList = IModelHelper.toList(body.getJsonArray(Field.STATISTICS_USERS), StatisticsUser.class);
                    statisticsService.createWithModifiedDate(structure, statisticsUserList, BusResultHandler.busResponseHandler(message));
                } else {
                    //Deprecated
                    List<String> student = body.getJsonArray(Field.STUDENTIDS).getList();
                    statisticsService.create(structure, student, BusResultHandler.busResponseHandler(message));
                }
                break;
            case "post-weekly-audiences":
                String structureId = body.getString(Field.STRUCTUREID);
                List<Integer> registerIds = body.getJsonArray(Field.REGISTERIDS).getList();
                FutureHelper.busObjectHandler(weeklyAudiencesService.create(structureId, registerIds), message);
                break;
            case "get-statistics-graph":
                structure = body.getString("structureId");
                String indicatorName = body.getString(Field.INDICATOR);
                JsonObject filterJson = body.getJsonObject("filter");
                StatisticsFilter filter = new StatisticsFilter(structure, filterJson);
                Indicator indicator = StatisticsPresences.indicatorMap.get(indicatorName);
                indicator.searchGraph(filter, BusResultHandler.busResponseHandler(message));
                break;
            case "get-statistics":
                structure = body.getString("structureId");
                indicatorName = body.getString(Field.INDICATOR);
                filterJson = body.getJsonObject("filter");
                Integer page = body.getInteger(Field.PAGE);
                filter = new StatisticsFilter(structure, filterJson).setPage(page);
                indicator = StatisticsPresences.indicatorMap.get(indicatorName);
                indicator.search(filter, BusResultHandler.busResponseHandler(message));
                break;
            case "get-statistics-indicator":
                JsonArray indicatorJsonArray = new JsonArray(Arrays.asList(StatisticsPresences.indicatorMap.keySet().toArray()));
                JsonObject result = new JsonObject().put("indicator", indicatorJsonArray);
                message.reply(new JsonObject()
                        .put("status", "ok")
                        .put("result", result));
                break;
            default:
                message.reply(new JsonObject()
                        .put("status", "error")
                        .put("message", "Invalid action."));
        }
    }
}
