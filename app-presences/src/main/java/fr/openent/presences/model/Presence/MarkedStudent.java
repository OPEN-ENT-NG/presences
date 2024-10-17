package fr.openent.presences.model.Presence;

import fr.openent.presences.model.Person.Student;
import io.vertx.core.json.JsonObject;

public class MarkedStudent implements Cloneable {

    private Integer presenceId;
    private String comment;
    private Student student;

    public MarkedStudent() {

    }

    public MarkedStudent(JsonObject markedStudent) {
        this.presenceId = markedStudent.getInteger("presence_id", null);
        this.comment = markedStudent.getString("comment", null);
        this.student = new Student(markedStudent.getString("student_id", null));
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put("presenceId", this.presenceId)
                .put("comment", this.comment)
                .put("student", this.student.toJSON());
    }

    @Override
    public MarkedStudent clone() {
        try {
            return (MarkedStudent) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }

    public Integer getPresenceId() {
        return presenceId;
    }

    public void setPresenceId(Integer presenceId) {
        this.presenceId = presenceId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

}
