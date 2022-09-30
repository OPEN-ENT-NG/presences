package fr.openent.presences.common.helper;


import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.EventModel;
import fr.openent.presences.model.SlotModel;
import fr.openent.presences.model.TimeslotModel;
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

    /**
     * @deprecated Replaced by {@link #mergeEventsByDates(JsonArray, String, Map)}
     */
    @Deprecated public static JsonArray mergeEventsByDates(JsonArray events, String halfDay) {
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
            if (earliestEvent != null && latestEvent != null) {
                groupEvents.stream()
                        .collect(Collectors.groupingBy(el -> el.getString(Field.STUDENT_ID)))
                        .forEach((s, studentEvents) -> newEvents.add(
                                setGroupedEvent(studentEvents, earliestEvent.getString(Field.START_DATE), latestEvent.getString(Field.END_DATE))
                        ));
            }
        });

        // return events sorted by date desc
        return new JsonArray(
                newEvents.stream().sorted((JsonObject eventA, JsonObject eventB) ->
                        DateHelper.isDateBeforeOrEqual(eventB.getString(Field.START_DATE), eventA.getString(Field.START_DATE)) ? -1 : 1
                ).collect(Collectors.toList())
        );
    }

    public static JsonArray mergeEventsByDates(JsonArray events, String halfDay, Map<String, TimeslotModel> mapStudentIdTimeslot) {
        if (halfDay == null || events == null) {
            return events;
        }

        // map with event
        Map<String, List<JsonObject>> dateListEventMap = addGroupEventsByDateAndHalfday(events, halfDay);
        List<JsonObject> newEvents = new ArrayList<>();

        //foreach day
        dateListEventMap.forEach((day, groupEvents) -> {
            if (groupEvents == null || groupEvents.isEmpty()) return;

            List<EventModel> eventModelList = IModelHelper.toList(new JsonArray(groupEvents), EventModel.class);
            final Map<String, List<EventModel>> mapStudentIdEvent = eventModelList.stream()
                    .collect(Collectors.groupingBy(eventModel -> eventModel.getStudent().getId()));

            mapStudentIdEvent.forEach((studentId, studentEventList) ->
                    newEvents.addAll(groupBySuccessiveEvent(mapStudentIdTimeslot.get(studentId), studentEventList)));
        });

        return new JsonArray(
                newEvents.stream().sorted((JsonObject eventA, JsonObject eventB) ->
                        DateHelper.isDateBeforeOrEqual(eventB.getString(Field.START_DATE), eventA.getString(Field.START_DATE)) ? -1 : 1
                ).collect(Collectors.toList()));
    }

    /**
     * Groups the events that follow each other according to their slot
     *
     * @param timeslotModel Timeslot on which to base to recover the slot of event
     * @param eventList Event list
     * @return list of JsonObject
     */
    private static List<JsonObject> groupBySuccessiveEvent(TimeslotModel timeslotModel, List<EventModel> eventList) {
        List<JsonObject> newEventList = new ArrayList<>();
        Map<EventModel, SlotModel> mapEventSlot = getTimeslotFromEvent(timeslotModel, eventList);

        // *1 Here we order the events by start date.
        // *2 So if n is not followed by n + 1 then it cannot be followed by n + 2, n + 3 etc.
        eventList.sort(EventModel::compareStartDate);
        // For each event we will check if the next events follow.
        for (int i = 0; i < eventList.size(); i++) {
            EventModel eventModelEarly = eventList.get(i);
            EventModel eventModelLatest = eventList.get(i);
            // This list is used to store the events that we will group together.
            List<EventModel> groupedEventList = new ArrayList<>();
            // The first is necessarily groupable with itself.
            groupedEventList.add(eventList.get(i));

            // For each event that are in the future (See *1) we will check if they follow the eventModelLatest
            for (int j = i + 1; j < eventList.size(); j++) {
                if (mapEventSlot.getOrDefault(eventModelLatest, null) != null
                        //If the events follow by their slot (or if it's the same)
                        && (mapEventSlot.get(eventModelLatest).equals(mapEventSlot.get(eventList.get(j)))
                        || timeslotModel.isNextSlot(mapEventSlot.get(eventModelLatest), mapEventSlot.get(eventList.get(j))))) {
                    // We define the new last element of the group.
                    // We have a ++i because when we continue the loop j we go if the next events follow.
                    eventModelLatest = eventList.get(++i);
                    // And we add it to the group
                    groupedEventList.add(eventList.get(i));
                } else {
                    // Otherwise we stop loop j (See *2)
                    break;
                }
            }
            // Here we make our event grouping thanks to our event list
            newEventList.add(defineGroupedEvent(groupedEventList, eventModelEarly.getStartDate(), eventModelLatest.getEndDate()));
        }

        return newEventList;
    }

    /**
     * Allows you to associate events with a slot based on a timeslot.
     * We only take into account the start date to find its slot.
     * If the event starts on slot id 1 and ends on slot id 2, then we associate it with slot id1.
     *
     * @param timeslotModel Timeslot on which to base to recover the Slot
     * @param eventList Event list
     * @return map with event as key and this slot as value
     */
    private static Map<EventModel, SlotModel> getTimeslotFromEvent(TimeslotModel timeslotModel, List<EventModel> eventList) {
        Map<EventModel, SlotModel> eventModelSlotModelMap = new HashMap<>();
        eventList.forEach(eventModel -> {
            final Date eventDate = DateHelper.parseDate(eventModel.getStartDate(), DateHelper.SQL_FORMAT);
            SlotModel slot = null;
            List<SlotModel> slotModelList = timeslotModel.getSlots();
            slotModelList.sort(SlotModel::compareTo);
            for (SlotModel slotModel: slotModelList) {
                Calendar slotCalendar = Calendar.getInstance();
                slotCalendar.setTime(eventDate);
                slotCalendar.set(Calendar.HOUR_OF_DAY, DateHelper.getField(slotModel.getStartHour(), DateHelper.HOUR_MINUTES, Calendar.HOUR_OF_DAY));
                slotCalendar.set(Calendar.MINUTE, DateHelper.getField(slotModel.getStartHour(), DateHelper.HOUR_MINUTES, Calendar.MINUTE));
                if (slot == null || slotCalendar.getTime().before(eventDate) || slotCalendar.getTime().equals(eventDate)) {
                    slot = slotModel;
                } else {
                    break;
                }
            }

            eventModelSlotModelMap.put(eventModel, slot);
        });
        return eventModelSlotModelMap;
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

    private static JsonObject defineGroupedEvent(List<EventModel> events, String startDate, String endDate) {
        if (events == null || events.isEmpty()) return new JsonObject();

        JsonObject newEvent = events.get(0).toJson();

        JsonArray individualEvents = new JsonArray();
        events.forEach(event -> individualEvents.add(event.toJson()));
        newEvent.put(Field.EVENTS, individualEvents);

        return setDatesEvent(newEvent, startDate, endDate);
    }

    /**
     * @deprecated Replaced by {@link #defineGroupedEvent(List, String, String)}
     */
    @Deprecated private static JsonObject setGroupedEvent(List<JsonObject> events, String startDate, String endDate) {
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
