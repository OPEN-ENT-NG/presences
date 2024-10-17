package fr.openent.presences.helper;

import fr.openent.presences.model.CollectiveAbsence;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class CollectiveAbsenceHelper {

    /**
     * Convert JsonArray into collective absence list
     *
     * @param collectiveAbsenceArray jsonArray response
     * @return new list of events
     */
    public static List<CollectiveAbsence> getCollectiveAbsenceListFromJsonArray(JsonArray collectiveAbsenceArray) {
        List<CollectiveAbsence> collectiveAbsenceList = new ArrayList<>();
        for (Object o : collectiveAbsenceArray) {
            if (!(o instanceof JsonObject)) continue;
            CollectiveAbsence collectiveAbsence = new CollectiveAbsence((JsonObject) o);
            collectiveAbsenceList.add(collectiveAbsence);
        }
        return collectiveAbsenceList;
    }
}
