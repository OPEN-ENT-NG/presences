package fr.openent.presences.common.statistics_presences;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.message.MessageResponseHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class StatisticsPresences {

    private String address = "fr.openent.statistics.presences";
    private EventBus eb;

    public static StatisticsPresences getInstance() {
        return StatisticsPresencesHolder.instance;
    }

    public void init(EventBus eb) {
        this.eb = eb;
    }

    public void postUsers(String structureId, List<String> studentIds,
                          Handler<AsyncResult<JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "post-users")
                .put("structureId", structureId)
                .put("studentIds", studentIds);

        eb.send(address, action, MessageResponseHandler.messageJsonObjectHandler(FutureHelper.handlerJsonObject(handler)));
    }

    private static class StatisticsPresencesHolder {
        private static final StatisticsPresences instance = new StatisticsPresences();

        private StatisticsPresencesHolder() {
        }
    }
}
