package fr.openent.presences.service.impl;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.EventsHelper;
import fr.openent.presences.common.helper.PersonHelper;
import fr.openent.presences.common.service.*;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DBService;
import fr.openent.presences.enums.*;
import fr.openent.presences.export.*;
import fr.openent.presences.helper.EventByStudentHelper;
import fr.openent.presences.helper.EventHelper;
import fr.openent.presences.helper.ReasonHelper;
import fr.openent.presences.model.*;
import fr.openent.presences.model.Event.Event;
import fr.openent.presences.model.Event.EventByStudent;
import fr.openent.presences.model.Person.Student;
import fr.openent.presences.service.*;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.*;
import fr.wseduc.webutils.template.*;
import fr.wseduc.webutils.template.lambdas.*;
import io.vertx.core.*;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.pdf.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultExportEventService extends DBService implements ExportEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExportEventService.class);
    private final Vertx vertx;
    private final EventService eventService;
    private final SettingsService settingsService;
    private final EventHelper eventHelper;
    private final PersonHelper personHelper;
    private final ReasonService reasonService;
    private final ExportPDFService exportPDFService;

    public DefaultExportEventService(CommonPresencesServiceFactory commonPresencesServiceFactory) {
        this.vertx = commonPresencesServiceFactory.vertx();
        this.eventService = commonPresencesServiceFactory.eventService();
        this.settingsService = commonPresencesServiceFactory.settingsService();
        this.eventHelper = commonPresencesServiceFactory.eventHelper();
        this.personHelper = commonPresencesServiceFactory.personHelper();
        this.reasonService = commonPresencesServiceFactory.reasonService();
        this.exportPDFService = commonPresencesServiceFactory.exportPDFService();
    }

    @Override
    public void getCsvData(String structureId, String startDate, String endDate, List<String> eventType,
                           List<String> listReasonIds, Boolean noReason, Boolean noReasonLateness, List<String> userId, JsonArray userIdFromClasses,
                           List<String> classes, List<String> restrictedClasses, Boolean regularized, Boolean followed, Handler<AsyncResult<List<Event>>> handler) {
        eventService.getEvents(structureId, startDate, endDate, eventType, listReasonIds, noReason, noReasonLateness, userId, userIdFromClasses,
                regularized, followed, null, eventHandler -> {
                    if (eventHandler.isLeft()) {
                        String err = "[Presences@DefaultExportEventService::getCsvData] Failed to fetch events: " + eventHandler.left().getValue();
                        LOGGER.error(err, eventHandler.left().getValue());
                        handler.handle(Future.failedFuture(eventHandler.left().getValue()));
                    } else {
                        List<Event> events = EventHelper.getEventListFromJsonArray(eventHandler.right().getValue(), Event.MANDATORY_ATTRIBUTE);

                        List<Integer> reasonIds = new ArrayList<>();
                        List<String> studentIds = new ArrayList<>();
                        List<String> ownerIds = new ArrayList<>();
                        List<Integer> eventTypeIds = new ArrayList<>();

                        for (Event event : events) {
                            if (!reasonIds.contains(event.getReason().getId())) {
                                reasonIds.add(event.getReason().getId());
                            }
                            if (!studentIds.contains(event.getStudent().getId())) {
                                studentIds.add(event.getStudent().getId());
                            }
                            if (!ownerIds.contains(event.getOwner().getId())) {
                                ownerIds.add(event.getOwner().getId());
                            }
                            if (!eventTypeIds.contains(event.getEventType().getId())) {
                                eventTypeIds.add(event.getEventType().getId());
                            }
                        }

                        // remove potential null value for each list
                        reasonIds.removeAll(Collections.singletonList(null));
                        studentIds.removeAll(Collections.singletonList(null));
                        ownerIds.removeAll(Collections.singletonList(null));
                        eventTypeIds.removeAll(Collections.singletonList(null));

                        Promise<JsonObject> reasonPromise = Promise.promise();
                        Promise<JsonObject> studentPromise = Promise.promise();
                        Promise<JsonObject> ownerPromise = Promise.promise();
                        Promise<JsonObject> eventTypePromise = Promise.promise();

                        eventHelper.addReasonsToEvents(events, reasonIds, reasonPromise);
                        eventHelper.addStudentsToEvents(events, studentIds, restrictedClasses, structureId, studentPromise);
                        eventHelper.addOwnerToEvents(events, ownerIds, ownerPromise);
                        eventHelper.addEventTypeToEvents(events, eventTypeIds, eventTypePromise);

                        Future.all(reasonPromise.future(), eventTypePromise.future(), studentPromise.future(), ownerPromise.future())
                                .onComplete(eventResult -> {
                                    if (eventResult.failed()) {
                                        String message = "[Presences@DefaultEventService::getCsvData] Failed to add " +
                                                "reasons, eventType, students or owner to corresponding event ";
                                        LOGGER.error(message + eventResult.cause().getMessage());
                                        handler.handle(Future.failedFuture(message));
                                    } else {
                                        handler.handle(Future.succeededFuture(events));
                                    }
                                });
                    }
                });
    }

    @Override
    public Future<List<Event>> getCsvData(String structureId, String startDate, String endDate, List<String> eventType,
                                          List<String> listReasonIds, Boolean noReason, Boolean noReasonLateness, List<String> userId, JsonArray userIdFromClasses,
                                          List<String> classes, Boolean regularized, Boolean followed) {
        Promise<List<Event>> promise = Promise.promise();

        getCsvData(structureId, startDate, endDate, eventType, listReasonIds, noReason, noReasonLateness, userId, userIdFromClasses,
                classes, null, regularized, followed, event -> {
                    if (event.failed()) {
                        promise.fail(event.cause().getMessage());
                    } else {
                        promise.complete(event.result());
                    }
                });

        return promise.future();
    }

    @Override
    public Future<Void> processCsvEvent(HttpServerRequest request, AsyncResult<List<Event>> event) {
        Promise<Void> promise = Promise.promise();
        if (event.failed()) {
            LOGGER.error("[Presences@ExportEventController::processCsvEvent] Something went wrong while getting CSV data",
                    event.cause().getMessage());

            promise.fail(event.cause().getMessage());
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

            EventsCSVExport ece = new EventsCSVExport(events, Renders.getHost(request), I18n.acceptLanguage(request));
            ece.setRequest(request);
            ece.setHeader(csvHeaders);
            ece.export();
        }
        return promise.future();
    }

    @Override
    public Future<ExportFile> processCsvEvent(String domain, String local, AsyncResult<List<Event>> event) {
        Promise<ExportFile> promise = Promise.promise();
        if (event.failed()) {
            LOGGER.error("[Presences@ExportEventController::processCsvEvent] Something went wrong while getting CSV data",
                    event.cause().getMessage());

            promise.fail(event.cause().getMessage());
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

            EventsCSVExport ece = new EventsCSVExport(events, domain, local);
            ece.setHeader(csvHeaders, domain, local);
            promise.complete(ece.getExportFile(domain, local));
        }
        return promise.future();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Future<JsonObject> getPdfData(Boolean canSeeAllStudent, String domain, String local, String structureId, String startDate, String endDate,
                                         List<String> eventType, List<String> listReasonIds, Boolean noReason, Boolean noReasonLateness, List<String> userId,
                                         JsonArray userIdFromClasses, Boolean regularized, Boolean followed) {
        Promise<JsonObject> promise = Promise.promise();

        JsonArray studentIdList;

        if (userIdFromClasses != null && !userIdFromClasses.isEmpty() && userId.isEmpty()) {
            List<String> studentIds = ((List<JsonObject>) userIdFromClasses.getList()).stream()
                    .map(user -> user.getString(Field.STUDENTID))
                    .collect(Collectors.toList());
            studentIdList = new JsonArray(studentIds);
        } else {
            studentIdList = new JsonArray(userId);
        }

        Future<JsonObject> settingsFuture = settingsService.retrieve(structureId);
        Future<JsonObject> slotsSettingsFuture = Viescolaire.getInstance().getSlotProfileSetting(structureId);
        Future<JsonArray> reasonFuture = reasonService.fetchAbsenceReason(structureId);

        CompositeFuture.all(settingsFuture, slotsSettingsFuture, reasonFuture)
                .onSuccess(ar -> {
                    Settings settings = new Settings(settingsFuture.result())
                            .setEndOfHalfDayTimeSlot(slotsSettingsFuture.result().getString(Field.END_OF_HALF_DAY));
                    List<Reason> reasons = ReasonHelper.getReasonListFromJsonArray(reasonFuture.result(), Reason.MANDATORY_ATTRIBUTE);

                    processEvents(canSeeAllStudent, settings, structureId, startDate, endDate, eventType, listReasonIds, noReason, noReasonLateness, studentIdList, regularized, followed)
                            .compose(eventByStudent -> formatEventsDataPdf(startDate, endDate, domain, local, settings, reasons, eventByStudent, eventType))
                            .onSuccess(promise::complete)
                            .onFailure(err -> {
                                String message = String.format("[Presences@%s::processEvents]: An error has occurred " +
                                        "during process events steps: %s", this.getClass().getSimpleName(), err.getMessage());
                                LOGGER.error(message, err);
                                promise.fail(err.getMessage());
                            });
                })
                .onFailure(err -> {
                    String message = String.format("[Presences@%s::processEvents]: An error has occurred during retrieve settings: %s",
                            this.getClass().getSimpleName(), err.getMessage());
                    LOGGER.error(message, err);
                    promise.fail(err.getMessage());
                });

        return promise.future();
    }

    @Override
    public Future<Pdf> processPdfEvent(JsonObject events) {
        Promise<Pdf> promise = Promise.promise();
        TemplateProcessor templateProcessor = new TemplateProcessor().escapeHTML(true);
        templateProcessor.setLambda("i18n", new I18nLambda("fr"));
        templateProcessor.setLambda("datetime", new LocaleDateLambda("fr"));
        templateProcessor.processTemplate("pdf/event-list-recap.xhtml", events, writer -> {
            if (writer == null) {
                String message = String.format("[Presences@%s::processPdfEvent] process template has no buffer result",
                        this.getClass().getSimpleName());
                promise.fail(message);
            } else {
                exportPDFService.generatePDF(events.getString(Field.TITLE), writer)
                        .onSuccess(promise::complete)
                        .onFailure(promise::fail);
            }
        });
        return promise.future();
    }

    /**
     * fetch all event by type and by students and proceed on extra fetching (student, reason...)
     *
     * @param setting           Presences settings {@link Settings}
     * @param structureId       Structure identifier
     * @param startDate         start date
     * @param endDate           end date
     * @param eventType         list of event type
     * @param listReasonIds     list of reason identifiers
     * @param noReason          boolean if filtering noReason or not
     * @param userIdFromClasses userId fetched from classes data
     * @return {@link Future<List>} of {@link EventByStudent}
     */
    @SuppressWarnings("unchecked")
    private Future<List<EventByStudent>> processEvents(Boolean canSeeAllStudent, Settings setting, String structureId, String startDate, String endDate,
                                                       List<String> eventType, List<String> listReasonIds, Boolean noReason, Boolean noReasonLateness,
                                                       JsonArray userIdFromClasses, Boolean regularized, Boolean followed) {
        Promise<List<EventByStudent>> promise = Promise.promise();
        List<Integer> eventsTypes = eventType.stream().map(Integer::parseInt).collect(Collectors.toList());
        List<Integer> reasonIds = listReasonIds != null ? listReasonIds.stream().map(Integer::parseInt).collect(Collectors.toList()) : new ArrayList<>();

        List<Future<JsonArray>> eventsTypeResult = new ArrayList<>();
        Promise<JsonArray> init = Promise.promise();
        Future<JsonArray> current = init.future();

        for (Integer type : eventsTypes) {
            current = current.compose(v -> {
                Future<JsonArray> next = this.eventService.getEventsByStudent(canSeeAllStudent, type, userIdFromClasses.getList(), structureId,
                        reasonIds, null, null, startDate, endDate, noReason, noReasonLateness,
                        setting.recoveryMethod(), null, null, regularized, followed);
                eventsTypeResult.add(next);
                return next;
            });
        }
        current
                .compose(aVoid -> mergeEventByStudent(eventsTypeResult))
                .compose(eventByStudents -> fetchStudentForEvents(eventByStudents, structureId))
                .onSuccess(promise::complete)
                .onFailure(err -> {
                    String message = String.format("[Presences@%s::processEvents]: An error has occurred during process events steps: %s",
                            this.getClass().getSimpleName(), err.getMessage());
                    LOGGER.error(message, err);
                    promise.fail(err.getMessage());
                });
        init.complete();
        return promise.future();
    }


    /**
     * format list of eventData with pdf format
     *
     * @param startDate       start date
     * @param endDate         end date
     * @param domain          domain url server
     * @param local           language request
     * @param settings        presences settings {@link Settings}
     * @param reasons         list of reason by current structure fetched {@link Reason}
     * @param eventByStudents Presences settings List of {@link EventByStudent}
     * @param eventsTypes     List of event types
     * @return {@link Future<JsonObject>} with format pdf to process
     */
    private Future<JsonObject> formatEventsDataPdf(String startDate, String endDate, String domain, String local,
                                                   Settings settings, List<Reason> reasons, List<EventByStudent> eventByStudents,
                                                   List<String> eventsTypes) {
        Promise<JsonObject> promise = Promise.promise();
        JsonObject formatPdf = new JsonObject();
        for (String type : eventsTypes) {
            JsonArray mergeEventsByDates = getFormattedEventByStudents(settings, eventByStudents, type);
            // process absence event to display extra data
            addDataToAbsenceEvent(type, mergeEventsByDates, reasons, domain, local);
            formatPdf.put(EventTypeEnum.getEventType(Integer.parseInt(type)).name(), mergeEventsByDates);
        }
        formatPdf.put(Field.TITLE, Field.EXPORT_PDF_EVENTS + "_" + startDate + "_" + endDate);
        promise.complete(formatPdf);
        return promise.future();
    }

    /**
     * Add extra data for pdf format to process
     *
     * @param type               event type
     * @param mergeEventsByDates result of new built events
     * @param reasons            list of reason by current structure fetched {@link Reason}
     * @param domain             domain url server
     * @param local              language request
     */
    @SuppressWarnings("unchecked")
    private void addDataToAbsenceEvent(String type, JsonArray mergeEventsByDates, List<Reason> reasons, String domain, String local) {
        ((List<JsonObject>) mergeEventsByDates.getList()).forEach(event -> {

            event.put(Field.DATE, DateHelper.getDateString(event.getString(Field.START_DATE), DateHelper.YEAR_MONTH_DAY));
            event.put(Field.DISPLAY_START_DATE, DateHelper.getDateString(event.getString(Field.DISPLAY_START_DATE), DateHelper.HOUR_MINUTES));
            event.put(Field.DISPLAY_END_DATE, DateHelper.getDateString(event.getString(Field.DISPLAY_END_DATE), DateHelper.HOUR_MINUTES));

            if (EventTypeEnum.ABSENCE.getType().equals(Integer.parseInt(type))) {
                List<Event> events = EventHelper.getEventListFromJsonArray(event.getJsonArray(Field.EVENTS, new JsonArray()));
                events.forEach(e -> {
                    if (e.getReason().getId() != null) {
                        e.setReason(reasons.stream()
                                .filter(reason -> e.getReason().getId().equals(reason.getId()))
                                .findFirst()
                                .orElse(null));
                    }
                });

                event.put(Field.REGULARIZED, events
                        .stream()
                        .map(Event::isCounsellorRegularisation)
                        .collect(Collectors.toList())
                        .stream()
                        .allMatch(Boolean::valueOf) ?
                        I18n.getInstance().translate("presences.exemptions.csv.attendance.true", domain, local) :
                        I18n.getInstance().translate("presences.exemptions.csv.attendance.false", domain, local));

                Boolean allSameReason = events
                        .stream()
                        .map(e -> e.getReason().getId())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                        .stream()
                        .allMatch(e -> {
                            if (events.get(0).getReason().getId() != null) {
                                return events.get(0).getReason().getId().equals(e);
                            }
                            return false;
                        });

                String reasonLabel = events.get(0).getReason().getLabel() != null ? events.get(0).getReason().getLabel() :
                        "presences.memento.absence.type.NO_REASON";
                event.put(Field.REASON, Boolean.TRUE.equals(allSameReason) ?
                        I18n.getInstance().translate(reasonLabel, domain, local) : Field.MULTIPLE);
            }
        });
    }

    /**
     * with defined event type, will fetch grouped events by its type and build event and its slots event
     *
     * @param settings        presences settings {@link Settings}
     * @param eventByStudents Presences settings List of {@link EventByStudent}
     * @param type            List of event type
     * @return {@link JsonArray}    with format pdf to process
     */
    @SuppressWarnings("unchecked")
    private JsonArray getFormattedEventByStudents(Settings settings, List<EventByStudent> eventByStudents, String type) {
        // split eventByStudent by each looped type
        List<EventByStudent> eventByStudentByType = eventByStudents
                .stream()
                .filter(eventByStudent -> eventByStudent.eventType().getId().equals(Integer.parseInt(type)))
                .collect(Collectors.toList());
        List<JsonObject> eventByStudentByTypeFormat = EventByStudentHelper.eventByStudentToJsonArray(eventByStudentByType);

        // create new list of event instance to factorize events slots
        List<JsonObject> result = EventsHelper.setDatesFromNestedEvents(eventByStudentByTypeFormat, settings.endOfHalfDayTimeSlot());

        return new JsonArray(
                result.stream()
                        .sorted((event1, event2) ->
                                event1.getJsonObject(Field.STUDENT).getString(Field.ID)
                                        .compareToIgnoreCase(event2.getJsonObject(Field.STUDENT).getString(Field.ID))
                        )
                        .collect(Collectors.toList())
        );
    }

    /**
     * merge all event by student in one
     *
     * @param eventsTypeResult Presences settings List of {@link Future} {@link JsonArray} response
     * @return {@link Future<List>} of {@link EventByStudent}
     */
    private Future<List<EventByStudent>> mergeEventByStudent(List<Future<JsonArray>> eventsTypeResult) {
        Promise<List<EventByStudent>> promise = Promise.promise();

        promise.complete(Stream.of(eventsTypeResult)
                .flatMap(Collection::stream)
                .map(eventByStudentsResult -> EventByStudentHelper.eventByStudentList(eventByStudentsResult.result()))
                .collect(Collectors.toList())
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));

        return promise.future();
    }

    /**
     * fetch all students data and inject into event by student list
     *
     * @param eventByStudents Presences settings List of {@link EventByStudent} response
     * @param structureId     Structure identifier
     * @return {@link Future<List>} of {@link EventByStudent}
     */
    private Future<List<EventByStudent>> fetchStudentForEvents(List<EventByStudent> eventByStudents, String structureId) {
        Promise<List<EventByStudent>> promise = Promise.promise();
        List<String> studentIds = eventByStudents.stream().map(eventByStudent -> eventByStudent.student().getId()).collect(Collectors.toList());
        studentIds.removeAll(Collections.singletonList(null));

        personHelper.getStudentsInfo(structureId, studentIds)
                .onSuccess(studentsRes -> {
                    List<Student> students = personHelper.getStudentListFromJsonArray(studentsRes);
                    Map<String, Student> studentMap = students
                            .stream()
                            .collect(Collectors.toMap(Student::getId, Function.identity(), (student1, student2) -> student1));
                    eventByStudents.forEach(eventByStudent -> {
                        if (studentMap.containsKey(eventByStudent.student().getId())) {
                            eventByStudent.setStudent(studentMap.get(eventByStudent.student().getId()));
                        }
                    });
                    promise.complete(eventByStudents);

                })
                .onFailure(promise::fail);
        return promise.future();
    }


}