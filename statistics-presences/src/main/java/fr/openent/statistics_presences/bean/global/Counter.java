package fr.openent.statistics_presences.bean.global;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Counter {
    private int count;
    private Integer slotsCount;
    private JsonArray reasons;

    public Counter(int count) {
        this.count = count;
    }

    public Counter setSlotsCount(int slotsCount) {
        this.slotsCount = slotsCount;
        return this;
    }

    public Counter setReasons(JsonArray reasons) {
        this.reasons = reasons;
        return this;
    }

    public JsonObject toJSON() {
        JsonObject value = new JsonObject()
                .put("count", this.count);

        if (this.slotsCount != null) {
            value.put("slots", this.slotsCount);
        }

        if (this.reasons != null) {
            value.put("reasons", this.reasons);
        }

        return value;
    }
}
