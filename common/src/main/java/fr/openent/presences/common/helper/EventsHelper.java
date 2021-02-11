package fr.openent.presences.common.helper;


import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

public class EventsHelper {

    private EventsHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static JsonArray mergeEventsByDates(JsonArray events, String halfDay) {
        if (halfDay == null || events == null) {
            return events;
        }

        List<JsonObject> newEvents = new ArrayList<>();
        Map<String, List<JsonObject>> dateGroupedEvents = addGroupEventsByDateAndHalfday(events, halfDay);

        dateGroupedEvents.forEach((key, groupEvents) -> {
            if (groupEvents == null || groupEvents.isEmpty()) return;

            JsonObject earliestEvent = groupEvents.stream().min((eventA, eventB) ->
                    DateHelper.isDateBeforeOrEqual(eventB.getString("start_date"), eventA.getString("start_date")) ? 1 : -1
            ).orElse(null);

            JsonObject latestEvent = groupEvents.stream().max((eventA, eventB) ->
                    DateHelper.isDateBeforeOrEqual(eventB.getString("start_date"), eventA.getString("start_date")) ? 1 : -1
            ).orElse(null);

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

    private static Map<String, List<JsonObject>> addGroupEventsByDateAndHalfday(JsonArray events, String halfDay) {
        Map<String, List<JsonObject>> dateGroupedEvents = new HashMap<>();

        for (Object o : events) {
            JsonObject event = (JsonObject) o;
            String eventStartDate = event.getString("start_date");
            String eventHalfDay = DateHelper.setTimeToDate(eventStartDate, halfDay, DateHelper.HOUR_MINUTES_SECONDS, DateHelper.SQL_FORMAT);
            // We group events by day and if start event date is before halfDay (yyyy-MM-dd_<true|false>)
            String groupKey =
                    DateHelper.getDateString(eventStartDate, DateHelper.SQL_FORMAT, DateHelper.YEAR_MONTH_DAY)
                            + "_" + DateHelper.isDateBeforeOrEqual(eventStartDate, eventHalfDay);

            if (dateGroupedEvents.containsKey(groupKey)) dateGroupedEvents.get(groupKey).add(event);
            else dateGroupedEvents.put(groupKey, new ArrayList<>(Collections.singletonList(event)));
        }

        return dateGroupedEvents;
    }

    private static JsonObject setGroupedEvent(List<JsonObject> events, String startDate, String endDate) {
        if (events == null || events.isEmpty()) {
            return new JsonObject();
        }

        JsonObject newEvent = events.get(0).copy();
        JsonArray individualEvents = new JsonArray();

        events.forEach(event -> individualEvents.addAll(event.getJsonArray("events", new JsonArray())));
        newEvent.put("events", individualEvents);
        newEvent.put("start_date", startDate);
        newEvent.put("display_start_date", startDate);
        newEvent.put("end_date", endDate);
        newEvent.put("display_end_date", endDate);

        return newEvent;
    }
}
