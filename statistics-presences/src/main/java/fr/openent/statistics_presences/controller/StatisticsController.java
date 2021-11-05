package fr.openent.statistics_presences.controller;

import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.controller.security.UserInStructure;
import fr.openent.statistics_presences.filter.Filter;
import fr.openent.statistics_presences.indicator.Indicator;
import fr.openent.statistics_presences.indicator.export.Global;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import fr.openent.statistics_presences.service.StatisticsPresencesService;
import fr.wseduc.rs.ApiDoc;
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
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class StatisticsController extends ControllerHelper {
    private final StatisticsPresencesService statisticsPresencesService;

    public StatisticsController(CommonServiceFactory serviceFactory) {
        this.statisticsPresencesService = serviceFactory.statisticsPresencesService();
    }

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
        return new JsonArray(new ArrayList<>(StatisticsPresences.indicatorMap.keySet().stream().sorted().collect(Collectors.toList())));
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

    @Post("/structures/:structure/indicators/:indicator/graph")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(UserInStructure.class)
    public void fetchGraph(HttpServerRequest request) {
        String indicatorName = request.getParam("indicator");
        if (!StatisticsPresences.indicatorMap.containsKey(indicatorName)) {
            notFound(request);
            return;
        }
        RequestUtils.bodyToJson(request, pathPrefix + "indicator", body -> {
            try {
                Filter filter = new Filter(request.getParam("structure"), body);
                Indicator indicator = StatisticsPresences.indicatorMap.get(indicatorName);
                indicator.searchGraph(filter, Indicator.handler(request));
            } catch (NumberFormatException e) {
                badRequest(request);
            }
        });
    }

    @Get("/structures/:structure/indicators/:indicator/export")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(UserInStructure.class)
    @SuppressWarnings("unchecked")
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
                indicator.export(request, filter,
                        searchResult.getJsonArray("data").getList(),
                        searchResult.getJsonObject("count"),
                        searchResult.getJsonObject("slots"),
                        searchResult.getJsonObject("rate"));
            } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                log.error(String.format("Failed to generate export for indicator %s", indicator.getClass().getSimpleName()), e);
            }
        });

    }

    @Post("/process/statistics/tasks")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdminFilter.class)
    @ApiDoc("Generate notebook archives")
    @SuppressWarnings("unchecked")
    public void processStatisticsPrefetch(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "processStatisticsPrefetch", body -> {
            List<String> structure = body.getJsonArray("structure").getList();
            statisticsPresencesService.processStatisticsPrefetch(structure)
                    .onSuccess(res -> renderJson(request, res))
                    .onFailure(unused -> renderError(request));
        });
    }
}
