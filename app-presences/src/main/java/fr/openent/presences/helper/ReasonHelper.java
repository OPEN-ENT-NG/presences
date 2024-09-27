package fr.openent.presences.helper;

import fr.openent.presences.model.Reason;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ReasonHelper {


    /**
     * Convert JsonArray into reason list
     *
     * @param array               JsonArray response
     * @param mandatoryAttributes List of mandatory attributes
     * @return new list of events
     */
    public static List<Reason> getReasonListFromJsonArray(JsonArray array, List<String> mandatoryAttributes) {
        List<Reason> reasonList = new ArrayList<>();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            Reason reason = new Reason((JsonObject) o, mandatoryAttributes);
            reasonList.add(reason);
        }
        return reasonList;
    }
}
