package fr.openent.incidents.service.impl;

import fr.openent.incidents.service.*;
import fr.openent.presences.common.helper.FutureHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserInfos;

import java.util.*;


public class DefaultEventStudentService implements EventStudentService {

    private static final Logger log = LoggerFactory.getLogger(DefaultEventStudentService.class);

    private final String INCIDENT = "INCIDENT";
    private final String PUNISHMENT = "PUNISHMENT";

    private IncidentsService incidentsService;
    private PunishmentService punishmentService;
    ProtagonistTypeService protagonistTypeService = new DefaultProtagonistTypeService();
    IncidentsTypeService incidentsTypeService = new DefaultIncidentsTypeService();
    private EventBus eb;

    public DefaultEventStudentService(EventBus eventBus) {
        super();
        incidentsService = new DefaultIncidentsService(eventBus);
        punishmentService = new DefaultPunishmentService(eventBus);
        eb = eventBus;
    }

    @Override
    public void get(MultiMap body, Handler<AsyncResult<JsonObject>> handler) {
        String student = body.get("id");
        String structure = body.get("structure_id");
        String limit = body.get("limit");
        String offset = body.get("offset");
        String start = body.get("start_at");
        String end = body.get("end_at");
        List<String> types = body.getAll("type");

        if (!validTypes(types)) {
            String message = "[Presences@DefaultEventStudentService::get] Types are not valid.";
            handler.handle(Future.failedFuture(message));
            return;
        }

        Future<JsonArray> protagonistsFuture = Future.future();
        Future<JsonArray> typesFuture = Future.future();

        // Get incident types to map it thanks type_id on incident events results
        incidentsTypeService.get(structure, typesResult -> {
            if (typesResult.isLeft()) {
                String message = ("[Presences@DefaultEventStudentService::get] Fail to retrieve incidents types.");
                typesFuture.fail(message + " " + typesResult.left().getValue());
                return;
            }

            typesFuture.complete(typesResult.right().getValue());
        });

        // Get protagonist types to map it thanks protagonist_type_id on incident events results
        protagonistTypeService.get(structure, protagonistResult -> {
            if (protagonistResult.isLeft()) {
                String message = "[Presences@DefaultEventStudentService::get] Fail to retrieve protagonist types.";
                protagonistsFuture.fail(message + " " + protagonistResult.left().getValue());
                return;
            }
            protagonistsFuture.complete(protagonistResult.right().getValue());
        });

        CompositeFuture.all(protagonistsFuture, typesFuture).setHandler(incidentSettingsResult -> {
            if (incidentSettingsResult.failed()) {
                log.error(incidentSettingsResult.cause().getMessage());
                handler.handle(Future.failedFuture(incidentSettingsResult.cause().getMessage()));
                return;
            }

            // Maps are easier to manipulate to retrieve one occurrence by id.
            Map<Integer, JsonObject> protagonists = new HashMap<>();
            for (Object o : protagonistsFuture.result()) {
                JsonObject protagonist = (JsonObject) o;
                protagonists.put(protagonist.getInteger("id"), protagonist);
            }

            Map<Integer, JsonObject> incidentTypes = new HashMap<>();
            for (Object o : typesFuture.result()) {
                JsonObject type = (JsonObject) o;
                incidentTypes.put(type.getInteger("id"), type);
            }

            getSortedEventsByTypes(body, types, structure, student, protagonists, incidentTypes, start, end, limit, offset, false, result -> {
                if (result.failed()) {
                    handler.handle(Future.failedFuture(result.cause()));
                    return;
                }

                Future<JsonObject> future = Future.future();
                if (limit == null && offset == null) { // If we get all result, we just need to get array size to get total results
                    future.complete(getTotalsByTypes(types, false, result.result()));
                } else { // Else, we use same queries with a count result
                    getSortedEventsByTypes(body, types, structure, student, protagonists, incidentTypes, start, end, null, null, true, resultAllEvents -> {
                        if (resultAllEvents.failed()) {
                            handler.handle(Future.failedFuture(resultAllEvents.cause()));
                            return;
                        }
                        future.complete(getTotalsByTypes(types, true, resultAllEvents.result()));
                    });
                }

                CompositeFuture.all(Collections.singletonList(future)).setHandler(resultTotals -> {
                    if (resultTotals.failed()) {
                        String message = "[Presences@DefaultEventStudentService::get] Failed to get totals from events.";
                        handler.handle(Future.failedFuture(message + " " + resultTotals.cause()));
                        return;
                    }

                    JsonObject response = new JsonObject()
                            .put("limit", limit)
                            .put("offset", offset)
                            .put("all", result.result())
                            .put("totals", future.result());
                    handler.handle(Future.succeededFuture(response));
                });
            });
        });
    }

    // check if types are in {{ INCIDENT / PUNISHMENT }}
    private boolean validTypes(List<String> types) {
        boolean valid = true;
        for (String type : types) {
            switch (type) {
                case INCIDENT:
                case PUNISHMENT:
                    valid = true;
                    break;
                default:
                    valid = false;
            }
        }

        return valid;
    }

    // Send request to get each events (thanks the great method corresponding to type {{ INCIDENT / PUNISHMENT }}).
    // Handler will return them mapped by type (with the great protagonist for punishment, and great incident type for incident).
    // If isCountGetter,  result will (still by type) will be the total of date.
    private void getSortedEventsByTypes(MultiMap body, List<String> types, String structure, String student, Map protagonists, Map incidentTypes, String start, String end,
                                        String limit, String offset, Boolean isCountGetter, Handler<AsyncResult<JsonObject>> handler) {
        List<Future> futures = new ArrayList<>();
        for (String type : types) {
            Future<JsonArray> future = Future.future();
            futures.add(future);

            if (isCountGetter && type.equals(PUNISHMENT)) { // When  isCountGetter, Punishments type have a specific method to get total result.
                getCountPunishment(body, student, FutureHelper.handlerJsonArray(future));
            } else {
                getTypedEventsByStudent(body, type, structure, student, start, end, limit, offset, FutureHelper.handlerJsonArray(future));
            }
        }

        CompositeFuture.all(futures).setHandler(result -> {
            if (result.failed()) {
                String message = "[Presences@DefaultEventStudentService::getSortedEventsByTypes] Failed to retrieve events info.";
                handler.handle(Future.failedFuture(message + " " + result.cause()));
                return;
            }
            handler.handle(Future.succeededFuture(getSortedByTypesEventsFromFutures(types, protagonists, incidentTypes, futures)));
        });
    }

    // Send Punishment request to get total result for a student (responding to each filters)
    private void getCountPunishment(MultiMap body, String studentId, Handler<Either<String, JsonArray>> handler) {
        body.add("id", (String) null);
        body.add("start_at", body.get("start_at").replace('-', '/'));
        body.add("end_at", body.get("end_at").replace('-', '/'));
        body.add("limit", "");
        body.add("offset", "");
        UserInfos user = new UserInfos();
        user.setUserId(studentId);
        punishmentService.count(user, body, true, result -> {
            if (result.failed()) {
                handler.handle(new Either.Left<>(result.cause().getMessage()));
            }
            handler.handle(new Either.Right<>(new JsonArray().add(new JsonObject().put("count", result.result()))));
        });
    }

    // Get events for a student, corresponding to the type mentioned
    private void getTypedEventsByStudent(MultiMap body, String type, String structureId, String studentId, String start,
                                         String end, String limit, String offset, Handler<Either<String, JsonArray>> handler) {
        switch (type) {
            case INCIDENT:
                incidentsService.get(structureId, start, end, studentId, limit, offset, handler);
                break;
            case PUNISHMENT:
                body.add("id", (String) null);
                body.add("start_at", body.get("start_at").replace('-', '/'));
                body.add("end_at", body.get("end_at").replace('-', '/'));
                body.add("limit", limit);
                body.add("offset", offset);
                UserInfos user = new UserInfos();
                user.setUserId(studentId);
                punishmentService.get(user, body, true, result -> {
                    if (result.failed()) {
                        handler.handle(new Either.Left<>(result.cause().getMessage()));
                    }
                    handler.handle(new Either.Right<>(result.result().getJsonArray("all")));
                });
                break;
            default:
                //There is no default case
                String message = "There is no case for value: " + type + ".";
                log.error(message);
                handler.handle(new Either.Left(message));
        }
    }

    //Map events result (resultFutures) by types {{ INCIDENT / PUNISHMENT }}
    private JsonObject getSortedByTypesEventsFromFutures(List<String> types, Map protagonists, Map incidentTypes, List<Future> resultFutures) {
        JsonObject sorted_events = new JsonObject();
        for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);
            sorted_events.put(type, new JsonArray());

            JsonArray values = (JsonArray) resultFutures.get(i).result();
            if (values.isEmpty()) continue;
            ((List<JsonObject>) values.getList()).forEach(event -> {
                event.put("protagonist", protagonists.get(event.getInteger("protagonist_type_id")));
                if (type.equals(INCIDENT)) {
                    event.put("type", incidentTypes.get(event.getInteger("type_id")));
                }
                sorted_events.getJsonArray(type).add(event);
            });
        }
        return sorted_events;
    }

    // Retrieve totals by types
    private JsonObject getTotalsByTypes(List<String> types, Boolean isFromCountPunishment, JsonObject sorted_events) {
        JsonObject totals = new JsonObject();
        for (String type : types) {
            if (isFromCountPunishment && type.equals(PUNISHMENT)) {
                totals.put(type, sorted_events.getJsonArray(type).getJsonObject(0).getLong("count"));
            } else {
                totals.put(type, sorted_events.getJsonArray(type).size());
            }
        }
        return totals;
    }

}