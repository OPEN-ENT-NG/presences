package fr.openent.presences.helper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MapHelper {
    public static JsonObject transformToMap(JsonArray objects, String key) {
        JsonObject object;
        JsonObject map = new JsonObject();
        for (int i = 0; i < objects.size(); i++) {
            object = objects.getJsonObject(i);
            map.put(object.getString(key), object);
        }

        return map;
    }

    public static JsonObject transformToMapMultiple(JsonArray objects, String key) {
        JsonObject object;
        JsonObject map = new JsonObject();
        for (int i = 0; i < objects.size(); i++) {
            object = objects.getJsonObject(i);
            if (!map.containsKey(object.getString(key))) {
                map.put(object.getString(key), new JsonArray());
            }

            map.getJsonArray(object.getString(key)).add(object);
        }

        return map;
    }
}
