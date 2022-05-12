package fr.openent.presences.model.Person;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.Audience;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class Student extends Person implements Cloneable {

    private String classId;
    private String className;
    private JsonArray dayHistory;

    private List<Audience> audiences;

    public Student(JsonObject student) {
        super();
        this.id = student.getString(Field.ID, null);
        this.displayName = student.getString(Field.DISPLAYNAME, null);
        this.classId = student.getString(Field.CLASSID, null);
        this.className = student.getString("classeName", student.getString(Field.CLASSNAME, null));
        this.firstName = student.getString(Field.FIRSTNAME, null);
        this.lastName = student.getString(Field.LASTNAME, null);
        this.dayHistory = student.getJsonArray(Field.DAY_HISTORY, new JsonArray());

        this.audiences = Audience.audiences(student.getJsonArray(Field.AUDIENCES, new JsonArray()));
    }

    public Student(String studentId) {
        super();
        this.id = studentId;
    }

    public static List<Student> students(JsonArray students) {
        return students.stream().map(student -> new Student((JsonObject) student)).collect(Collectors.toList());
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("id", this.id)
                .put("displayName", this.displayName)
                .put("firstName", this.firstName)
                .put("lastName", this.lastName)
                .put("classId", this.classId)
                .put("classeName", this.className)
                .put("day_history", this.dayHistory);
    }

    public static JsonArray toJSON(List<Student> students) {
        return new JsonArray(students.stream().map(Student::toJSON).collect(Collectors.toList()));
    }

    @Override
    public Student clone() {
        try {
            return (Student) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getDisplayName() {
        return this.displayName != null ? this.displayName : this.lastName + " " + this.firstName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public JsonArray getDayHistory() {
        return dayHistory;
    }

    public void setDayHistory(JsonArray dayHistory) {
        this.dayHistory = dayHistory;
    }

    public List<Audience> getAudiences() {
        return audiences;
    }

    public void addAudience(Audience audience) {
        this.audiences.add(audience);
    }
}
