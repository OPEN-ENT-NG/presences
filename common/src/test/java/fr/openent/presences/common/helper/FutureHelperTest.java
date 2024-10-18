package fr.openent.presences.common.helper;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static fr.openent.presences.common.helper.FutureHelper.handlerEitherPromise;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FutureHelperTest {

    private String error = "This is an error message";

    @Test
    @DisplayName("FutureHelper.handlerEitherPromise should succeed the future when succeeding the handler")
    public void handlerEitherPromise_JsonArray_should_succeed_the_future_when_succeeding_the_handler() {
        Promise<JsonArray> promise = Promise.promise();
        Handler<Either<String, JsonArray>> handler = handlerEitherPromise(promise);
        handler.handle(new Either.Right<>(new JsonArray()));
        assertTrue(promise.future().succeeded() && promise.future().result() != null && promise.future().result().size() == 0);
    }

    @Test
    @DisplayName("FutureHelper.handlerEitherPromise should failed the future when failing the handler")
    public void handlerEitherPromise_JsonArray_should_failed_the_future_when_failing_the_handler() {
        Promise<JsonArray> promise = Promise.promise();
        Handler<Either<String, JsonArray>> handler = handlerEitherPromise(promise);
        handler.handle(new Either.Left<>(error));
        assertTrue(promise.future().failed() && promise.future().cause() != null && error.equals(promise.future().cause().getMessage()));
    }


    @Test
    @DisplayName("FutureHelper.handlerEitherPromise should succeed the future when succeeding the handler")
    public void handlerEitherPromise_JsonObject_should_succeed_the_future_when_succeeding_the_handler() {
        Promise<JsonObject> promise = Promise.promise();
        Handler<Either<String, JsonObject>> handler = handlerEitherPromise(promise);
        handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
        assertTrue(promise.future().succeeded() && promise.future().result() != null && "ok".equals(promise.future().result().getString("status")));
    }

    @Test
    @DisplayName("FutureHelper.handlerEitherPromise should failed the future when failing the handler")
    public void handlerEitherPromise_JsonObject_the_future_when_failing_the_handler() {
        Promise<JsonObject> promise = Promise.promise();
        Handler<Either<String, JsonObject>> handler = handlerEitherPromise(promise);
        handler.handle(new Either.Left<>(error));
        assertTrue(promise.future().failed() && promise.future().cause() != null && error.equals(promise.future().cause().getMessage()));
    }

}
