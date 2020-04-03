package fr.openent.presences.common.helper;

import fr.openent.presences.model.Model;
import fr.wseduc.webutils.collections.JsonArray;

import java.util.List;

public class ModelHelper {
    public static JsonArray convertToJsonArray(List<? extends Model> modelInterfaceList) {
        JsonArray jsonArrayModel = new JsonArray();
        if (!modelInterfaceList.isEmpty()) {
            for (Model modelInstance : modelInterfaceList) {
                jsonArrayModel.add(modelInstance.toJsonObject());
            }
        }
        return jsonArrayModel;
    }
}