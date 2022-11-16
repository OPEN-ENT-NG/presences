package fr.openent.statistics_presences.helper;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.model.SlotModel;
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

    /**
     * {@link Timeslot} is deprecated
     *
     * @deprecated  Replaced by {@link fr.openent.presences.common.helper.IModelHelper#toList(JsonArray, Class)} by using
     * {@link fr.openent.presences.model.TimeslotModel}
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public static List<Timeslot> getTimeslotsFromArray(JsonArray timeslots) {
        return ((List<JsonObject>) timeslots.getList()).stream()
                .map(Timeslot::new)
                .collect(Collectors.toList());
    }

    /**
     * {@link Slot} is deprecated
     *
     * @deprecated  Replaced by {@link #getSlotModelsFromPeriod(String, String, List)} by using
     * {@link fr.openent.presences.model.SlotModel}
     */
    @Deprecated
    public static List<Slot> getSlotsFromPeriod(String startAt, String endAt, List<Slot> slots) {
        long currentEventStartTime = DateHelper.parseDate(
                DateHelper.fetchTimeString(startAt, DateHelper.SQL_FORMAT),
                DateHelper.HOUR_MINUTES
        ).getTime();

        long currentEventEndTime = DateHelper.parseDate(
                DateHelper.fetchTimeString(endAt, DateHelper.SQL_FORMAT),
                DateHelper.HOUR_MINUTES
        ).getTime();

        return slots
                .stream()
                .filter(slot ->
                        DateHelper.parseDate(slot.getEndHour(), DateHelper.HOUR_MINUTES).getTime() - currentEventStartTime > 0
                                && currentEventEndTime - DateHelper.parseDate(slot.getStartHour(), DateHelper.HOUR_MINUTES).getTime() > 0
                )
                .collect(Collectors.toList());
    }

    public static List<SlotModel> getSlotModelsFromPeriod(String startAt, String endAt, List<SlotModel> slots) {
        long currentEventStartTime = DateHelper.parseDate(
                DateHelper.fetchTimeString(startAt, DateHelper.SQL_FORMAT),
                DateHelper.HOUR_MINUTES
        ).getTime();

        long currentEventEndTime = DateHelper.parseDate(
                DateHelper.fetchTimeString(endAt, DateHelper.SQL_FORMAT),
                DateHelper.HOUR_MINUTES
        ).getTime();

        return slots
                .stream()
                .filter(slot ->
                        DateHelper.parseDate(slot.getEndHour(), DateHelper.HOUR_MINUTES).getTime() - currentEventStartTime > 0
                                && currentEventEndTime - DateHelper.parseDate(slot.getStartHour(), DateHelper.HOUR_MINUTES).getTime() > 0
                )
                .collect(Collectors.toList());
    }
}

