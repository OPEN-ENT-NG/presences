package fr.openent.statistics_presences.helper;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.statistics_presences.bean.timeslot.Slot;
import fr.openent.statistics_presences.bean.timeslot.Timeslot;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class TimeslotHelper {

    private TimeslotHelper() {
        throw new IllegalStateException("Utility class");
    }

    @SuppressWarnings("unchecked")
    public static List<Timeslot> getRegistersFromArray(JsonArray timeslots) {
        return ((List<JsonObject>) timeslots.getList()).stream()
                .map(Timeslot::new)
                .collect(Collectors.toList());
    }

    public static List<Slot> getSlotsFromPeriod(String startAt, String endAt, List<Slot> slots) {
        long currentEventStartTime = DateHelper.parseDate(
                DateHelper.fetchTimeString(startAt, DateHelper.SQL_FORMAT),
                DateHelper.HOUR_MINUTES
        ).getTime();

        long currentEventEndTime = DateHelper.parseDate(
                DateHelper.fetchTimeString(endAt, DateHelper.SQL_FORMAT),
                DateHelper.HOUR_MINUTES
        ).getTime();

        //todo à tester en debug
        return slots
                .stream()
                .filter(slot ->
                        DateHelper.parseDate(slot.getEndHour(), DateHelper.HOUR_MINUTES).getTime() - currentEventStartTime > 0
                                && currentEventEndTime - DateHelper.parseDate(slot.getStartHour(), DateHelper.HOUR_MINUTES).getTime() > 0
                )
                .collect(Collectors.toList());
    }
}

