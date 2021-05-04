package fr.openent.statistics_presences.controller;

import fr.openent.presences.common.bus.BusResultHandler;
import fr.openent.statistics_presences.service.StatisticsPresencesService;
import fr.openent.statistics_presences.service.impl.DefaultStatisticsPresencesService;
import fr.wseduc.bus.BusAddress;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

public class EventBusController extends ControllerHelper {

    private final StatisticsPresencesService statisticsService;

    public EventBusController() {
        statisticsService = new DefaultStatisticsPresencesService();
    }

    @BusAddress("fr.openent.statistics.presences")
    @SuppressWarnings("unchecked")
    public void bus(final Message<JsonObject> message) {
        JsonObject body = message.body();
        String action = body.getString("action");
        if ("post-users".equals(action)) {
            statisticsService.create(
                    body.getString("structureId"), body.getJsonArray("studentIds").getList(),
                    BusResultHandler.busResponseHandler(message)
            );
            return;
        }

        message.reply(new JsonObject()
                .put("status", "error")
                .put("message", "Invalid action."));
    }
}
