package fr.openent.massmailing.controller;

import fr.openent.massmailing.service.InitService;
import fr.openent.massmailing.service.impl.DefaultInitService;
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
        String action = body.getString("action");
        switch (action) {
            case "init-get-templates-statement":
                JsonHttpServerRequest request = new JsonHttpServerRequest(body.getJsonObject("request", new JsonObject()));
                String structure = body.getString("structure");
                String owner = body.getString("owner");
                initService.getTemplateStatement(request, structure, owner, BusResponseHandler.busResponseHandler(message));
                break;
            default:
                message.reply(new JsonObject()
                        .put("status", "error")
                        .put("message", "Invalid action."));
        }
    }
}
