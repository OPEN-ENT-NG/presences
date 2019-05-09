package fr.openent.presences.common.helper;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static fr.openent.presences.common.helper.FutureHelper.handlerJsonArray;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FutureHelperTest {

    private String error = "This is an error message";

    @Test
    @DisplayName("FutureHelper.handlerJsonArray should succeed the future when succeeding the handler")
    public void handlerJsonArray_should_succeed_the_future_when_succeeding_the_handler() {
        Future<JsonArray> future = Future.future();
        Handler<Either<String, JsonArray>> handler = handlerJsonArray(future);
        handler.handle(new Either.Right<>(new JsonArray()));
        assertTrue(future.succeeded() && future.result() != null && future.result().size() == 0);
    }

    @Test
    @DisplayName("FutureHelper.handlerJsonArray should failed the future when failing the handler")
    public void handlerJsonArray_should_failed_the_future_when_failing_the_handler() {
        Future<JsonArray> future = Future.future();
        Handler<Either<String, JsonArray>> handler = handlerJsonArray(future);
        handler.handle(new Either.Left<>(error));
        assertTrue(future.failed() && future.cause() != null && error.equals(future.cause().getMessage()));
    }


    @Test
    @DisplayName("FutureHelper.handlerJsonObject should succeed the future when succeeding the handler")
    public void handlerJsonObject_should_succeed_the_future_when_succeeding_the_handler() {
        Future<JsonObject> future = Future.future();
        Handler<Either<String, JsonObject>> handler = FutureHelper.handlerJsonObject(future);
        handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
        assertTrue(future.succeeded() && future.result() != null && "ok".equals(future.result().getString("status")));
    }

    @Test
    @DisplayName("FutureHelper.handlerJsonObject should failed the future when failing the handler")
    public void handlerJsonObject_should_failed_the_future_when_failing_the_handler() {
        Future<JsonObject> future = Future.future();
        Handler<Either<String, JsonObject>> handler = FutureHelper.handlerJsonObject(future);
        handler.handle(new Either.Left<>(error));
        assertTrue(future.failed() && future.cause() != null && error.equals(future.cause().getMessage()));
    }

}
