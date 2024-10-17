package fr.openent.presences.helper;

import fr.openent.presences.model.Discipline;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class DisciplineHelper {

    /**
     * Convert JsonArray into discipline list
     *
     * @param array jsonArray response
     * @return new list of events
     */
    public static List<Discipline> getDisciplineListFromJsonArray(JsonArray array) {
        List<Discipline> disciplineList = new ArrayList<>();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            Discipline discipline = new Discipline((JsonObject) o);
            disciplineList.add(discipline);
        }
        return disciplineList;
    }

    /**
     * Convert List disciplines into discipline JsonArray
     *
     * @param disciplinesList disciplines list
     * @return new JsonArray of disciplines
     */
    public static JsonArray toJsonArray(List<Discipline> disciplinesList) {
        JsonArray disciplines = new JsonArray();
        for (Discipline discipline : disciplinesList) {
            disciplines.add(discipline.toJSON());
        }
        return disciplines;
    }
}
