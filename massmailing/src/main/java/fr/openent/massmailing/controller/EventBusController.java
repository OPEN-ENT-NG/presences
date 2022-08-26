package fr.openent.massmailing.controller;

import fr.openent.massmailing.service.InitService;
import fr.openent.massmailing.service.impl.DefaultInitService;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.InitTypeEnum;
import fr.wseduc.bus.BusAddress;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.entcore.common.bus.BusResponseHandler;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.request.JsonHttpServerRequest;

public class EventBusController extends ControllerHelper {

    private InitService initService = new DefaultInitService();

    @BusAddress("fr.openent.massmailing")
    public void bus(final Message<JsonObject> message) {
        JsonObject body = message.body();
        String action = body.getString(Field.ACTION);
        switch (action) {
            case "init-get-templates-statement":
                JsonHttpServerRequest request = new JsonHttpServerRequest(body.getJsonObject(Field.REQUEST, new JsonObject()));
                String structure = body.getString(Field.STRUCTURE);
                String owner = body.getString(Field.OWNER);
                InitTypeEnum initTypeEnum = InitTypeEnum.getInitType(body.getInteger(Field.INITTYPE));
                initService.getTemplateStatement(request, structure, owner, initTypeEnum, BusResponseHandler.busResponseHandler(message));
                break;
            default:
                message.reply(new JsonObject()
                        .put(Field.STATUS, Field.ERROR)
                        .put(Field.MESSAGE, "Invalid action."));
        }
    }
}
