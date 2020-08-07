package fr.openent.statistics_presences.bean;

import io.vertx.core.json.JsonObject;

public abstract class Value {
    public abstract JsonObject toJson();
}
