package fr.openent.statistics_presences.indicator.worker;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.SlotModel;
import fr.openent.presences.model.TimeslotModel;
import fr.openent.statistics_presences.bean.statistics.StatisticsData;
import fr.openent.statistics_presences.helper.TimeslotHelper;
import fr.openent.statistics_presences.indicator.IndicatorGeneric;
import fr.openent.statistics_presences.indicator.IndicatorWorker;
import fr.openent.statistics_presences.utils.EventType;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StatisticsWorker extends IndicatorWorker {

    @Override
    protected Future<List<StatisticsData>> fetchEventData(EventType type, String structureId, String studentId, TimeslotModel timeslot, String startDate, String endDate) {
        Future<List<StatisticsData>> future;
        switch (type) {
            case INCIDENT:
                String select = "date as start_date, date as end_date";
                future = countHandler(IndicatorGeneric.fetchIncidentValue(structureId, studentId, select, null, startDate, endDate), null);
                break;
            case DEPARTURE:
                future = retrieveEventCount(structureId, studentId, 3, null, timeslot, startDate, endDate);
                break;
            case LATENESS:
                //todo check with monthy reason param
                future = retrieveEventCount(structureId, studentId, 2, reasonIdList(structureId), timeslot, startDate, endDate);
                break;
            case NO_REASON:
                future = fetchEventCountFromPresences(structureId, studentId, new ArrayList<>(), true,
                        null, timeslot, startDate, endDate);
                break;
            case UNREGULARIZED:
                future = fetchEventCountFromPresences(structureId, studentId, reasonIdList(structureId), false,
                        false, timeslot, startDate, endDate);
                break;
            case REGULARIZED:
                future = fetchEventCountFromPresences(structureId, studentId, reasonIdList(structureId), false,
                        true, timeslot, startDate, endDate);
                break;
            case PUNISHMENT:
            case SANCTION:
                future = retrievePunishmentCount(structureId, studentId, type.toString(), startDate, endDate);
                break;
            default:
                future = Future.failedFuture(new RuntimeException("Unrecognized event type"));
        }
        return future;
    }

    private Future<List<StatisticsData>> countHandler(Future<JsonArray> requestResult, TimeslotModel timeslot) {
        Promise<List<StatisticsData>> promise = Promise.promise();
        requestResult
                .onSuccess(result -> {
                    List<StatisticsData> stats = result.stream()
                            .map(JsonObject.class::cast)
                            .map(statisticsData -> {
                                StatisticsData stat = new StatisticsData()
                                        .setReason(statisticsData.getLong(Field.REASON_ID, null))
                                        .setStartDate(statisticsData.getString(Field.START_DATE))
                                        .setEndDate(statisticsData.getString(Field.END_DATE));
                                setStatFromStartDate(stat, timeslot);
                                return stat;
                            })
                            .collect(Collectors.toList());
                    promise.complete(stats);
                })
                .onFailure(promise::fail);
        return promise.future();
    }

    private void setStatFromStartDate(StatisticsData statisticsData, TimeslotModel timeslot) {
        if (timeslot == null) return;
        SlotModel slot = getCurrentSlot(statisticsData.getStartDate(), timeslot.getSlots());
        if (slot != null) {
            statisticsData.getSlots().add(slot);
        }
    }

    private SlotModel getCurrentSlot(String date, List<SlotModel> slots) {
        long currentEventStartTime = DateHelper.parseDate(
                DateHelper.fetchTimeString(date, DateHelper.SQL_FORMAT),
                DateHelper.HOUR_MINUTES
        ).getTime();

        return slots.stream()
                .filter(slot -> DateHelper.parseDate(slot.getStartHour(), DateHelper.HOUR_MINUTES).getTime() - currentEventStartTime >= 0)
                .min((slotA, slotB) -> {
                    long dateA = DateHelper.parseDate(slotA.getStartHour(), DateHelper.HOUR_MINUTES).getTime() - currentEventStartTime;
                    long dateB = DateHelper.parseDate(slotB.getStartHour(), DateHelper.HOUR_MINUTES).getTime() - currentEventStartTime;
                    return Long.compare(dateA, dateB);
                })
                .orElse(null);
    }

    private Future<List<StatisticsData>> retrieveEventCount(String structureId, String studentId, Integer eventType, List<Integer> reasonIds,
                                                            TimeslotModel timeslot, String startDate, String endDate) {
        String select = "event.start_date, event.end_date, event.reason_id";
        return countHandler(IndicatorGeneric.retrieveEventCount(structureId, studentId, eventType, select, null, reasonIds, startDate, endDate), timeslot);
    }

    private Future<List<StatisticsData>> fetchEventCountFromPresences(String structureId, String studentId, List<Integer> reasonIds,
                                                            Boolean noReasons, Boolean regularized, TimeslotModel timeslot, String startDate, String endDate) {
        Promise<List<StatisticsData>> promise = Promise.promise();
        IndicatorGeneric.fetchEventsFromPresences(structureId, studentId, reasonIds, noReasons, regularized, startDate, endDate)
                .onSuccess(result -> {
                    List<StatisticsData> stats = result.stream()
                            .map(JsonObject.class::cast)
                            .map(event -> {
                                        StatisticsData statisticsData = new StatisticsData()
                                                .setReason(event.getJsonArray(Field.EVENTS).stream()
                                                        .map(JsonObject.class::cast)
                                                        .map(evt -> evt.getLong(Field.REASON_ID))
                                                        .filter(Objects::nonNull)
                                                        .findFirst()
                                                        .orElse(null))
                                                .setStartDate(event.getString(Field.START_DATE))
                                                .setEndDate(event.getString(Field.END_DATE));
                                        this.setStatisticsDataSlot(statisticsData, timeslot);
                                        return statisticsData;
                                    }
                            )
                            .collect(Collectors.toList());
                    promise.complete(stats);
                })
                .onFailure(promise::fail);

        return promise.future();
    }

    private void setStatisticsDataSlot(StatisticsData statisticsData, TimeslotModel timeslot) {
        List<SlotModel> slots = TimeslotHelper.getSlotModelsFromPeriod(statisticsData.getStartDate(), statisticsData.getEndDate(), timeslot.getSlots());
        statisticsData.getSlots().addAll(slots);
    }

    private Future<List<StatisticsData>> retrievePunishmentCount(String structureId, String studentId, String eventType, String startDate, String endDate) {
        Promise<List<StatisticsData>> promise = Promise.promise();
        IndicatorGeneric.retrievePunishments(structureId, studentId, eventType, startDate, endDate)
                .onSuccess(result -> {
                    List<StatisticsData> stats = result.stream()
                            .map(JsonObject.class::cast)
                            .flatMap(punishmentsHolder -> {
                                JsonArray punishments = punishmentsHolder.getJsonArray(Field.PUNISHMENTS);
                                return punishments.stream()
                                        .map(JsonObject.class::cast)
                                        .map(punishment -> {
                                            StatisticsData statisticsData = new StatisticsData()
                                                    .setPunishmentType(punishment.getLong(Field.TYPEID))
                                                    .setGroupedPunishmentId(punishment.getString(Field.GROUPED_PUNISHMENT_ID));
                                            setDatesFromPunishments(punishment, statisticsData);
                                            return statisticsData;
                                        });
                            })
                            .collect(Collectors.toList());
                    promise.complete(stats);
                })
                .onFailure(promise::fail);
        return promise.future();
    }

    private void setDatesFromPunishments(JsonObject punishment, StatisticsData stat) {
        JsonObject fields = punishment.getJsonObject(Field.FIELDS, new JsonObject());

        String startAt = fields.getString(Field.START_AT);
        String endAt = fields.getString(Field.END_AT);
        if (startAt != null && endAt != null) {
            stat.setStartDate(startAt)
                    .setEndDate(endAt);
            return;
        }

        String delayAt = fields.getString(Field.DELAY_AT);
        if (delayAt != null) {
            stat.setStartDate(delayAt)
                    .setEndDate(delayAt);
            return;
        }

        String createdAt = punishment.getString(Field.CREATED_AT);
        stat.setStartDate(createdAt)
                .setEndDate(createdAt);
    }
}
