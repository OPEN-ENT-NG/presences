package fr.openent.presences.model.Event;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class RegisterEvent {

    private String studentId;
    private JsonArray events;

    public RegisterEvent(JsonObject registerEvent) {
        this.studentId = registerEvent.getString("student_id", null);
        this.events = new JsonArray(registerEvent.getString("events", null));
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public JsonArray getEvents() {
        return events;
    }

    public void setEvents(JsonArray events) {
        this.events = events;
    }
}
