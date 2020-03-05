package fr.openent.presences.model.Person;

import io.vertx.core.json.JsonObject;

public class User extends Person implements Cloneable {

    public User(String studentId) {
        super();
        this.id = studentId;
    }

    public User(String studentId, String info) {
        super();
        this.id = studentId;
        this.info = info;
    }

    public User(JsonObject user) {
        this.id = user.getString("id");
        this.displayName = user.getString("displayName");
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("id", this.id)
                .put("displayName", this.displayName)
                .put("info", this.info);
    }

    @Override
    public User clone() {
        try {
            return (User) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }
}
