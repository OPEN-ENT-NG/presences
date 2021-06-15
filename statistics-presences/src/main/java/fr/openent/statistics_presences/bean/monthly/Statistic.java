package fr.openent.statistics_presences.bean.monthly;

import fr.openent.statistics_presences.bean.Value;
import io.vertx.core.json.JsonObject;

public class Statistic extends Value {
    private final Integer count;
    private final Integer slots;
    private Boolean max;

    public Statistic(Integer count, Integer slots) {
        this.count = count;
        this.slots = slots;
    }

    public Integer count() {
        return this.count;
    }

    public Integer slots() {
        return this.slots;
    }

    public void setMax(Boolean max) {
        this.max = max;
    }

    @Override
    public JsonObject toJson() {
        JsonObject res = new JsonObject()
                .put("count", count)
                .put("slots", slots);

        if (Boolean.TRUE.equals(max)) res.put("max", true);
        return res;
    }
}
