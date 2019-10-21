package fr.openent.presences.common.incidents;

import fr.openent.presences.common.message.MessageResponseHandler;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class Incidents {
    private String address = "fr.openent.incidents";
    private EventBus eb;

    private Incidents() {
    }

    public static Incidents getInstance() {
        return IncidentsHolder.instance;
    }

    public void init(EventBus eb) {
        this.eb = eb;
    }

    /**
     * Retrieve incidents for given users in date range
     *
     * @param startDate range start date
     * @param endDate   range end date
     * @param users     user list
     * @param handler   Function handler returning data
     */
    public void getIncidents(String startDate, String endDate, List<String> users, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "get-incidents-users-range")
                .put("startDate", startDate)
                .put("endDate", endDate)
                .put("users", new JsonArray(users));

        eb.send(address, action, MessageResponseHandler.messageJsonArrayHandler(handler));
    }

    private static class IncidentsHolder {
        private static final Incidents instance = new Incidents();

        private IncidentsHolder() {
        }
    }
}
