package fr.openent.presences.model.grouping;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.IModel;
import io.vertx.core.json.JsonObject;

public class StudentDivision implements IModel<StudentDivision> {
    private String id;
    private String name;

    public StudentDivision(JsonObject studentDivision) {
        this.id = studentDivision.getString(Field.ID, "");
        this.name = studentDivision.getString(Field.NAME, "");
    }

    public String getName() {
        return name;
    }

    public StudentDivision setName(String name) {
        this.name = name;
        return this;
    }

    public String getId() {
        return id;
    }

    public StudentDivision setId(String id) {
        this.id = id;
        return this;
    }

    public JsonObject toJson() {
        return new JsonObject().put(Field.ID, this.id).put(Field.NAME, this.name);
    }

    @Override
    public boolean validate() {
        return this.name != null && !this.name.isEmpty()
                && this.id != null && !this.id.isEmpty();
    }
}
