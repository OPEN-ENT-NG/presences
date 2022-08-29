package fr.openent.massmailing.service;

import fr.openent.presences.enums.InitTypeEnum;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.request.JsonHttpServerRequest;

public interface InitService {
    void getTemplateStatement(JsonHttpServerRequest request, String structure, String owner, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler);
}
