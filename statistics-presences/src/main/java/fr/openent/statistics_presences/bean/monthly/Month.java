package fr.openent.statistics_presences.bean.monthly;

import fr.openent.statistics_presences.bean.Value;
import io.vertx.core.json.JsonObject;

public class Month extends Value {
    private final String key;
    private final Statistic statistic;

    public Month(String key, Statistic statistic) {
        this.key = key;
        this.statistic = statistic;
    }

    public String key() {
        return this.key;
    }

    public Statistic statistic() {
        return this.statistic;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(key, statistic.toJson());
    }
}
