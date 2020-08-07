package fr.openent.statistics_presences.bean;

import io.vertx.core.json.JsonObject;

public class User {
    private String id;
    private String name;
    private String audience;
    private Value value;

    public User(String id, String name, String audience) {
        this.id = id;
        this.name = name;
        this.audience = audience;
    }

    public User setValue(Value value) {
        this.value = value;
        return this;
    }

    public String id() {
        return this.id;
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("id", this.id)
                .put("name", this.name)
                .put("audience", this.audience)
                .put("statistics", this.value != null ? this.value.toJson() : new JsonObject());
    }
}
