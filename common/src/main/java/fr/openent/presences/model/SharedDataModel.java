package fr.openent.presences.model;

import fr.openent.presences.common.helper.SharedDataHelper;
import fr.openent.presences.core.constants.Field;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public interface SharedDataModel<I extends IModel<I>> extends IModel<I>{
    I load(JsonObject jsonObject);
    String getKey();

    default Future<I> save(Vertx vertx) {
        Promise<I> promise = Promise.promise();
        SharedDataHelper.setObjectFromAsyncMap(vertx, Field.PRESENCES_SHARED_DATA_MAP_NAME, this.getKey(), this.toJson(), false)
                .onFailure(promise::fail)
                .onSuccess(result -> promise.complete((I) this));
        return promise.future();
    }
}
