package fr.openent.presences.helper;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.model.Event.EventType;
import fr.openent.presences.model.Settings;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringEscapeUtils;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.Collections;
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

    public static Future<EventType> getEventType(Integer eventTypeId) {
        Promise<EventType> promise = Promise.promise();

        String query = "SELECT * FROM " + Presences.dbSchema + ".event_type where id = ? ";
        JsonArray params = new JsonArray().add(eventTypeId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(IModelHelper.sqlUniqueResultToIModel(promise, EventType.class, "[Presences@%s::getEventType] an error has occurred during retrieving event types")));

        return promise.future();
    }

    public static Future<String> getEventLabel(Integer eventTypeId) {
        Promise<String> promise = Promise.promise();

        getEventType(eventTypeId)
                .onSuccess(eventType -> promise.complete(eventType.getLabel()))
                .onFailure(promise::fail);

        return promise.future();
    }
}
