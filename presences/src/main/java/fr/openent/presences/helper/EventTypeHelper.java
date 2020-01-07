package fr.openent.presences.helper;

import fr.openent.presences.Presences;
import fr.openent.presences.model.Event.EventType;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.List;

public class EventTypeHelper {

    public static List<EventType> getEventTypeListFromJsonArray(JsonArray eventTypeJsonArray, List<String> mandatoryAttributes) {
        List<EventType> eventTypes = new ArrayList<>();
        for (Object o : eventTypeJsonArray) {
            if (!(o instanceof JsonObject)) continue;
            EventType slot = new EventType((JsonObject) o, mandatoryAttributes);
            eventTypes.add(slot);
        }
        return eventTypes;
    }

    public void getEventType(List<Integer> eventTypeIds, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Presences.dbSchema + ".event_type where id IN " + Sql.listPrepared(eventTypeIds);
        JsonArray params = new JsonArray().addAll(new JsonArray(eventTypeIds));
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
