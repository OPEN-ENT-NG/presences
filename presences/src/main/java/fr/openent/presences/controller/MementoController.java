package fr.openent.presences.controller;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.ReasonService;
import fr.openent.presences.service.impl.DefaultEventService;
import fr.openent.presences.service.impl.DefaultReasonService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

public class MementoController extends ControllerHelper {

    private final String REGULARIZED = "REGULARIZED";
    private final String UNREGULARIZED = "UNREGULARIZED";
    private final String LATENESS = "LATENESS";
    private final String DEPARTURE = "DEPARTURE";
    private final String NO_REASON = "NO_REASON";

    private final EventService eventService;
    private final ReasonService reasonService = new DefaultReasonService();

    public MementoController(EventBus eventBus) {
        super();
        eventService = new DefaultEventService(eventBus);
    }

    private boolean validTypes(List<String> types) {
        boolean valid = true;
        for (String type : types) {
            switch (type) {
                case REGULARIZED:
                case UNREGULARIZED:
                case LATENESS:
                case DEPARTURE:
                case NO_REASON:
                    valid = true;
                    break;
                default:
                    valid = false;
            }
        }

        return valid;
    }

    @Get("/memento/students/:id/absences/summary")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    @ApiDoc("Retrieve for given student its absence summary based on period setting")
    public void getAbsenceSummary(HttpServerRequest request) {
        String student = request.getParam("id");
        String structure = request.getParam("structure");
        String start = request.getParam("start");
        String end = request.getParam("end");
        List<String> types = request.params().getAll("type");

        if (!validTypes(types)) {
            badRequest(request);
            return;
        }

        reasonService.fetchAbsenceReason(structure, eventAsync -> {
            if (eventAsync.isLeft()) {
                log.error("[Presences@MementoController::getAbsenceSummary] failed to fetch reasons for getting our events " + eventAsync.left().getValue());
                badRequest(request);
            } else {

                List<Integer> reasonIds = ((List<JsonObject>) eventAsync.right().getValue().getList())
                        .stream()
                        .map(reason -> reason.getLong("id").intValue())
                        .collect(Collectors.toList());

                List<Future> futures = new ArrayList<>();
                for (String type : types) {
                    Future<JsonArray> future = Future.future();
                    switch (type) {
                        case UNREGULARIZED:
                            eventService.getEventsByStudent(1, Collections.singletonList(student), structure, reasonIds, null, start, end, false, null, false, FutureHelper.handlerJsonArray(future));
                            break;
                        case REGULARIZED:
                            eventService.getEventsByStudent(1, Collections.singletonList(student), structure, reasonIds, null, start, end, false, null, true, FutureHelper.handlerJsonArray(future));
                            break;
                        case NO_REASON:
                            eventService.getEventsByStudent(1, Collections.singletonList(student), structure, new ArrayList<>(), null, start, end, true, null, false, FutureHelper.handlerJsonArray(future));
                            break;
                        case LATENESS:
                            eventService.getEventsByStudent(2, Collections.singletonList(student), structure, new ArrayList<>(), null, start, end, true, null, null, FutureHelper.handlerJsonArray(future));
                            break;
                        case DEPARTURE:
                            eventService.getEventsByStudent(3, Collections.singletonList(student), structure, new ArrayList<>(), null, start, end, true, null, null, FutureHelper.handlerJsonArray(future));
                            break;
                        default:
                            //There is no default case
                            log.error("[Presences@MementoController::getAbsenceSummary] There is no case for value: " + type);
                    }
                    futures.add(future);
                }

                Future<JsonObject> absenceRateFuture = Future.future();
                eventService.getAbsenceRate(student, structure, start, end, FutureHelper.handlerJsonObject(absenceRateFuture));
                futures.add(absenceRateFuture);

                CompositeFuture.all(futures).setHandler(res -> {
                    if (res.failed()) {
                        renderError(request);
                        return;
                    }

                    Map<Integer, JsonObject> months = generateMonthMap(types, getPeriodRange(start, end));

                    for (int i = 0; i < types.size(); i++) {
                        String type = types.get(i);
                        JsonArray values = (JsonArray) futures.get(i).result();
                        if (values.isEmpty()) continue;
                        ((List<JsonObject>) values.getList()).forEach(event -> {
                            try {
                                Date evtStartDate = DateHelper.parse(event.getString("start_date"));
                                JsonObject monthTypes = months.get(DateHelper.getMonthNumber(evtStartDate))
                                        .getJsonObject("types");
                                monthTypes.put(type, monthTypes.getInteger(type) + 1);
                            } catch (ParseException e) {
                                log.error("[Presences@MementoController] Failed to parse date: " + event.getString("start_date"), e);
                            }
                        });
                    }

                    JsonArray result = new JsonArray();
                    months.keySet().forEach(key -> result.add(months.get(key)));
                    JsonObject response = new JsonObject()
                            .put("months", result)
                            .put("absence_rate", absenceRateFuture.result().getValue("absence_rate"));
                    renderJson(request, response);
                });
            }
        });
    }

    private HashMap<Integer, JsonObject> generateMonthMap(List<String> types, List<Integer> periodRange) {
        LinkedHashMap<Integer, JsonObject> months = new LinkedHashMap<>();
        for (Integer monthNumber : periodRange) {
            JsonObject month = new JsonObject();
            JsonObject monthTypes = new JsonObject();
            for (String type : types) {
                monthTypes.put(type, 0);
            }

            month.put("month", monthNumber);
            month.put("types", monthTypes);
            months.put(monthNumber, month);
        }

        return months;
    }

    private List<Integer> getPeriodRange(String start, String end) {
        List<Integer> range = new ArrayList<>();
        try {
            Date startDate = DateHelper.parse(start, DateHelper.YEAR_MONTH_DAY);
            Date endDate = DateHelper.parse(end, DateHelper.YEAR_MONTH_DAY);
            int monthNumber = DateHelper.getMonthNumber(startDate);
            int yearValue = DateHelper.getYear(startDate);
            Date newDate = startDate;
            range.add(monthNumber);
            while (!(monthNumber == DateHelper.getMonthNumber(endDate) && yearValue == DateHelper.getYear(endDate))) {
                newDate = DateHelper.add(newDate, Calendar.MONTH, 1);
                monthNumber = DateHelper.getMonthNumber(newDate);
                yearValue = DateHelper.getYear(newDate);
                range.add(monthNumber);
            }
        } catch (ParseException e) {
            log.error("[Presences@MementoController] Failed to parse date value", e);
        }

        return range;
    }
}
