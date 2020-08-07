package fr.openent.statistics_presences.bean.global;

import fr.openent.statistics_presences.bean.Value;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GlobalValue extends Value {
    private Map<String, JsonObject> values = new HashMap<>();

    public GlobalValue setValue(String key, JsonObject value) {
        this.values.put(key, value);
        return this;
    }

    public Set<String> keys() {
        return this.values.keySet();
    }

    public JsonObject value(String key) {
        return this.values.get(key);
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject((Map) values);
    }
}
