package fr.openent.presences.model.Person;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Student extends Person implements Cloneable {

    private String classId;
    private String className;
    private JsonArray dayHistory;

    public Student(JsonObject student) {
        super();
        this.id = student.getString("id", null);
        this.displayName = student.getString("displayName", null);
        this.classId = student.getString("classId", null);
        this.className = student.getString("classeName", null);
        this.dayHistory = student.getJsonArray("day_history", new JsonArray());
    }

    public Student(String studentId) {
        super();
        this.id = studentId;
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("id", this.id)
                .put("displayName", this.displayName)
                .put("classId", this.classId)
                .put("classeName", this.className)
                .put("day_history", this.dayHistory);
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
}
