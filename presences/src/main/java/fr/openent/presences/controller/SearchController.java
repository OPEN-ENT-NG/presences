package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.common.security.SearchRight;
import fr.openent.presences.service.SearchService;
import fr.openent.presences.service.impl.DefaultSearchService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class SearchController extends ControllerHelper {

    private EventBus eb;
    private SearchService searchService;

    public SearchController(EventBus eb) {
        super();
        this.eb = eb;
        this.searchService = new DefaultSearchService();
    }

    @Get("/search/users")
    @ApiDoc("Search for users")
    @SecuredAction(Presences.SEARCH)
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

            callViescolaireEventBus(action, request);
        } else {
            badRequest(request);
        }
    }

    @Get("/search/groups")
    @ApiDoc("Search for groups")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SearchRight.class)
    public void searchGroups(HttpServerRequest request) {
        if (request.params().contains("q") && !"".equals(request.params().get("q").trim())
                && request.params().contains("field")
                && request.params().contains("structureId")) {
            String query = request.getParam("q");
            List<String> fields = request.params().getAll("field");
            String structure_id = request.getParam("structureId");

            JsonObject action = new JsonObject()
                    .put("action", "groupe.search")
                    .put("q", query)
                    .put("fields", new JsonArray(fields))
                    .put("structureId", structure_id);

            callViescolaireEventBus(action, request);
        } else {
            badRequest(request);
        }
    }

    @Get("/search")
    @ApiDoc("Search for a student or a group")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SearchRight.class)
    public void search(HttpServerRequest request) {
        if (request.params().contains("q") && !"".equals(request.params().get("q").trim())
                && request.params().contains("structureId")) {
            searchService.search(request.getParam("q"), request.getParam("structureId"), arrayResponseHandler(request));
        }
    }


    private void callViescolaireEventBus(JsonObject action, HttpServerRequest request) {
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
