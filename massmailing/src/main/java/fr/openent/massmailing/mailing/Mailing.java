package fr.openent.massmailing.mailing;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface Mailing {
    void process(Handler<Either<String, List<JsonObject>>> handler);

    abstract void massmail(Handler<Either<String, Boolean>> handler);
}
