package fr.openent.presences.model.grouping;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.IModel;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class Grouping implements IModel<Grouping> {
    private String id;
    private String name;
    private String structureId;
    private List<StudentDivision> studentDivisions;

    public Grouping(JsonObject grouping) {
        this.id = grouping.getString(Field.ID, "");
        this.name = grouping.getString(Field.NAME, "");
        this.structureId = grouping.getString(Field.STRUCTURE_ID, "");
        this.studentDivisions = grouping.getJsonArray(Field.STUDENT_DIVISIONS, new JsonArray()).stream()
                .map(JsonObject.class::cast)
                .map(StudentDivision::new)
                .collect(Collectors.toList());
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStructureId() {
        return structureId;
    }

    public List<StudentDivision> getStudentDivisions() {
        return studentDivisions;
    }

    public Grouping setId(String id) {
        this.id = id;
        return this;
    }

    public Grouping setName(String name) {
        this.name = name;
        return this;
    }

    public Grouping setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    public Grouping setStudentDivisions(List<StudentDivision> studentDivisions) {
        this.studentDivisions = studentDivisions;
        return this;
    }

    public JsonObject toJson() {
        return new JsonObject().put(Field.ID, this.id).put(Field.NAME, this.name).put(Field.STRUCTURE_ID, this.structureId)
                .put(Field.STUDENT_DIVISIONS, new JsonArray(this.studentDivisions));
    }

    @Override
    public boolean validate() {
        return this.id != null && !this.id.isEmpty()
                && this.name != null && !this.name.isEmpty()
                && this.structureId != null && this.structureId.isEmpty()
                && this.studentDivisions.stream().allMatch(StudentDivision::validate);
    }
}
