package fr.openent.presences.model;

import io.vertx.core.json.JsonObject;


public abstract class Model {
    public abstract JsonObject toJsonObject();
}
