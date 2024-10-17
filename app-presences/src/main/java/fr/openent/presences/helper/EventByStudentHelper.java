package fr.openent.presences.helper;

import fr.openent.presences.model.Event.EventByStudent;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class EventByStudentHelper {

    private EventByStudentHelper() { throw new IllegalStateException("Helper class"); }

    public static List<EventByStudent> eventByStudentList(JsonArray eventsByStudents) {
        return eventsByStudents.stream().map(eventByStudent -> new EventByStudent((JsonObject) eventByStudent)).collect(Collectors.toList());
    }

    public static List<JsonObject> eventByStudentToJsonArray(List<EventByStudent> eventsByStudents) {
        return eventsByStudents.stream().map(EventByStudent::toJSON).collect(Collectors.toList());
    }
}