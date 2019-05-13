package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserUtils;

public class PresencesController extends ControllerHelper {

    private EventBus eb;

    public PresencesController(EventBus eb) {
        this.eb = eb;
    }

    @Get("")
    @ApiDoc("Render view")
    @SecuredAction("view")
    public void view(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            JsonObject action = new JsonObject()
                    .put("action", "user.getActivesStructure")
                    .put("module", Presences.dbSchema)
                    .put("structures", new JsonArray(user.getStructures()));
            eb.send(Presences.ebViescoAddress, action, event -> {
                JsonObject body = (JsonObject) event.result().body();
                if (event.failed() || "error".equals(body.getString("status"))) {
                    log.error("[Presences@PresencesController] Failed to retrieve actives structures");
                    renderError(request);
                } else {
//                    StringBuilder structures = new StringBuilder();
//                    JsonArray results = body.getJsonArray("results");
//                    for (int i = 0; i < results.size(); i++) {
//                        structures.append(results.getJsonObject(i).getString("id_etablissement")).append(",");
//                    }
//                    structures = new StringBuilder(structures.substring(0, structures.length() - 1));
//                    renderView(request, new JsonObject().put("structures", structures.toString()));
                    renderView(request, new JsonObject().put("structures", body.getJsonArray("results")));
                }
            });
        });
    }
}
