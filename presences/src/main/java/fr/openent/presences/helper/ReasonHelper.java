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

    public static List<Reason> getReasonsInit() {
        List<Reason> reasons = new ArrayList<>();
        reasons.add(new Reason("presences.reasons.init.0", false));
        reasons.add(new Reason("presences.reasons.init.1", true));
        reasons.add(new Reason("presences.reasons.init.2", true));
        reasons.add(new Reason("presences.reasons.init.3", false));
        reasons.add(new Reason("presences.reasons.init.4", false));
        reasons.add(new Reason("presences.reasons.init.5", true));
        reasons.add(new Reason("presences.reasons.init.6", false));
        reasons.add(new Reason("presences.reasons.init.7", false));
        reasons.add(new Reason("presences.reasons.init.8", false));
        reasons.add(new Reason("presences.reasons.init.9", true));
        reasons.add(new Reason("presences.reasons.init.10", true));
        reasons.add(new Reason("presences.reasons.init.11", true));
        reasons.add(new Reason("presences.reasons.init.12", false));
        reasons.add(new Reason("presences.reasons.init.13", false));
        reasons.add(new Reason("presences.reasons.init.14", true));
        reasons.add(new Reason("presences.reasons.init.15", true));
        reasons.add(new Reason("presences.reasons.init.16", true));
        reasons.add(new Reason("presences.reasons.init.17", false));
        reasons.add(new Reason("presences.reasons.init.18", true));
        reasons.add(new Reason("presences.reasons.init.19", true));
        reasons.add(new Reason("presences.reasons.init.20", false));
        reasons.add(new Reason("presences.reasons.init.21", false));
        reasons.add(new Reason("presences.reasons.init.22", true));
        reasons.add(new Reason("presences.reasons.init.23", true));
        reasons.add(new Reason("presences.reasons.init.24", false));
        reasons.add(new Reason("presences.reasons.init.25", true));
        reasons.add(new Reason("presences.reasons.init.26", true));
        reasons.add(new Reason("presences.reasons.init.27", true));
        reasons.add(new Reason("presences.reasons.init.28", false));

        return reasons;
    }
}
