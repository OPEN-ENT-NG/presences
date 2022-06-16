package fr.openent.statistics_presences.indicator.worker;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.bean.Stat;
import fr.openent.statistics_presences.bean.timeslot.Slot;
import fr.openent.statistics_presences.bean.weekly.WeeklyStat;
import fr.openent.statistics_presences.bean.timeslot.Timeslot;
import fr.openent.statistics_presences.helper.TimeslotHelper;
import fr.openent.statistics_presences.indicator.IndicatorGeneric;
import fr.openent.statistics_presences.indicator.IndicatorWorker;
import fr.openent.statistics_presences.utils.EventType;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

public class Weekly extends IndicatorWorker {

    /**
     * Fetch events in queue and set their count values.
     *
     * @param type        event type
     * @param structureId structure identifier
     * @param studentId   student identifier
     * @param timeslot   student timeslot
     * @return future with event process
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Future<List<Stat>> fetchEvent(EventType type, String structureId, String studentId, Timeslot timeslot) {
        Future<List<Stat>> future;
        switch (type) {
            case DEPARTURE:
                future = retrieveEventCount(structureId, studentId, 3, null, timeslot);
                break;
            case LATENESS:
                future = retrieveEventCount(structureId, studentId, 2, reasonIds(structureId).getList(), timeslot);
                break;
            case NO_REASON:
                future = fetchEventCountFromPresences(structureId, studentId, new ArrayList<>(), true,
                        false, timeslot);
                break;
            case UNREGULARIZED:
                future = fetchEventCountFromPresences(structureId, studentId, reasonIds(structureId).getList(), false,
                        false, timeslot);
                break;
            case REGULARIZED:
                future = fetchEventCountFromPresences(structureId, studentId, reasonIds(structureId).getList(), false,
                        true, timeslot);
                break;
            case INCIDENT:
            case SANCTION:
            case PUNISHMENT:
                future = Future.succeededFuture(Collections.emptyList());
                break;
            default:
                future = Future.failedFuture(new RuntimeException("Unrecognized event type"));
        }

        return future;
    }

    private Future<List<Stat>> countHandler(Future<JsonArray> requestResult, Timeslot timeslot) {
        Promise<List<Stat>> promise = Promise.promise();
        requestResult
                .onSuccess(result -> {
                    List<Stat> stats = new ArrayList<>();
                    for (int i = 0; i < result.size(); i++) {
                        JsonObject incident = result.getJsonObject(i);
                        stats.add(setStatFromStartDate(incident, timeslot).setReason(incident.getLong(Field.REASON_ID, null)));
                    }

                    promise.complete(stats);
                })
                .onFailure(promise::fail);
        return promise.future();
    }

    @SuppressWarnings("unchecked")
    private Future<List<Stat>> fetchEventCountFromPresences(String structureId, String studentId, List<Integer> reasonIds,
                                                            Boolean noReasons, Boolean regularized, Timeslot timeslot) {
        Promise<List<Stat>> promise = Promise.promise();
        IndicatorGeneric.fetchEventsFromPresences(structureId, studentId, reasonIds, noReasons, regularized)
                .onSuccess(result -> {
                    List<Stat> stats = ((List<JsonObject>) result.getList()).stream()
                            .flatMap(event -> {
                                List<WeeklyStat> slotStats = getSplitEventsBySlots(event, timeslot);
                                Long reasonId = ((List<JsonObject>) event.getJsonArray(Field.EVENTS).getList()).stream()
                                        .map(evt -> evt.getLong(Field.REASON_ID))
                                        .filter(Objects::nonNull)
                                        .findFirst()
                                        .orElse(null);

                                slotStats.forEach(stat -> stat.setReason(reasonId));
                                return slotStats.stream();
                            })
                            .collect(Collectors.toList());
                    promise.complete(stats);
                })
                .onFailure(promise::fail);

        return promise.future();
    }

    private Future<List<Stat>> retrieveEventCount(String structureId, String studentId, Integer eventType, List<Integer> reasonIds, Timeslot timeslot) {
        String select = "event.start_date, event.end_date, event.reason_id";
        return countHandler(IndicatorGeneric.retrieveEventCount(structureId, studentId, eventType, select, null, reasonIds), timeslot);
    }

    private List<WeeklyStat> getSplitEventsBySlots(JsonObject event, Timeslot timeslot) {
        List<Slot> slots = TimeslotHelper.getSlotsFromPeriod(event.getString(Field.START_DATE), event.getString(Field.END_DATE), timeslot.getSlots());


        if (slots == null || slots.isEmpty()) {
            String message = String.format("[StatisticsPresences@%s::getSplitEventsBySlots] " +
                            "Slots not found for event %s",
                    this.getClass().getSimpleName(), event.getJsonArray(Field.EVENTS, new JsonArray()).toString());
            log.debug(message);
            return Collections.singletonList(new WeeklyStat().setSlotId(null)
                    .setStartDate(event.getString(Field.START_DATE))
                    .setEndDate(event.getString(Field.END_DATE)));
        }
        return slots.stream()
                .map(slot -> createStatFromStartDate(event, slot))
                .collect(Collectors.toList());
    }

    private WeeklyStat setStatFromStartDate(JsonObject event, Timeslot timeslot) {
        Slot slot = getCurrentSlot(event.getString(Field.START_DATE), timeslot.getSlots());
        if (slot == null) {
            return new WeeklyStat().setSlotId(null)
                    .setStartDate(event.getString(Field.START_DATE))
                    .setEndDate(event.getString(Field.END_DATE));
        }
        return createStatFromStartDate(event, slot);
    }

    private WeeklyStat createStatFromStartDate(JsonObject event, Slot slot) {
        return new WeeklyStat()
                .setSlotId(slot.getId())
                .setStartDate(DateHelper.setTimeToDate(
                        event.getString(Field.START_DATE), slot.getStartHour(), DateHelper.HOUR_MINUTES, DateHelper.SQL_FORMAT
                ))
                .setEndDate(DateHelper.setTimeToDate(
                        event.getString(Field.START_DATE), slot.getEndHour(), DateHelper.HOUR_MINUTES, DateHelper.SQL_FORMAT
                ));
    }

    private Slot getCurrentSlot(String date, List<Slot> slots) {
        long currentEventStartTime = DateHelper.parseDate(
                DateHelper.fetchTimeString(date, DateHelper.SQL_FORMAT),
                DateHelper.HOUR_MINUTES
        ).getTime();

        return slots
                .stream()
                .filter(slot -> DateHelper.parseDate(slot.getStartHour(), DateHelper.HOUR_MINUTES).getTime() - currentEventStartTime >= 0)
                .min((slotA, slotB) -> {
                    long dateA = DateHelper.parseDate(slotA.getStartHour(), DateHelper.HOUR_MINUTES).getTime() - currentEventStartTime;
                    long dateB = DateHelper.parseDate(slotB.getStartHour(), DateHelper.HOUR_MINUTES).getTime() - currentEventStartTime;
                    return Long.compare(dateA, dateB);
                }).orElse(null);
    }

}
