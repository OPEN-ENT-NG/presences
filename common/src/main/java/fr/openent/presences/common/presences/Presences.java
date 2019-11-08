package fr.openent.presences.common.presences;

import fr.openent.presences.common.message.MessageResponseHandler;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class Presences {
    private String address = "fr.openent.presences";
    private EventBus eb;

    private Presences() {
    }

    public static Presences getInstance() {
        return PresencesHolder.instance;
    }

    public void init(EventBus eb) {
        this.eb = eb;
    }

    public void getCountEventByStudent(Integer eventType, List<String> students, String structure, Boolean justified, Integer startAt, List<Integer> reasonsId, Boolean massmailed,
                                       String startDate, String endDate, boolean noReasons, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("eventType", eventType)
                .put("justified", justified)
                .put("students", new JsonArray(students))
                .put("structure", structure)
                .put("startAt", startAt)
                .put("reasonsId", reasonsId)
                .put("massmailed", massmailed)
                .put("startDate", startDate)
                .put("endDate", endDate)
                .put("noReasons", noReasons)
                .put("action", "get-count-event-by-student");
        eb.send(address, action, MessageResponseHandler.messageJsonArrayHandler(handler));
    }

    public void getEventsByStudent(List<Integer> eventTypes, List<String> students, String structure, Boolean justified, List<Integer> reasonsId, Boolean massmailed,
                                   String startDate, String endDate, Boolean noReasons, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("eventType", new JsonArray(eventTypes))
                .put("justified", justified)
                .put("students", new JsonArray(students))
                .put("structure", structure)
                .put("reasonsId", reasonsId)
                .put("massmailed", massmailed)
                .put("startDate", startDate)
                .put("endDate", endDate)
                .put("noReasons", noReasons)
                .put("action", "get-events-by-student");
        eb.send(address, action, MessageResponseHandler.messageJsonArrayHandler(handler));
    }

    public void getReasons(String structure, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("structure", structure)
                .put("action", "get-reasons");
        eb.send(address, action, MessageResponseHandler.messageJsonArrayHandler(handler));
    }

    private static class PresencesHolder {
        private static final Presences instance = new Presences();

        private PresencesHolder() {
        }
    }
}
