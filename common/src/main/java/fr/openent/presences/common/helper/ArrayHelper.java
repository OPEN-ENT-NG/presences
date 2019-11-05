package fr.openent.presences.common.helper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Comparator;
import java.util.List;

public class ArrayHelper {

    public static JsonArray sort(JsonArray values, String fieldname) {
        List items = values.getList();
        items.sort((Comparator<JsonObject>) (o1, o2) -> o1.getString(fieldname).compareToIgnoreCase(o2.getString(fieldname)));

        return new JsonArray(items);
    }
}
