package fr.openent.presences.controller.events;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.export.EventsCSVExport;
import fr.openent.presences.model.Event.Event;
import fr.openent.presences.security.ActionRight;
import fr.openent.presences.security.CreateEventRight;
import fr.openent.presences.security.Event.EventReadRight;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.impl.DefaultEventService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
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
import org.entcore.common.http.filter.Trace;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class EventController extends ControllerHelper {

    private final EventService eventService;

    public EventController(EventBus eb) {
        super();
        this.eventService = new DefaultEventService(eb);
    }

    @Get("/events")
    @ApiDoc("get events")
    @SecuredAction(Presences.READ_EVENT)
    @SuppressWarnings("unchecked")
    public void getEvents(HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        String startDate = request.getParam("startDate");
        String endDate = request.getParam("endDate");
        String startTime = request.getParam("startTime");
        String endTime = request.getParam("endTime");

        List<String> eventType = request.getParam("eventType") != null ? Arrays.asList(request.getParam("eventType").split("\\s*,\\s*")) : null;
        List<String> reasonIds = request.getParam("reasonIds") != null ? Arrays.asList(request.getParam("reasonIds").split("\\s*,\\s*")) : new ArrayList<>();
        Boolean noReason = request.params().contains("noReason") && Boolean.parseBoolean(request.getParam("noReason"));
        List<String> userIds = request.getParam("userId") != null ? Arrays.asList(request.getParam("userId").split("\\s*,\\s*")) : new ArrayList<>();
        List<String> classes = request.getParam("classes") != null ? Arrays.asList(request.getParam("classes").split("\\s*,\\s*")) : null;
        Boolean regularized = request.params().contains("regularized") ? Boolean.parseBoolean(request.getParam("regularized")) : null;
        Boolean followed = request.params().contains("followed") ? Boolean.parseBoolean(request.getParam("followed")) : null;
        Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;

        if (!request.params().contains("structureId") || !request.params().contains("startDate") ||
                !request.params().contains("endDate")) {
            badRequest(request);
            return;
        }

        getUserIdFromClasses(classes, event -> {
            if (event.isLeft()) {
                renderError(request, JsonObject.mapFrom(event.left().getValue()));
                return;
            }
            JsonArray userFromClasses = event.right().getValue();
            if (userFromClasses != null && !userFromClasses.isEmpty()) {
                List<String> studentIds = ((List<JsonObject>) userFromClasses.getList()).stream()
                        .map(user -> user.getString("studentId"))
                        .collect(Collectors.toList());
                userIds.addAll(studentIds);
            }


            Future<JsonArray> eventsFuture = Future.future();
            Future<JsonObject> pageNumberFuture = Future.future();

            CompositeFuture.all(eventsFuture, pageNumberFuture).setHandler(resultFuture -> {

                if (resultFuture.failed()) {
                    renderError(request, JsonObject.mapFrom(resultFuture.cause()));
                } else {
                    // set 0 if count.equal Presences.PAGE_SIZE (20 === 20) else set > 0
                    Integer pageCount = pageNumberFuture.result().getInteger("count", 0).equals(Presences.PAGE_SIZE) ? 0
                            : pageNumberFuture.result().getInteger("count", 0) / Presences.PAGE_SIZE;
                    JsonObject res = new JsonObject()
                            .put("page", page)
                            .put("page_count", pageCount)
                            .put("all", eventsFuture.result());

                    renderJson(request, res);

                }
            });

            eventService.get(structureId, startDate, endDate, startTime, endTime, eventType,
                    reasonIds, noReason, userIds, regularized, followed, page, eventsFuture);
            eventService.getPageNumber(structureId, startDate, endDate, startTime, endTime, eventType, reasonIds, noReason, userIds,
                    regularized, followed, FutureHelper.handlerJsonObject(pageNumberFuture));

        });
    }

    @Get("/events/export")
    @ApiDoc("Export events")
    @ResourceFilter(EventReadRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void exportEvents(HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        String startDate = request.getParam("startDate");
        String endDate = request.getParam("endDate");

        List<String> eventType = request.getParam("eventType") != null ? Arrays.asList(request.getParam("eventType").split("\\s*,\\s*")) : null;
        List<String> reasonIds = request.getParam("reasonIds") != null ? Arrays.asList(request.getParam("reasonIds").split("\\s*,\\s*")) : null;
        Boolean noReason = request.params().contains("noReason") ? Boolean.parseBoolean(request.getParam("noReason")) : false;
        List<String> userId = request.getParam("userId") != null ? Arrays.asList(request.getParam("userId").split("\\s*,\\s*")) : null;
        List<String> classes = request.getParam("classes") != null ? Arrays.asList(request.getParam("classes").split("\\s*,\\s*")) : null;
        Boolean regularized = request.params().contains("regularized") ? Boolean.parseBoolean(request.getParam("regularized")) : null;
        Boolean followed = request.params().contains("followed") ? Boolean.parseBoolean(request.getParam("followed")) : null;
        if (!request.params().contains("structureId") || !request.params().contains("startDate") ||
                !request.params().contains("endDate")) {
            badRequest(request);
            return;
        }

        getUserIdFromClasses(classes, userResponse -> {
            if (userResponse.isLeft()) {
                renderError(request, JsonObject.mapFrom(userResponse.left().getValue()));
            } else {
                JsonArray userIdFromClasses = userResponse.right().getValue();
                eventService.getCsvData(structureId, startDate, endDate, eventType, reasonIds, noReason, userId, userIdFromClasses,
                        classes, regularized, followed, event -> {
                            if (event.failed()) {
                                log.error("[Presences@EventController::exportEvents] Something went wrong while getting CSV data",
                                        event.cause().getMessage());
                                renderError(request);
                            } else {
                                List<Event> events = event.result();

                                List<String> csvHeaders = Arrays.asList(
                                        "presences.csv.header.student.lastName",
                                        "presences.csv.header.student.firstName",
                                        "presences.exemptions.csv.header.audiance",
                                        "presences.event.type",
                                        "presences.absence.reason",
                                        "presences.created.by",
                                        "presences.exemptions.dates",
                                        "presences.hour",
                                        "presences.exemptions.csv.header.comment",
                                        "presences.widgets.absences.regularized",
                                        "presences.id");
//                                String body = I18n.getInstance().translate("presences.register.notify.body", Renders.getHost(request), I18n.acceptLanguage(request), subjectName, groups, startHour, endHour, structureName);
//                                Renders.getHost(this.request), I18n.acceptLanguage(this.request)) + SEPARATOR;
                                EventsCSVExport ece = new EventsCSVExport(events, Renders.getHost(request), I18n.acceptLanguage(request));
                                ece.setHeader(csvHeaders);
                                ece.export();
                            }
                        });
            }
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

    @Post("/events")
    @ApiDoc("Create event")
    @SecuredAction(Presences.CREATE_EVENT)
    @Trace(Actions.EVENT_CREATION)
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
    @Trace(Actions.EVENT_SET_REASON)
    public void changeReasonEvents(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, event -> {
            UserUtils.getUserInfos(eb, request, user -> {
                eventService.changeReasonEvents(event, user, DefaultResponseHandler.defaultResponseHandler(request));
            });
        });
    }

    @Put("/events/regularized")
    @ApiDoc("Update regularized absent in event")
    @ResourceFilter(CreateEventRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.ABSENCE_REGULARIZATION)
    public void regularizedEvents(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, event -> {
            UserUtils.getUserInfos(eb, request, user -> {
                eventService.changeRegularizedEvents(event, user, DefaultResponseHandler.defaultResponseHandler(request));
            });
        });
    }

    @Put("/events/:id")
    @ApiDoc("Update given event")
    @ResourceFilter(CreateEventRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.EVENT_UPDATE)
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
    @Trace(Actions.EVENT_DELETION)
    public void deleteEvent(HttpServerRequest request) {
        try {
            Integer eventId = Integer.parseInt(request.getParam("id"));
            eventService.delete(eventId, defaultResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("[Presences@EventController] Failed to cast event identifier");
            badRequest(request);
        }
    }

    @Get("/events/:id/actions")
    @ApiDoc("Get given structure")
    @ResourceFilter(ActionRight.class)
    @SecuredAction(Presences.CREATE_ACTION)
    public void get(final HttpServerRequest request) {
        String eventId = request.getParam("id");
        eventService.getActions(eventId, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/events/actions")
    @ApiDoc("Create event action")
    @ResourceFilter(ActionRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.ABSENCE_ACTION_CREATION)
    public void postAction(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, actionBody -> {
            if (isActionBodyInvalid(actionBody)) {
                badRequest(request);
                return;
            }
            eventService.createAction(actionBody, either -> {
                if (either.isLeft()) {
                    log.error("[Presences@EventController::postAction] failed to create action", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    private boolean isActionBodyInvalid(JsonObject actionBody) {
        return !actionBody.containsKey("event_id") &&
                !actionBody.containsKey("action_id") &&
                !actionBody.containsKey("owner") &&
                !actionBody.containsKey("comment");
    }

    @Get("/events/absences/summary")
    @ApiDoc("Get absences counts summary")
    @ResourceFilter(EventReadRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getAbsentsCounts(final HttpServerRequest request) {

        String structureId = request.getParam("structureId");
        String currentDate = request.getParam("currentDate");

        eventService.getAbsencesCountSummary(structureId, currentDate, summary -> {
            if (summary.isLeft()) {
                log.error("[Presences@EventController::getAbsentsCounts] failed to fetch absences count summary",
                        summary.left().getValue());
                renderError(request);
            } else {
                renderJson(request, summary.right().getValue());
            }
        });
    }
}