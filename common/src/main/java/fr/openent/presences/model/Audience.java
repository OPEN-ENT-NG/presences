package fr.openent.presences.model;

import fr.openent.presences.model.Person.Student;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class Audience {

    private String id;
    private String name;
    private Integer countStudents;

    private List<Student> students;

    public Audience(JsonObject audience) {
        this.id = audience.getString("id");
        this.name = audience.getString("name");
        this.countStudents = audience.getInteger("countStudents");
        this.students = Student.students(audience.getJsonArray("students", new JsonArray()));
    }

    public static List<Audience> audiences(JsonArray audiences) {
        return (audiences != null) ? audiences.stream().map(audience -> new Audience((JsonObject) audience)).collect(Collectors.toList()) : null;
    }

    public JsonObject toJSON() {
        JsonObject result = new JsonObject()
                .put("id", this.id)
                .put("name", this.name);

        if (countStudents != null) result.put("countStudents", countStudents);
        if (students != null && !students.isEmpty()) result.put("students", Student.toJSON(students));
        return result;
    }

    public static JsonArray toJSON(List<Audience> audiences) {
        return new JsonArray(audiences.stream().map(Audience::toJSON).collect(Collectors.toList()));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void addStudent(Student student) {
        this.students.add(student);
    }


}
