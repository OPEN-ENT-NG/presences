package fr.openent.statistics_presences.service.impl;

import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.service.StatisticsService;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;

import java.util.List;

public class DefaultStatisticsService implements StatisticsService {
    private final Logger log = LoggerFactory.getLogger(DefaultStatisticsService.class);
    private String indicatorName;

    public DefaultStatisticsService(String indicatorName) {
        this.indicatorName = indicatorName;
    }

    @Deprecated
    @Override
    public void save(String structureId, JsonArray students, List<JsonObject> values, Handler<AsyncResult<Void>> handler) {
        this.save(structureId, students, values, null, null, handler);
    }

    @Override
    public void save(String structureId, JsonArray students, List<JsonObject> values, String startDate, String endDate,
                        Handler<AsyncResult<Void>> handler) {
        if (values == null || values.isEmpty()) {
            deleteOldValues(structureId, students, values, startDate, endDate).onComplete(event -> {
                if (event.failed()) {
                    handler.handle(Future.failedFuture(event.cause()));
                } else {
                    handler.handle(Future.succeededFuture());
                }
            });
            return;
        }

        deleteOldValues(structureId, students, values, startDate, endDate)
                .compose(this::storeValues)
                .onComplete(handler);
    }

    @Override
    public Future<List<JsonObject>> overrideStatisticsStudent(String structureId, String studentId, List<JsonObject> values, String startDate,
                                                              String endDate) {
        Promise<List<JsonObject>> promise = Promise.promise();

        Future<List<JsonObject>> future = deleteOldValuesForStudent(structureId, studentId, values, startDate, endDate);

        if (values.isEmpty()) {
            future.onComplete(promise);
        } else {
            future.compose(this::storeValues)
                    .onSuccess(res -> promise.complete(values))
                    .onFailure(promise::fail);
        }

        return promise.future();
    }

    /**
     * No filter date
     *
     * @deprecated Replaced by {@link #deleteOldValues(String, JsonArray, List, String, String)}
     */
    @Deprecated
    private Future<List<JsonObject>> deleteOldValues(String structureId, JsonArray students, List<JsonObject> values) {
        return deleteOldValues(structureId, students, values, null, null);
    }

    private Future<List<JsonObject>> deleteOldValues(String structureId, JsonArray students, List<JsonObject> values, String startDate, String endDate) {
        Promise<List<JsonObject>> promise = Promise.promise();
        JsonObject $in = new JsonObject()
                .put(Field.$IN, students);
        JsonObject selector = new JsonObject()
                .put(Field.INDICATOR, this.indicatorName)
                .put(Field.STRUCTURE, structureId)
                .put(Field.USER, $in);
        if (startDate != null && endDate != null) {
            JsonObject $gte = new JsonObject()
                    .put(Field.$GTE, startDate);
            JsonObject $lte = new JsonObject()
                    .put(Field.$LTE, endDate);
            selector.put(Field.START_DATE, $gte)
                    .put(Field.END_DATE, $lte);
        }

        MongoDb.getInstance().delete(StatisticsPresences.COLLECTION, selector, MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@DefaultStatisticsService::deleteOldValues] " +
                                "Failed to remove old statistics for indicator %s. %s",
                        this.indicatorName,
                        either.left().getValue()
                ));
                promise.fail(either.left().getValue());
            } else {
                promise.complete(values);
            }
        }));

        return promise.future();
    }

    private Future<List<JsonObject>> deleteOldValuesForStudent(String structureId, String studentId, List<JsonObject> values, String startDate, String endDate) {
        Promise<List<JsonObject>> promise = Promise.promise();
        JsonObject selector = new JsonObject()
                .put(Field.STRUCTURE, structureId)
                .put(Field.USER, studentId);
        if (startDate != null && endDate != null) {
            JsonObject $gte = new JsonObject()
                    .put(Field.$GTE, startDate);
            JsonObject $lte = new JsonObject()
                    .put(Field.$LTE, endDate);
            selector.put(Field.START_DATE, $gte)
                    .put(Field.END_DATE, $lte);
        }

        MongoDb.getInstance().delete(StatisticsPresences.COLLECTION, selector, MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@DefaultStatisticsService::deleteOldValuesForStudent] " +
                                "Failed to remove old statistics for student %s for indicator %s. %s",
                        studentId, this.indicatorName, either.left().getValue()
                ));
                promise.fail(either.left().getValue());
            } else {
                promise.complete(values);
            }
        }));

        return promise.future();
    }

    private Future<Void> storeValues(List<JsonObject> values) {
        Promise<Void> promise = Promise.promise();
        MongoDb.getInstance().insert(StatisticsPresences.COLLECTION, new JsonArray(values), MongoDbResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[StatisticsPresences@DefaultStatisticsService::storeValues] " +
                                "%s indicator failed to store new values. %s",
                        this.indicatorName,
                        either.left().getValue()
                ));
                promise.fail(either.left().getValue());
            } else {
                promise.complete();
            }
        }));

        return promise.future();
    }
}
