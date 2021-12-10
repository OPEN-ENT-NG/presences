package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.common.security.UserInStructure;
import fr.openent.presences.common.statistics_presences.StatisticsPresences;
import fr.openent.presences.core.constants.Field;
import fr.wseduc.rs.Post;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserUtils;

public class StatisticsController extends ControllerHelper {

    @Post("/statistics/structures/:structure/student/:student/graph")
    @SecuredAction(Presences.STATISTICS_ACCESS_DATA)
    public void getStatisticsGraph(HttpServerRequest request) {
        request.pause();
        UserUtils.getUserInfos(eb, request, user ->
                new UserInStructure().authorize(request, null, user, isAuthorized -> {
                    if (Boolean.TRUE.equals(isAuthorized)) {
                        String structure = request.getParam(Field.STRUCTURE);
                        String student = request.getParam(Field.STUDENT);
                        String indicator = Field.MONTHLY;
                        checkStatisticsIndicator(request, indicator)
                                .compose(isStatisticsIndicator -> {
                                    request.resume();
                                    return bodyToJson(request);
                                })
                                .compose(jsonBody -> {
                                    jsonBody.put("users", new JsonArray().add(student));
                                    return getStatisticsGraph(jsonBody, structure, indicator);
                                })
                                .onSuccess(result -> renderJson(request, result))
                                .onFailure(err -> badRequest(request, err.getMessage()));
                    } else {
                        unauthorized(request);
                    }
                })
        );
    }

    @Post("/statistics/structures/:structure/student/:student")
    @SecuredAction(Presences.STATISTICS_ACCESS_DATA)
    public void getStatistics(HttpServerRequest request) {
        request.pause();
        UserUtils.getUserInfos(eb, request, user ->
                new UserInStructure().authorize(request, null, user, isAuthorized -> {
                    if (Boolean.TRUE.equals(isAuthorized)) {
                        String structure = request.getParam(Field.STRUCTURE);
                        String student = request.getParam(Field.STUDENT);
                        String indicator = Field.GLOBAL;
                        checkStatisticsIndicator(request, indicator)
                                .compose(isStatisticsIndicator -> {
                                    request.resume();
                                    return bodyToJson(request);
                                })
                                .compose(jsonBody -> {
                                    jsonBody.put("users", new JsonArray().add(student));
                                    return getStatistics(jsonBody, structure, indicator);
                                })
                                .onSuccess(result -> renderJson(request, result))
                                .onFailure(err -> badRequest(request, err.getMessage()));
                    } else {
                        unauthorized(request);
                    }
                })
        );
    }

    /**
     * Get statistics graph data
     *
     * @param body       JsonObject containing all filter information for statistics
     * @param structure  Structure string id
     * @param indicator  Desired indicator name
     * @return Future of {@link JsonObject}, a json containing the returned data
     */
    private Future<JsonObject> getStatisticsGraph(JsonObject body, String structure, String indicator) {
        Promise<JsonObject> promise = Promise.promise();
        StatisticsPresences.getInstance().getStatisticsGraph(body, structure, indicator, result -> {
            if (result.succeeded()) {
                promise.complete(result.result());
            } else {
                promise.fail(result.cause().getMessage());
            }
        });

        return promise.future();
    }

    /**
     * Get statistics data
     *
     * @param body       JsonObject containing all filter information for statistics
     * @param structure  Structure string id
     * @param indicator  Desired indicator name
     * @return Future of {@link JsonObject}, a json containing the returned data
     */
    private Future<JsonObject> getStatistics(JsonObject body, String structure, String indicator) {
        Promise<JsonObject> promise = Promise.promise();
        StatisticsPresences.getInstance().getStatistics(body, structure, indicator, 0, result -> {
            if (result.succeeded()) {
                promise.complete(result.result());
            } else {
                promise.fail(result.cause().getMessage());
            }
        });

        return promise.future();
    }

    /**
     * Convert the body of {@link HttpServerRequest} to {@link JsonObject}
     *
     * @param request     The specified request
     * @return Future of {@link JsonObject}, a json containing the returned data
     */
    private Future<JsonObject> bodyToJson(HttpServerRequest request) {
        Promise<JsonObject> promise = Promise.promise();
        RequestUtils.bodyToJson(request, pathPrefix + Field.INDICATOR, promise::complete);

        return promise.future();
    }

    /**
     * Check if the indicator exists. Return a bad request otherwise
     *
     * @param request     The request for which we are going to return a bad request
     * @param indicator   Desired indicator name
     *
     * @return Future of {@link Void}
     */
    private Future<Void> checkStatisticsIndicator(HttpServerRequest request, String indicator) {
        Promise<Void> promise = Promise.promise();

        StatisticsPresences.getInstance().getStatisticsIndicator(event -> {
            if (event.succeeded()) {
                if (Boolean.TRUE.equals(event.result().getJsonArray("indicator").contains(indicator))) {
                    promise.complete();
                } else {
                    badRequest(request, "indicator.not.found");
                }
            } else {
                promise.fail(event.cause().getMessage());
            }
        });

        return promise.future();
    }
}
