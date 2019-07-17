package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.security.CreateEventRight;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.impl.DefaultEventService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.UserUtils;

import java.util.Arrays;
import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class EventController extends ControllerHelper {

    private EventService eventService;

    public EventController(EventBus eb) {
        super();
        this.eventService = new DefaultEventService(eb);
    }

    @Get("/events")
    @ApiDoc("get events")
    public void getEvents(HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        String startDate = request.getParam("startDate");
        String endDate = request.getParam("endDate");

        List<String> eventType = request.getParam("eventType") != null ? Arrays.asList(request.getParam("eventType").split("\\s*,\\s*")) : null;
        List<String> userId = request.getParam("userId") != null ? Arrays.asList(request.getParam("userId").split("\\s*,\\s*")) : null;
        List<String> classes = request.getParam("classes") != null ? Arrays.asList(request.getParam("classes").split("\\s*,\\s*")) : null;

        boolean unjustified = request.params().contains("unjustified") && Boolean.parseBoolean(request.getParam("unjustified"));
        boolean regularized = request.params().contains("regularized") && Boolean.parseBoolean(request.getParam("regularized"));

        Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;

        if (!request.params().contains("structureId") || !request.params().contains("startDate") ||
                !request.params().contains("endDate") || !request.params().contains("page")) {
            badRequest(request);
            return;
        }

        getUserIdFromClasses(classes, event -> {
            if (event.isLeft()) {
                renderError(request, JsonObject.mapFrom(event.left().getValue()));
                return;
            }
            JsonArray userIdFromClasses = event.right().getValue();
            Future<JsonArray> eventsFuture = Future.future();
            Future<JsonObject> pageNumberFuture = Future.future();

            CompositeFuture.all(eventsFuture, pageNumberFuture).setHandler(resultFuture -> {
                if (resultFuture.failed()) {
                    renderError(request, JsonObject.mapFrom(resultFuture.cause()));
                } else {
                    JsonObject res = new JsonObject()
                            .put("page", page)
                            .put("page_count", pageNumberFuture.result().getLong("count")
                                    != null ? pageNumberFuture.result().getLong("count") / Presences.PAGE_SIZE : 0)
                            .put("all", eventsFuture.result());

                    renderJson(request, res);
                }
            });
            eventService.get(structureId, startDate, endDate, eventType, userId, userIdFromClasses,
                    classes, unjustified, regularized, page, FutureHelper.handlerJsonArray(eventsFuture));
            eventService.getPageNumber(structureId, startDate, endDate, eventType, userId,
                    unjustified, regularized, userIdFromClasses, FutureHelper.handlerJsonObject(pageNumberFuture));
        });
    }

    private void getUserIdFromClasses(List<String> classes, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (c:FunctionalGroup)<-[:IN]-(s:User {profiles:['Student']}) " +
                "WHERE c.id IN {classesId} return s.id as studentId" +
                " UNION " +
                "MATCH (c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(s:User {profiles:['Student']}) " +
                "WHERE c.id IN {classesId} return s.id as studentId";

        JsonObject params = new JsonObject().put("classesId", classes);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    @Get("/event/reason/types")
    @ApiDoc("Retrieve event reason type")
    public void getEventsReason(HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        if (!request.params().contains("structureId")) {
            badRequest(request);
            return;
        }
        eventService.getEventsReasonType(structureId, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/events")
    @ApiDoc("Create event")
    @SecuredAction(Presences.CREATE_EVENT)
    public void postEvent(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "event", event -> {
            if (!isValidBody(event)) {
                badRequest(request);
                return;
            }
            UserUtils.getUserInfos(eb, request, user -> {
                eventService.create(event, user, either -> {
                    if (either.isLeft()) {
                        log.error("[Presences@EventController] failed to create event", either.left().getValue());
                        renderError(request);
                    } else {
                        renderJson(request, either.right().getValue(), 201);
                    }
                });
            });
        });
    }

    @Put("/events/reason")
    @ApiDoc("Update reason in event")
    @ResourceFilter(CreateEventRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void changeReasonEvents(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, event -> {
            eventService.changeReasonEvents(event, DefaultResponseHandler.defaultResponseHandler(request));
        });
    }

    @Put("/events/:id")
    @ApiDoc("Update given event")
    @ResourceFilter(CreateEventRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void putEvent(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "event", event -> {
            if (!isValidBody(event)
                    && !EventType.LATENESS.getType().equals(event.getInteger("type_id"))
                    && !EventType.DEPARTURE.getType().equals(event.getInteger("type_id"))) {
                badRequest(request);
                return;
            }

            try {
                Integer eventId = Integer.parseInt(request.getParam("id"));
                eventService.update(eventId, event, defaultResponseHandler(request));
            } catch (ClassCastException e) {
                log.error("[Presences@EventController] Failed to cast event identifier");
                badRequest(request);
            }
        });
    }

    private Boolean isValidBody(JsonObject event) {
        boolean valid = event.containsKey("student_id") && event.containsKey("type_id") && event.containsKey("register_id");
        Integer type = event.getInteger("type_id");
        if (!EventType.ABSENCE.getType().equals(type)) {
            valid = valid && event.containsKey("start_date") && event.containsKey("end_date");
        }
        return valid;
    }

    @Delete("/events/:id")
    @ApiDoc("Delete given event")
    @ResourceFilter(CreateEventRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void deleteEvent(HttpServerRequest request) {
        try {
            Integer eventId = Integer.parseInt(request.getParam("id"));
            eventService.delete(eventId, defaultResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("[Presences@EventController] Failed to cast event identifier");
            badRequest(request);
        }
    }
}
