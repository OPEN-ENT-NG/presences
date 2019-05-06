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

import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class SearchController extends ControllerHelper {

    EventBus eb;

    public SearchController(EventBus eb) {
        super();
        this.eb = eb;
    }

    @Get("/search/users")
    @ApiDoc("Search for users")
    @SecuredAction(Presences.SEARCH_REGISTER)
    public void searchUsers(HttpServerRequest request) {
        if (request.params().contains("q") && !"".equals(request.params().get("q").trim())
                && request.params().contains("field")
                && request.params().contains("profile")
                && request.params().contains("structureId")) {
            String query = request.getParam("q");
            List<String> fields = request.params().getAll("field");
            String profile = request.getParam("profile");
            String structure_id = request.getParam("structureId");

            JsonObject action = new JsonObject()
                    .put("action", "user.search")
                    .put("q", query)
                    .put("fields", new JsonArray(fields))
                    .put("profile", profile)
                    .put("structureId", structure_id);

            eb.send("viescolaire", action, handlerToAsyncHandler(event -> {
                JsonObject body = event.body();
                if (!"ok".equals(body.getString("status"))) {
                    log.error("[Presences@SearchController] Failed to search for user");
                    renderError(request);
                    return;
                }

                renderJson(request, body.getJsonArray("results"));
            }));
        }
    }
}
