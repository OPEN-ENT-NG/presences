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
        this.id = user.getString("id", null);
        this.displayName = user.getString("displayName", null);
        this.firstName = user.getString("firstName", null);
        this.lastName = user.getString("lastName", null);
    }

    public JsonObject toJSON() {
        JsonObject result = new JsonObject()
                .put("id", this.id)
                .put("displayName", this.displayName)
                .put("info", this.info);

        if (this.firstName != null) result.put("firstName", this.firstName);
        if (this.lastName != null) result.put("lastName", this.lastName);

        return result;
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
