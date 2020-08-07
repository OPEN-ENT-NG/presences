package fr.openent.statistics_presences.controller;

import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.controller.security.UserInStructure;
import fr.openent.statistics_presences.filter.Filter;
import fr.openent.statistics_presences.indicator.Indicator;
import fr.openent.statistics_presences.indicator.export.Global;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;

public class StatisticsController extends ControllerHelper {

    @Get("")
    @SecuredAction(StatisticsPresences.VIEW)
    public void view(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            JsonObject action = new JsonObject()
                    .put("action", "user.getActivesStructure")
                    .put("module", "presences")
                    .put("structures", new JsonArray(user.getStructures()));
            eb.send("viescolaire", action, event -> {
                JsonObject body = (JsonObject) event.result().body();
                if (event.failed() || "error".equals(body.getString("status"))) {
                    log.error("[Presences@PresencesController] Failed to retrieve actives structures");
                    renderError(request);
                } else {
                    JsonObject params = new JsonObject().put("structures", body.getJsonArray("results", new JsonArray()));
                    params.put("indicators", indicatorList());
                    renderView(request, params);
                }
            });
        });
    }

    private JsonArray indicatorList() {
        return new JsonArray(StatisticsPresences.indicatorMap.keySet().stream().collect(Collectors.toList()));
    }

    @Post("/structures/:structure/indicators/:indicator")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(UserInStructure.class)
    public void fetch(HttpServerRequest request) {
        String indicatorName = request.getParam("indicator");
        if (!StatisticsPresences.indicatorMap.containsKey(indicatorName)) {
            notFound(request);
            return;
        }

        RequestUtils.bodyToJson(request, pathPrefix + "indicator", body -> {
            try {
                Integer page = request.params().contains("page") ? Integer.parseInt(request.getParam("page")) : null;
                Filter filter = new Filter(request.getParam("structure"), body)
                        .setPage(page);
                Indicator indicator = StatisticsPresences.indicatorMap.get(indicatorName);
                indicator.search(filter, Indicator.handler(request));
            } catch (NumberFormatException e) {
                badRequest(request);
            }
        });
    }

    @Get("/structures/:structure/indicators/:indicator/export")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(UserInStructure.class)
    public void export(HttpServerRequest request) {
        Filter filter = new Filter(request);
        String indicatorName = request.getParam("indicator");
        Indicator indicator = StatisticsPresences.indicatorMap.get(indicatorName);
        indicator.search(filter, ar -> {
            if (ar.failed()) {
                log.error(String.format("Search failed for indicator %s in csv export", Global.class.getSimpleName()), ar.cause());
                Renders.renderError(request);
                return;
            }

            try {
                JsonObject searchResult = ar.result();
                indicator.export(request, filter, searchResult.getJsonArray("data").getList());
            } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                log.error(String.format("Failed to generate exprot for indicator %s", indicator.getClass().getSimpleName()), e);
            }
        });

    }

}
