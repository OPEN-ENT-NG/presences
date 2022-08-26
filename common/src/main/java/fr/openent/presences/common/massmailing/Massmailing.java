package fr.openent.presences.common.massmailing;

import fr.openent.presences.common.helper.RequestHelper;
import fr.openent.presences.common.message.MessageResponseHandler;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.InitTypeEnum;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Massmailing {
    private static final Logger LOGGER = LoggerFactory.getLogger(Massmailing.class);

    private String address = "fr.openent.massmailing";
    private EventBus eb;

    private Massmailing() {
    }

    public static Massmailing getInstance() {
        return ViescolaireHolder.instance;
    }

    public void init(EventBus eb) {
        this.eb = eb;
    }

    public void getInitTemplatesStatement(HttpServerRequest request, String structure, String owner, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put(Field.ACTION, "init-get-templates-statement")
                .put(Field.STRUCTURE, structure)
                .put(Field.OWNER, owner)
                .put(Field.INITTYPE, initTypeEnum.getValue())
                .put(Field.REQUEST, RequestHelper.getJsonRequest(request));

        eb.send(address, action, MessageResponseHandler.messageJsonObjectHandler(handler));
    }

    private static class ViescolaireHolder {
        private static final Massmailing instance = new Massmailing();

        private ViescolaireHolder() {
        }
    }
}
