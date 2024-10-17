package fr.openent.presences.model.Event;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.helper.EventHelper;
import fr.openent.presences.model.Person.Student;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;

public class EventByStudent {

    private Student student;
    private final String startDate;
    private final String endDate;
    private final EventType eventType;
    private final String recoveryMethod;
    private final List<Event> events;


    public EventByStudent(JsonObject eventByStudent) {
        this.student = new Student(eventByStudent.getString(Field.STUDENT_ID, null));
        this.startDate = eventByStudent.getString(Field.START_DATE, null);
        this.endDate = eventByStudent.getString(Field.END_DATE, null);
        this.eventType = new EventType(eventByStudent.getInteger(Field.TYPEID, null));
        this.recoveryMethod =  eventByStudent.getString(Field.RECOVERY_METHOD, null);
        this.events = EventHelper.getEventListFromJsonArray(eventByStudent.getJsonArray(Field.EVENTS, new JsonArray()), Event.MANDATORY_ATTRIBUTE);
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put(Field.STUDENT, this.student.toJSON())
                .put(Field.START_DATE, this.startDate)
                .put(Field.END_DATE, this.endDate)
                .put(Field.EVENT_TYPE, this.eventType.toJSON())
                .put(Field.RECOVERY_METHOD, this.recoveryMethod)
                .put(Field.EVENTS, EventHelper.getEventListToJsonArray(this.events));
    }

    public EventByStudent setStudent(Student student) {
        this.student = student;
        return this;
    }

    public Student student() {
        return student;
    }


    public String startDate() {
        return startDate;
    }

    public String endDate() {
        return endDate;
    }

    public EventType eventType() {
        return eventType;
    }

    public String recoveryMethod() {
        return recoveryMethod;
    }

    public List<Event> events() {
        return events;
    }

}
