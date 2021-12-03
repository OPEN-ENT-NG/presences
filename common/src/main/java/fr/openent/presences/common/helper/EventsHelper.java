package fr.openent.presences.common.helper;


import fr.openent.presences.core.constants.Field;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

public class EventsHelper {

    private EventsHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static JsonObject getEarliestEvent(List<JsonObject> events) {
        return events.stream().min((eventA, eventB) ->
                DateHelper.isDateBeforeOrEqual(eventB.getString(Field.START_DATE), eventA.getString(Field.START_DATE)) ? 1 : -1
        ).orElse(null);
    }

    public static JsonObject getLatestEvent(List<JsonObject> events) {
        return events.stream().max((eventA, eventB) ->
                DateHelper.isDateBeforeOrEqual(eventB.getString(Field.START_DATE), eventA.getString(Field.START_DATE)) ? 1 : -1
        ).orElse(null);
    }

    public static JsonArray mergeEventsByDates(JsonArray events, String halfDay) {
        if (halfDay == null || events == null) {
            return events;
        }

        List<JsonObject> newEvents = new ArrayList<>();
        Map<String, List<JsonObject>> dateGroupedEvents = addGroupEventsByDateAndHalfday(events, halfDay);

        dateGroupedEvents.forEach((key, groupEvents) -> {
            if (groupEvents == null || groupEvents.isEmpty()) return;

            JsonObject earliestEvent = getEarliestEvent(groupEvents);
            JsonObject latestEvent = getLatestEvent(groupEvents);

            // we set new Event start date with the earliest events start_date and end date with the latest events end_date
            if (earliestEvent != null && latestEvent != null)
                newEvents.add(setGroupedEvent(groupEvents, earliestEvent.getString("start_date"), latestEvent.getString("end_date")));
        });

        // return events sorted by date desc
        return new JsonArray(
                newEvents.stream().sorted((JsonObject eventA, JsonObject eventB) ->
                        DateHelper.isDateBeforeOrEqual(eventB.getString("start_date"), eventA.getString("start_date")) ? -1 : 1
                ).collect(Collectors.toList())
        );
    }

    @SuppressWarnings("unchecked")
    public static List<JsonObject> setDatesFromNestedEvents(List<JsonObject> events, String halfDay) {
        if (halfDay == null || events == null) return events;

        return events.stream()
                .map(event -> {
                    JsonObject earliestEvent = getEarliestEvent(event.getJsonArray(Field.EVENTS).getList());
                    JsonObject latestEvent = getLatestEvent(event.getJsonArray(Field.EVENTS).getList());
                    return setDatesEvent(event,
                            earliestEvent != null ? earliestEvent.getString(Field.START_DATE) : event.getString(Field.START_DATE),
                            latestEvent != null ? latestEvent.getString(Field.END_DATE) : event.getString(Field.END_DATE)
                    );
                }).collect(Collectors.toList());
    }

    private static Map<String, List<JsonObject>> addGroupEventsByDateAndHalfday(JsonArray events, String halfDay) {
        Map<String, List<JsonObject>> dateGroupedEvents = new HashMap<>();

        for (Object o : events) {
            JsonObject event = (JsonObject) o;
            String eventStartDate = event.getString("start_date");
            String eventHalfDay = DateHelper.setTimeToDate(eventStartDate, halfDay, DateHelper.HOUR_MINUTES_SECONDS, DateHelper.SQL_FORMAT);
            // We group events by day and if start event date is before halfDay (studentId-yyyy-MM-dd_<true|false>)
            String groupKey =
                    event.getJsonObject("student", new JsonObject()).getString("id", "") + "-" +
                            DateHelper.getDateString(eventStartDate, DateHelper.SQL_FORMAT, DateHelper.YEAR_MONTH_DAY)
                            + "_" + DateHelper.isDateBefore(eventStartDate, eventHalfDay);

            if (dateGroupedEvents.containsKey(groupKey)) dateGroupedEvents.get(groupKey).add(event);
            else dateGroupedEvents.put(groupKey, new ArrayList<>(Collections.singletonList(event)));
        }

        return dateGroupedEvents;
    }

    private static JsonObject setGroupedEvent(List<JsonObject> events, String startDate, String endDate) {
        if (events == null || events.isEmpty()) return new JsonObject();

        JsonObject newEvent = events.get(0).copy();

        JsonArray individualEvents = new JsonArray();
        events.forEach(event -> individualEvents.addAll(event.getJsonArray(Field.EVENTS, new JsonArray())));
        newEvent.put(Field.EVENTS, individualEvents);

        return setDatesEvent(newEvent, startDate, endDate);
    }

    private static JsonObject setDatesEvent(JsonObject event, String startDate, String endDate) {
        event.put(Field.START_DATE, startDate);
        event.put(Field.DISPLAY_START_DATE, startDate);
        event.put(Field.END_DATE, endDate);
        event.put(Field.DISPLAY_END_DATE, endDate);
        return event;
    }
}
