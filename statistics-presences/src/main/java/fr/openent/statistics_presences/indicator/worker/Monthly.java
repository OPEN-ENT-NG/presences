package fr.openent.statistics_presences.indicator.worker;

import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.bean.Stat;
import fr.openent.statistics_presences.bean.monthly.MonthlyStat;
import fr.openent.statistics_presences.bean.timeslot.Timeslot;
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


/**
 * We keep the same instruction like the Global Worker as we figured the behavior should be the same
 * We might still be able to "evolve" this behavior
 */

public class Monthly extends IndicatorWorker {

    /**
     * Fetch events in queue and set their count values.
     *
     * @param type        event type
     * @param structureId structure identifier
     * @param studentId   student identifier
     * @return future with event process
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Future<List<Stat>> fetchEvent(EventType type, String structureId, String studentId, Timeslot timeslot) {
        Future<List<Stat>> future;
        switch (type) {
            case INCIDENT:
                String select = "date as start_date, date as end_date";
                future = countHandler(IndicatorGeneric.fetchIncidentValue(structureId, studentId, select, null));
                break;
            case DEPARTURE:
                future = retrieveEventCount(structureId, studentId, 3, reasonIds(structureId).getList());
                break;
            case LATENESS:
                future = retrieveEventCount(structureId, studentId, 2, reasonIds(structureId).getList());
                break;
            case NO_REASON:
                future = fetchEventCountFromPresences(structureId, studentId, new ArrayList<>(), true, null);
                break;
            case UNREGULARIZED:
                future = fetchEventCountFromPresences(structureId, studentId, reasonIds(structureId).getList(), false, false);
                break;
            case REGULARIZED:
                future = fetchEventCountFromPresences(structureId, studentId, reasonIds(structureId).getList(), false, true);
                break;
            case PUNISHMENT:
            case SANCTION:
                future = retrievePunishmentCount(structureId, studentId, type.toString());
                break;
            default:
                future = Future.failedFuture(new RuntimeException("Unrecognized event type"));
        }

        return future;
    }

    private Future<List<Stat>> countHandler(Future<JsonArray> requestResult) {
        Promise<List<Stat>> promise = Promise.promise();
        requestResult
                .onSuccess(result -> {
                    List<Stat> stats = new ArrayList<>();
                    for (int i = 0; i < result.size(); i++) {
                        JsonObject incident = result.getJsonObject(i);
                        MonthlyStat stat = new MonthlyStat()
                                .setReason(incident.getLong(Field.REASON_ID, null))
                                .setStartDate(incident.getString(Field.START_DATE))
                                .setEndDate(incident.getString(Field.END_DATE));
                        stats.add(stat);
                    }

                    promise.complete(stats);
                })
                .onFailure(promise::fail);
        return promise.future();
    }

    @SuppressWarnings("unchecked")
    private Future<List<Stat>> fetchEventCountFromPresences(String structureId, String studentId, List<Integer> reasonIds,
                                                            Boolean noReasons, Boolean regularized) {
        Promise<List<Stat>> promise = Promise.promise();
        IndicatorGeneric.fetchEventsFromPresences(structureId, studentId, reasonIds, noReasons, regularized)
                .onSuccess(result -> {
                    List<Stat> stats = ((List<JsonObject>) result.getList()).stream()
                            .map(event -> new MonthlyStat()
                                    .setReason(((List<JsonObject>) event.getJsonArray("events").getList()).stream()
                                            .map(evt -> evt.getLong("reason_id"))
                                            .filter(Objects::nonNull)
                                            .findFirst()
                                            .orElse(null))
                                    .setStartDate(event.getString("start_date"))
                                    .setEndDate(event.getString("end_date"))
                            )
                            .collect(Collectors.toList());
                    promise.complete(stats);
                })
                .onFailure(promise::fail);

        return promise.future();
    }

    private Future<List<Stat>> retrieveEventCount(String structureId, String studentId, Integer eventType, List<Integer> reasonIds) {
        String select = "event.start_date, event.end_date, event.reason_id";
        return countHandler(IndicatorGeneric.retrieveEventCount(structureId, studentId, eventType, select, null, reasonIds));
    }

    @SuppressWarnings("unchecked")
    private Future<List<Stat>> retrievePunishmentCount(String structureId, String studentId, String eventType) {
        Promise<List<Stat>> promise = Promise.promise();
        IndicatorGeneric.retrievePunishments(structureId, studentId, eventType)
                .onSuccess(result -> {
                    List<Stat> stats = ((List<JsonObject>) result.getList()).stream()
                            .flatMap(punishmentsHolder -> {
                                List<JsonObject> punishments = punishmentsHolder.getJsonArray("punishments").getList();
                                return punishments.stream().map(punishment -> {
                                    String createdAt = punishment.getString(Field.CREATED_AT);
                                    MonthlyStat stat = new MonthlyStat()
                                            .setPunishmentType(punishment.getLong(Field.TYPEID))
                                            .setGroupedPunishmentId(punishment.getString(Field.GROUPED_PUNISHMENT_ID))
                                            .setStartDate(createdAt)
                                            .setEndDate(createdAt);
                                    return stat;
                                });
                            })
                            .collect(Collectors.toList());
                    promise.complete(stats);
                })
                .onFailure(promise::fail);
        return promise.future();
    }
}
