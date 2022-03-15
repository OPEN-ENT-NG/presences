package fr.openent.statistics_presences.indicator.impl;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.bean.weekly.WeeklySearch;
import fr.openent.statistics_presences.indicator.Indicator;
import fr.openent.statistics_presences.indicator.IndicatorGeneric;
import fr.openent.statistics_presences.model.StatisticsFilter;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class Weekly extends Indicator {
    public static final Integer PAGE_SIZE = 35;
    private final Logger log = LoggerFactory.getLogger(Weekly.class);

    public Weekly(Vertx vertx, String name) {
        super(vertx, name);
    }

    @Override
    public void search(StatisticsFilter filter, Handler<AsyncResult<JsonObject>> handler) {
        setSearchUserWithAudiences(filter)
                .compose(this::searchProcess)
                .onComplete(handler);
    }

    @SuppressWarnings("unchecked")
    private Future<WeeklySearch> setSearchUserWithAudiences(StatisticsFilter filter) {
        Promise<WeeklySearch> promise = Promise.promise();
        WeeklySearch search = new WeeklySearch(filter);

        if (!filter.users().isEmpty()) {
            filter.setUserId(filter.users().get(0));
            IndicatorGeneric.retrieveAudiences(filter.structure(), filter.userId())
                    .onFailure(err -> {
                        String message = String.format("[StatisticsPresences@%s::setSearchUserWithAudiences] " +
                                "Indicator %s failed to retrieve settings", this.getClass().getSimpleName(), Weekly.class.getName());
                        log.error(String.format("%s. %s", message, err.getMessage()));
                        promise.fail(message);
                    })
                    .onSuccess(audienceIds -> {
                        filter.setAudiences(audienceIds.getList());
                        promise.complete(search);
                    });
        } else if (!filter.audiences().isEmpty()) {
            filter.setAudiences(Collections.singletonList(filter.audiences().get(0)));
            promise.complete(search);
        } else {
            String message = String.format("[StatisticsPresences@%s::setSearchUserWithAudiences] " +
                    "Indicator %s search need one audience or one student to retrieve data.", this.getClass().getSimpleName(),
                    Weekly.class.getName());
            log.error(message);
            promise.fail(message);
        }

        return promise.future();
    }


    @Override
    public void searchGraph(StatisticsFilter filter, Handler<AsyncResult<JsonObject>> handler) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    private Future<JsonObject> searchProcess(WeeklySearch search) {
        Promise<JsonObject> promise = Promise.promise();


        Future<JsonArray> countEventsBySlotsFuture = retrieveStatistics(search.countEventTypedBySlotsCommand());
        Future<JsonArray> studentCountBySlotsFuture = retrieveStatistics(search.countStudentsBySlotsCommand());

        CompositeFuture.all(countEventsBySlotsFuture, studentCountBySlotsFuture)
                .onSuccess(ar -> {
                    List<JsonObject> values = (List<JsonObject>) ((List<JsonObject>) countEventsBySlotsFuture.result().getList()).stream()
                            .map(eventCount ->
                                    formatEventToRateSlots(eventCount, studentCountBySlotsFuture.result().getList()))
                            .collect(Collectors.toList());

                    setMaxValue(values);
                    JsonObject response = new JsonObject()
                            .put(Field.DATA, values);

                    promise.complete(response);
                })
                .onFailure(fail -> {
                    log.error(String.format("[StatisticsPresences@Global::searchProcess] " +
                            "Indicator %s failed to complete search", Weekly.class.getName()), fail.getCause());
                    promise.fail(fail.getCause());
                });
        return promise.future();
    }

    private JsonObject formatEventToRateSlots(JsonObject eventCount, List<JsonObject> studentCountBySlots) {
        JsonObject studentCount = studentCountBySlots.stream()
                .filter(studentsSlot ->
                        studentsSlot.getString(Field.SLOT_ID, "")
                                .equals(eventCount.getString(Field.SLOT_ID))
                        && studentsSlot.getInteger(Field.DAYOFWEEK) != null  && studentsSlot.getInteger(Field.DAYOFWEEK)
                                .equals(eventCount.getInteger(Field.DAYOFWEEK))
                )
                .findFirst()
                .orElse(new JsonObject().put(Field.COUNT, 0));

        return new JsonObject()
                .put(Field.SLOT_ID, eventCount.getString(Field.SLOT_ID))
                .put(Field.DAYOFWEEK, DateHelper.getDayOfWeek(eventCount.getInteger(Field.DAYOFWEEK)))
                .put(Field.RATE, getEventRates(studentCount.getInteger(Field.COUNT),
                        eventCount.getInteger(Field.COUNT)));
    }


    private double getEventRates(Integer studentCount, Integer eventCount) {
        double rate = (studentCount == null || eventCount == null ? 0.0 : Math.abs((((double)eventCount) * 100) / ((double)studentCount)));
        return  getValidRate(Math.min(rate, 100));
    }

    private double getValidRate(double rate) {
        if (Double.isInfinite(rate) || Double.isNaN(rate)) return 0.0;
        return BigDecimal.valueOf(Math.min(rate, 100)).setScale(2, RoundingMode.HALF_DOWN).doubleValue();
    }

    public Future<JsonArray> retrieveStatistics(JsonObject command) {
        Promise<JsonArray> promise = Promise.promise();
        if (command == null || command.isEmpty()) {
            promise.complete(new JsonArray());
            return promise.future();
        }

        mongoDb.command(command.toString(), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                String message = String.format("[StatisticsPresences@%s::retrieveStatistics] " +
                                "Indicator %s failed to execute mongodb aggregation pipeline", this.getClass().getSimpleName(),
                        Weekly.class.getName());
                log.error(String.format("%s. %s", message, either.left().getValue()));
                promise.fail(message);
                return;
            }
            JsonObject result = either.right().getValue();
            if (result.getJsonObject("cursor") == null) {
                String error = either.right().getValue().getString("errmsg");
                String message = String.format("[StatisticsPresences@%s::retrieveStatistics] Indicator %s failed to execute " +
                        "mongodb aggregation pipeline.", this.getClass().getSimpleName(), Weekly.class.getName());
                log.error(String.format("%s. %s", message, error));
                promise.fail(message);
                return;
            }


            promise.complete(result.getJsonObject("cursor").getJsonArray("firstBatch", new JsonArray()));
        }));

        return promise.future();
    }

    private void setMaxValue(List<JsonObject> rateSlots) {
        rateSlots.stream()
                .max(Comparator.comparing(rateSlot -> rateSlot.getDouble(Field.RATE, 0.0)))
                .ifPresent(rateSlot -> {
                    if (rateSlot.getDouble(Field.RATE, 0.0) != 0.0) rateSlot.put(Field.MAX, Boolean.TRUE);
                });
    }
}
