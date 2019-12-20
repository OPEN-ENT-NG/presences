package fr.openent.presences.helper;

import fr.openent.presences.model.Absence;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class AbsenceHelper {

    public static List<Absence> getAbsenceListFromJsonArray(JsonArray slotsJsonArray, List<String> mandatoryAttributes) {
        List<Absence> absences = new ArrayList<>();
        for (Object o : slotsJsonArray) {
            if (!(o instanceof JsonObject)) continue;
            Absence absence = new Absence((JsonObject) o, mandatoryAttributes);
            absences.add(absence);
        }
        return absences;
    }
}
