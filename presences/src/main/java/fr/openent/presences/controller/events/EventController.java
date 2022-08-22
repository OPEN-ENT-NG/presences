package fr.openent.presences.controller.events;

import fr.openent.presences.Presences;
import fr.openent.presences.common.export.*;
import fr.openent.presences.common.helper.*;
import fr.openent.presences.common.service.*;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.*;
import fr.openent.presences.security.ActionRight;
import fr.openent.presences.security.CreateEventRight;
import fr.openent.presences.security.Event.EventReadRight;
import fr.openent.presences.service.CommonPresencesServiceFactory;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.ExportEventService;
import fr.openent.presences.worker.*;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.*;
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
    private final ExportEventService exportEventService;
    private final GroupService groupService;
    private final ExportData exportData;
    private static final int MAX_EXPORTED_EVENTS = 1000;

    public EventController(CommonPresencesServiceFactory commonPresencesServiceFactory) {
        super();
        this.eventService = commonPresencesServiceFactory.eventService();
        this.exportEventService = commonPresencesServiceFactory.exportEventService();
        this.groupService = commonPresencesServiceFactory.groupService();
        this.exportData = commonPresencesServiceFactory.exportData();
    }

    @Get("/events")
    @ApiDoc("get events")
    @ResourceFilter(EventReadRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @SuppressWarnings("unchecked")
    public void getListEvents(HttpServerRequest request) {

        if (!request.params().contains(Field.STRUCTUREID) || !request.params().contains(Field.STARTDATE) ||
                !request.params().contains(Field.ENDDATE)) {
            badRequest(request);
            return;
        }
        String structureId = request.getParam(Field.STRUCTUREID);
        String startDate = request.getParam(Field.STARTDATE);
        String endDate = request.getParam(Field.ENDDATE);
        String startTime = request.getParam(Field.START_TIME);
        String endTime = request.getParam(Field.END_TIME);
        List<String> eventType = request.getParam(Field.EVENTTYPE) != null ?
                Arrays.asList(request.getParam(Field.EVENTTYPE).split("\\s*,\\s*")) : null;
        List<String> reasonIds = request.getParam(Field.REASONIDS) != null ?
                Arrays.asList(request.getParam(Field.REASONIDS).split("\\s*,\\s*")) : new ArrayList<>();
        Boolean noAbsenceReason = request.params().contains(Field.NOREASON)
                && Boolean.parseBoolean(request.getParam(Field.NOREASON));
        Boolean noLatenessReason = request.params().contains(Field.NOREASONLATENESS)
                && Boolean.parseBoolean(request.getParam(Field.NOREASONLATENESS));
        List<String> userIds = request.getParam(Field.USERID) != null ?
                new ArrayList<>(Arrays.asList(request.getParam(Field.USERID).split("\\s*,\\s*"))) : new ArrayList<>();

        Boolean regularized = request.params().contains(Field.REGULARIZED) ?
                Boolean.parseBoolean(request.getParam(Field.REGULARIZED)) : null;
        Boolean followed = request.params().contains(Field.FOLLOWED) ? Boolean.parseBoolean(request.getParam(Field.FOLLOWED)) : null;
        Integer page = request.getParam(Field.PAGE) != null ? Integer.parseInt(request.getParam(Field.PAGE)) : 0;

        List<String> classes = request.getParam(Field.CLASSES) != null ?
                Arrays.asList(request.getParam(Field.CLASSES).split("\\s*,\\s*")) : new ArrayList<>();


        UserUtils.getUserInfos(eb, request, userInfos -> {

            String teacherId = (WorkflowHelper.hasRight(userInfos, WorkflowActions.READ_EVENT_RESTRICTED.toString())
                    && "Teacher".equals(userInfos.getType())) ?
                    userInfos.getUserId() : null;


            this.groupService.getGroupsAndClassesFromTeacherId(teacherId, structureId)
                    .onFailure(fail -> renderError(request, JsonObject.mapFrom(fail.getMessage())))
                    .onSuccess(restrictedClasses ->
                            getUserIdFromClasses((classes.isEmpty()) ? restrictedClasses : classes, event -> {
                                if (event.isLeft()) {
                                    renderError(request, JsonObject.mapFrom(event.left().getValue()));
                                    return;
                                }
                                JsonArray userFromClasses = event.right().getValue();
                                if (userFromClasses != null && !userFromClasses.isEmpty() && userIds.isEmpty()) {
                                    List<String> studentIds = ((List<JsonObject>) userFromClasses.getList()).stream()
                                            .map(user -> user.getString(Field.STUDENTID))
                                            .collect(Collectors.toList());
                                    userIds.addAll(studentIds);
                                }

                                Promise<JsonArray> eventsPromise = Promise.promise();
                                Promise<JsonObject> pageNumberPromise = Promise.promise();

                                CompositeFuture.all(eventsPromise.future(), pageNumberPromise.future())
                                        .onFailure(fail -> renderError(request, JsonObject.mapFrom(fail.getMessage())))
                                        .onSuccess(evt -> {
                                            // set 0 if count.equal Presences.PAGE_SIZE (20 === 20) else set > 0
                                            Integer pageCount = pageNumberPromise.future().result().getInteger(Field.COUNT, 0)
                                                    .equals(Presences.PAGE_SIZE) ? 0
                                                    : pageNumberPromise.future().result().getInteger(Field.COUNT, 0) / Presences.PAGE_SIZE;

                                            JsonObject res = new JsonObject()
                                                    .put(Field.PAGE, page)
                                                    .put(Field.PAGE_COUNT, pageCount)
                                                    .put(Field.ALL, eventsPromise.future().result());

                                            renderJson(request, res);
                                        });

                                eventService.get(structureId, startDate, endDate, startTime, endTime, eventType,
                                        reasonIds, noAbsenceReason, noLatenessReason, userIds, restrictedClasses, regularized, followed, page, eventsPromise);
                                eventService.getPageNumber(structureId, startDate, endDate, startTime, endTime, eventType, reasonIds, noAbsenceReason, noLatenessReason, userIds,
                                        regularized, followed, FutureHelper.handlerJsonObject(pageNumberPromise));

                            }));
        });
    }

    @Get("/events/export")
    @ApiDoc("Export events")
    @ResourceFilter(EventReadRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void exportEvents(HttpServerRequest request) {
        if (!request.params().contains(Field.STRUCTUREID) || !request.params().contains(Field.STARTDATE) ||
                !request.params().contains(Field.ENDDATE) || !request.params().contains(Field.TYPE)) {
            badRequest(request);
            return;
        }

        String structureId = request.getParam(Field.STRUCTUREID);
        String startDate = request.getParam(Field.STARTDATE);
        String endDate = request.getParam(Field.ENDDATE);
        String type = request.getParam(Field.TYPE);
        List<String> eventType = request.getParam(Field.EVENTTYPE) != null ? Arrays.asList(request.getParam(Field.EVENTTYPE).split("\\s*,\\s*")) : null;
        List<String> reasonIds = request.getParam(Field.REASONIDS) != null ? Arrays.asList(request.getParam(Field.REASONIDS).split("\\s*,\\s*")) : null;
        Boolean noReason = request.params().contains(Field.NOREASON) && Boolean.parseBoolean(request.getParam(Field.NOREASON));
        Boolean noReasonLateness = request.params().contains(Field.NOREASONLATENESS) && Boolean.parseBoolean(request.getParam(Field.NOREASONLATENESS));
        List<String> userId = request.getParam(Field.USERID) != null ? Arrays.asList(request.getParam(Field.USERID).split("\\s*,\\s*")) : new ArrayList<>();
        List<String> classes = request.getParam(Field.CLASSES) != null ?
                Arrays.asList(request.getParam(Field.CLASSES).split("\\s*,\\s*")) : new ArrayList<>();

        Boolean regularized = request.params().contains(Field.REGULARIZED) ? Boolean.parseBoolean(request.getParam(Field.REGULARIZED)) : null;
        Boolean followed = request.params().contains(Field.FOLLOWED) ? Boolean.parseBoolean(request.getParam(Field.FOLLOWED)) : null;


        UserUtils.getUserInfos(eb, request, userInfos -> {

            String teacherId = (WorkflowHelper.hasRight(userInfos, WorkflowActions.READ_EVENT_RESTRICTED.toString())
                    && "Teacher".equals(userInfos.getType())) ?
                    userInfos.getUserId() : null;
            Boolean canSeeAllStudent = teacherId == null;

            this.groupService.getGroupsAndClassesFromTeacherId(teacherId, structureId)
                    .onFailure(fail -> renderError(request, JsonObject.mapFrom(fail.getMessage())))
                    .onSuccess(restrictedClasses -> {

                        List<String> filterClasses;

                        if (restrictedClasses.isEmpty()) {
                            filterClasses = classes;
                        } else {
                            filterClasses = classes.isEmpty() ? restrictedClasses :
                                    classes.stream().filter(restrictedClasses::contains).collect(Collectors.toList());
                        }

                        getUserIdFromClasses(filterClasses, userResponse -> {
                            if (userResponse.isLeft()) {
                                renderError(request, JsonObject.mapFrom(userResponse.left().getValue()));
                            } else {
                                JsonArray userIdFromClasses = userResponse.right().getValue();

                                String domain = Renders.getHost(request);
                                String locale = I18n.acceptLanguage(request);

                                JsonObject params = new JsonObject()
                                        .put(Field.STRUCTUREID, structureId)
                                        .put(Field.STARTDATE, startDate)
                                        .put(Field.ENDDATE, endDate)
                                        .put(Field.EVENTTYPE, eventType)
                                        .put(Field.REASONIDS, reasonIds)
                                        .put(Field.NOREASON, noReason)
                                        .put(Field.NOREASONLATENESS, noReasonLateness)
                                        .put(Field.USERIDS, userId)
                                        .put(Field.USERIDFROMCLASSES, userIdFromClasses)
                                        .put(Field.CLASSES, filterClasses)
                                        .put(Field.RESTRICTEDCLASSES, restrictedClasses)
                                        .put(Field.REGULARIZED, regularized)
                                        .put(Field.FOLLOWED, followed)
                                        .put(Field.CANSEEALLSTUDENT, canSeeAllStudent)
                                        .put(Field.LOCALE, locale)
                                        .put(Field.DOMAIN, domain)
                                        .put(Field.USER, UserInfosHelper.toJSON(userInfos));

                                exportData.export(PresencesExportWorker.class.getName(),
                                        ExportActions.EXPORT_EVENTS, type, params);

                                eventService.getEventsCount(structureId, startDate, endDate, eventType, reasonIds, noReason, noReasonLateness,
                                        userId, userIdFromClasses, regularized, followed)
                                        .onFailure(fail -> renderError(request, new JsonObject().put(Field.MESSAGE, fail.getMessage())))
                                        .onSuccess(resCount -> {
                                            if (resCount.getLong(Field.COUNT) < MAX_EXPORTED_EVENTS){
                                                if (ExportType.CSV.type().equals(type)) {
                                                    exportEventService.getCsvData(structureId, startDate, endDate, eventType, reasonIds,
                                                            noReason, noReasonLateness, userId, userIdFromClasses,
                                                            classes, restrictedClasses, regularized, followed, event ->
                                                                    exportEventService.processCsvEvent(request, event));
                                                } else if (ExportType.PDF.type().equals(type)) {
                                                    exportEventService.getPdfData(canSeeAllStudent, domain, locale, structureId, startDate, endDate, eventType, reasonIds,
                                                                    noReason, noReasonLateness, userId, userIdFromClasses, regularized, followed)
                                                            .compose(exportEventService::processPdfEvent)
                                                            .onSuccess(res -> request.response()
                                                                    .putHeader("Content-type", "application/pdf; charset=utf-8")
                                                                    .putHeader("Content-Disposition", "attachment; filename=" + res.getName())
                                                                    .end(res.getContent())
                                                            )
                                                            .onFailure(err -> {
                                                                String message = "An error has occurred during export pdf process";
                                                                String logMessage = String.format("[Presences@%s::exportEvents] %s : %s",
                                                                        this.getClass().getSimpleName(), message, err.getMessage());
                                                                log.error(logMessage);
                                                                renderError(request, new JsonObject().put(Field.MESSAGE, message));
                                                            });
                                                } else {
                                                    badRequest(request);
                                                }
                                            } else {
                                                renderJson(request, new JsonObject().put("success", Field.OK));
                                            }
                                        });
                            }
                        });
                    });
        });
    }

    @Get("/events/count")
    @ApiDoc("Get count events")
    @ResourceFilter(EventReadRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void countEvents(HttpServerRequest request) {
        if (!request.params().contains(Field.STRUCTUREID) || !request.params().contains(Field.STARTDATE) ||
                !request.params().contains(Field.ENDDATE)) {
            badRequest(request);
            return;
        }

        String structureId = request.getParam(Field.STRUCTUREID);
        String startDate = request.getParam(Field.STARTDATE);
        String endDate = request.getParam(Field.ENDDATE);
        List<String> eventType = request.getParam(Field.EVENTTYPE) != null ? Arrays.asList(request.getParam(Field.EVENTTYPE).split("\\s*,\\s*")) : null;
        List<String> reasonIds = request.getParam(Field.REASONIDS) != null ? Arrays.asList(request.getParam(Field.REASONIDS).split("\\s*,\\s*")) : null;
        Boolean noReason = request.params().contains(Field.NOREASON) && Boolean.parseBoolean(request.getParam(Field.NOREASON));
        Boolean noReasonLateness = request.params().contains(Field.NOREASONLATENESS) && Boolean.parseBoolean(request.getParam(Field.NOREASONLATENESS));
        List<String> userId = request.getParam(Field.USERID) != null ? Arrays.asList(request.getParam(Field.USERID).split("\\s*,\\s*")) : new ArrayList<>();
        List<String> classes = request.getParam(Field.CLASSES) != null ?
                Arrays.asList(request.getParam(Field.CLASSES).split("\\s*,\\s*")) : new ArrayList<>();

        Boolean regularized = request.params().contains(Field.REGULARIZED) ? Boolean.parseBoolean(request.getParam(Field.REGULARIZED)) : null;
        Boolean followed = request.params().contains(Field.FOLLOWED) ? Boolean.parseBoolean(request.getParam(Field.FOLLOWED)) : null;

        UserUtils.getUserInfos(eb, request, userInfos -> {

            String teacherId = (WorkflowHelper.hasRight(userInfos, WorkflowActions.READ_EVENT_RESTRICTED.toString())
                    && "Teacher".equals(userInfos.getType())) ?
                    userInfos.getUserId() : null;

            this.groupService.getGroupsAndClassesFromTeacherId(teacherId, structureId)
                    .onFailure(fail -> renderError(request, JsonObject.mapFrom(fail.getMessage())))
                    .onSuccess(restrictedClasses -> {

                        List<String> filterClasses;

                        if (restrictedClasses.isEmpty()) {
                            filterClasses = classes;
                        } else {
                            filterClasses = classes.isEmpty() ? restrictedClasses :
                                    classes.stream().filter(restrictedClasses::contains).collect(Collectors.toList());
                        }

                        getUserIdFromClasses(filterClasses, userResponse -> {
                            if (userResponse.isLeft()) {
                                renderError(request, JsonObject.mapFrom(userResponse.left().getValue()));
                            } else {
                                JsonArray userIdFromClasses = userResponse.right().getValue();

                                eventService.getEventsCount(structureId, startDate, endDate, eventType, reasonIds, noReason, noReasonLateness,
                                                userId, userIdFromClasses, regularized, followed)
                                        .onFailure(fail -> {
                                            String message = String.format("[Presences@%s::countEvents] error counting events : %s",
                                                    this.getClass().getSimpleName(), fail.getMessage());
                                            log.error(message);
                                            renderError(request, new JsonObject().put(Field.MESSAGE, fail.getMessage()));
                                        })
                                        .onSuccess(resCount -> renderJson(request, resCount));
                            }
                        });
                    });
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
            if (Boolean.FALSE.equals(isValidBody(event))) {
                badRequest(request);
                return;
            }
            UserUtils.getUserInfos(eb, request, user -> eventService.create(event, user, either -> {
                if (either.isLeft()) {
                    log.error("[Presences@EventController] failed to create event", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue(), 201);
                }
            }));
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
        String startAt = request.getParam(Field.STARTAT) != null ? request.getParam(Field.STARTAT) : request.getParam(Field.CURRENTDATE);
        String endAt = request.getParam(Field.ENDAT) != null ? request.getParam(Field.ENDAT) : request.getParam(Field.CURRENTDATE);

        if (structureId == null || startAt == null || endAt == null) {
            badRequest(request);
            return;
        }

        eventService.getAbsencesCountSummary(structureId, startAt, endAt)
                .onFailure(err -> renderError(request))
                .onSuccess(summary -> renderJson(request, summary));
    }

    @Get("/rights/read/events")
    @SecuredAction(Presences.READ_EVENT)
    public void getEvents(HttpServerRequest request) {
        request.response().setStatusCode(501).end();
    }
}