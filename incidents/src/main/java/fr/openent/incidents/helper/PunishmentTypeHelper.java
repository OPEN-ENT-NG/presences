package fr.openent.incidents.helper;

import fr.openent.incidents.model.PunishmentType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class PunishmentTypeHelper {

    /**
     * Convert JsonArray into punishment type list
     *
     * @param array jsonArray response
     * @return new list of events
     */
    public static List<PunishmentType> getPunishmentTypeListFromJsonArray(JsonArray array) {
        List<PunishmentType> punishmentTypeList = new ArrayList<>();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            PunishmentType punishmentType =
                    new PunishmentType((JsonObject) o);
            punishmentTypeList.add(punishmentType);
        }
        return punishmentTypeList;
    }

    /**
     * Convert List punishmentsType into punishmentsType JsonArray
     *
     * @param punishmentTypeList punishmentType list
     * @return new JsonArray of punishmentType
     */
    public static JsonArray toJsonArray(List<PunishmentType> punishmentTypeList) {
        JsonArray punishmentsType = new JsonArray();
        for (PunishmentType punishmentType : punishmentTypeList) {
            punishmentsType.add(punishmentType.toJsonObject());
        }
        return punishmentsType;
    }
}

