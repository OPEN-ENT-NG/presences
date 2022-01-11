package fr.openent.incidents.service.impl;

import fr.openent.incidents.service.*;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.core.constants.Field;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


public class DefaultEventStudentService implements EventStudentService {

    private static final Logger log = LoggerFactory.getLogger(DefaultEventStudentService.class);

    private final String INCIDENT = "INCIDENT";
    private final String PUNISHMENT = "PUNISHMENT";

    private final IncidentsService incidentsService;
    private final PunishmentService punishmentService;
    ProtagonistTypeService protagonistTypeService = new DefaultProtagonistTypeService();
    IncidentsTypeService incidentsTypeService = new DefaultIncidentsTypeService();

    public DefaultEventStudentService(EventBus eventBus) {
        super();
        incidentsService = new DefaultIncidentsService(eventBus);
        punishmentService = new DefaultPunishmentService(eventBus);
    }

    @Override
    public Future<JsonObject> get(String structureId, String studentId, List<String> types, String startAt, String endAt, String limit, String offset) {
        Promise<JsonObject> promise = Promise.promise();
        Map<Integer, JsonObject> protagonists = new HashMap<>();
        Map<Integer, JsonObject> incidentTypes = new HashMap<>();
        setProtagonistsAndIncidentTypes(structureId, protagonists, incidentTypes)
                .compose(res -> getEventsData(types, structureId, Collections.singletonList(studentId), protagonists, incidentTypes, startAt, endAt, limit, offset))
                .onFailure(promise::fail)
                .onSuccess(result -> {
                    JsonObject response = result.get(studentId)
                            .put(Field.LIMIT, limit)
                            .put(Field.OFFSET, offset);
                    promise.complete(response);
                });
        return promise.future();
    }

    @Override
    public Future<JsonObject> get(String structureId, List<String> studentIds, List<String> types, String startAt, String endAt, String limit, String offset) {
        Promise<JsonObject> promise = Promise.promise();
        Map<Integer, JsonObject> protagonists = new HashMap<>();
        Map<Integer, JsonObject> incidentTypes = new HashMap<>();
        setProtagonistsAndIncidentTypes(structureId, protagonists, incidentTypes)
                .compose(res -> getEventsData(types, structureId, studentIds, protagonists, incidentTypes, startAt, endAt, limit, offset))
                .onFailure(promise::fail)
                .onSuccess(result -> {
                    JsonObject response = new JsonObject()
                            .put(Field.LIMIT, limit)
                            .put(Field.OFFSET, offset)
                            .put(Field.STUDENTS_EVENTS, result);
                    promise.complete(response);
                });
        return promise.future();
    }


    public Future<Void> setProtagonistsAndIncidentTypes(String structureId, Map<Integer, JsonObject> protagonists,
                                                        Map<Integer, JsonObject> incidentTypes) {
        Promise<Void> promise = Promise.promise();

        Future<Map<Integer, JsonObject>> protagonistsFuture = getProtagonists(structureId);
        Future<Map<Integer, JsonObject>> typesFuture = getIncidentTypes(structureId);
        CompositeFuture.all(protagonistsFuture, typesFuture)
                .onSuccess(result -> {
                    protagonists.putAll(protagonistsFuture.result());
                    incidentTypes.putAll(typesFuture.result());
                    promise.complete();
                })
                .onFailure(error -> {
                    String message =
                            String.format("[Incidents@%s::setProtagonistsAndIncidentTypes] Fail to get protagonist data.",
                                    this.getClass().getSimpleName());
                    log.error(String.format("%s %s", message, error.getMessage()));
                    promise.fail(error);
                });

        return promise.future();
    }

    @SuppressWarnings("unchecked")
    public Future<Map<Integer, JsonObject>> getIncidentTypes(String structureId) {
        Promise<Map<Integer, JsonObject>> promise = Promise.promise();
        incidentsTypeService.get(structureId, typesResult -> {
            if (typesResult.isLeft()) {
                String message =
                        String.format("[Incidents@%s::getIncidentTypes] Fail to retrieve incidents types.",
                                this.getClass().getSimpleName());
                log.error(String.format("%s %s", message, typesResult.left().getValue()));
                promise.fail(message);
                return;
            }
            promise.complete(
                    ((List<JsonObject>) typesResult.right().getValue().getList()).stream()
                            .collect(Collectors.toMap(type -> type.getInteger(Field.ID), type -> type))
            );
        });
        return promise.future();
    }

    @SuppressWarnings("unchecked")
    public Future<Map<Integer, JsonObject>> getProtagonists(String structureId) {
        Promise<Map<Integer, JsonObject>> promise = Promise.promise();

        protagonistTypeService.get(structureId, protagonistResult -> {
            if (protagonistResult.isLeft()) {
                String message =
                        String.format("[Incidents@%s::getProtagonists] Fail to retrieve protagonist types.",
                                this.getClass().getSimpleName());
                log.error(String.format("%s %s", message, protagonistResult.left().getValue()));
                promise.fail(message);
                return;
            }

            promise.complete(
                    ((List<JsonObject>) protagonistResult.right().getValue().getList()).stream()
                            .collect(Collectors.toMap(protagonist -> protagonist.getInteger(Field.ID), protagonist -> protagonist))
            );
        });

        return promise.future();
    }

    private Future<Map<String, JsonObject>> getEventsData(List<String> types, String structureId, List<String> studentIds,
                                                          Map<Integer, JsonObject> protagonists, Map<Integer, JsonObject> incidentTypes,
                                                          String start, String end, String limit, String offset) {
        Promise<Map<String, JsonObject>> promise = Promise.promise();

        Map<String, JsonObject> studentsEvents = studentIds.stream()
                .collect(Collectors.toMap(
                        studentId -> studentId,
                        studentId -> new JsonObject()
                                .put(Field.ALL, new JsonObject(types.stream().collect(Collectors.toMap(type -> type, type -> new JsonArray()))))
                                .put(Field.TOTALS, new JsonObject(types.stream().collect(Collectors.toMap(type -> type, type -> 0))))
                ));


        Future<Map<String, JsonObject>> eventsCountFuture = getCountEvents(types, structureId, studentIds, start, end, studentsEvents);
        Future<Map<String, JsonObject>> eventsFuture = getEvents(types, structureId, studentIds, protagonists, incidentTypes, start,
                end, limit, offset, studentsEvents);

        CompositeFuture.all(eventsCountFuture, eventsFuture)
                .onSuccess(result -> promise.complete(studentsEvents))
                .onFailure(error -> {
                    String message =
                            String.format("[Incidents@%s::getEventsData] Fail to get events or totals events.",
                                    this.getClass().getSimpleName());
                    log.error(String.format("%s %s", message, error.getMessage()));
                    promise.fail(message);
                });

        return promise.future();
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
    private Future<Map<String, JsonObject>> getEvents(List<String> types, String structureId, List<String> studentIds, Map<Integer, JsonObject> protagonists,
                                                      Map<Integer, JsonObject> incidentTypes, String start, String end, String limit, String offset,
                                                      Map<String, JsonObject> studentsEvents) {
        Promise<Map<String, JsonObject>> promise = Promise.promise();
        List<Future<JsonArray>> futures = new ArrayList<>();
        for (String type : types)
            futures.add(getTypedEventsByStudent(type, structureId, studentIds, start, end, limit, offset));

        FutureHelper.all(futures)
                .onSuccess(result -> promise.complete(getSortedByTypesEventsFromFutures(types, protagonists, incidentTypes, futures, studentsEvents)))
                .onFailure(error -> {
                    String message =
                            String.format("[Incidents@%s::getEvents] Fail to retrieve events info.",
                                    this.getClass().getSimpleName());
                    log.error(String.format("%s %s", message, error.getMessage()));
                    promise.fail(message);
                });

        return promise.future();
    }

    private Future<Map<String, JsonObject>> getCountEvents(List<String> types, String structureId, List<String> studentIds,
                                                           String start, String end, Map<String, JsonObject> studentsEvents) {
        Promise<Map<String, JsonObject>> promise = Promise.promise();
        List<Future<JsonArray>> futures = new ArrayList<>();
        for (String type : types)
            futures.add(getCount(type, structureId, studentIds, start, end));

        FutureHelper.all(futures)
                .onSuccess(result -> promise.complete(setCountEventsByStudents(types, futures, studentsEvents)))
                .onFailure(error -> {
                    String message =
                            String.format("[Incidents@%s::getCountEvents] Fail to count events.",
                                    this.getClass().getSimpleName());
                    log.error(String.format("%s %s", message, error.getMessage()));
                    promise.fail(message);
                });

        return promise.future();
    }

    // Send Punishment request to get total result for a student (responding to each filters)

    private Future<JsonArray> getCount(String type, String structureId, List<String> studentIds, String startAt, String endAt) {
        Promise<JsonArray> promise = Promise.promise();
        switch (type) {
            case INCIDENT:
                incidentsService.countByStudents(structureId, startAt, endAt, studentIds)
                        .onFailure(promise::fail)
                        .onSuccess(promise::complete);
                break;
            case PUNISHMENT:
                punishmentService.getPunishmentCountByStudent(structureId, startAt, endAt, studentIds, Collections.emptyList(),
                        null, null, FutureHelper.handlerJsonArray(promise));
                break;
            default:
                //There is no default case
                String message = "There is no case for value: " + type + ".";
                log.error(message);
                promise.fail(message);
        }
        return promise.future();
    }

    @SuppressWarnings("unchecked")
    private Map<String, JsonObject> setCountEventsByStudents(List<String> types, List<Future<JsonArray>> resultFutures,
                                                             Map<String, JsonObject> studentsEvents) {
        for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);
            List<JsonObject> values = resultFutures.get(i).result().getList();
            if (values.isEmpty()) continue;
            for (JsonObject countType : values) {
                JsonObject studentEvents = studentsEvents.get(countType.getString(Field.STUDENT_ID));
                if (studentEvents != null && studentEvents.containsKey(Field.TOTALS))
                    studentEvents.getJsonObject(Field.TOTALS).put(type, countType.getInteger(Field.COUNT));
            }
        }

        return studentsEvents;

    }

    // Get events for a student, corresponding to the type mentioned
    private Future<JsonArray> getTypedEventsByStudent(String type, String structureId, List<String> studentIds, String startAt,
                                                      String endAt, String limit, String offset) {
        Promise<JsonArray> promise = Promise.promise();
        switch (type) {
            case INCIDENT:
                incidentsService.get(structureId, startAt, endAt, studentIds, limit, offset)
                        .onFailure(promise::fail)
                        .onSuccess(promise::complete);
                break;
            case PUNISHMENT:
                punishmentService.get(null, null, structureId, startAt, endAt, studentIds, Collections.emptyList(),
                        Collections.emptyList(), Collections.emptyList(), false, null, limit, offset, result -> {
                            if (result.failed()) {
                                promise.fail(result.cause().getMessage());
                            }
                            promise.complete(result.result().getJsonArray(Field.ALL));
                        });
                break;
            default:
                //There is no default case
                String message =
                        String.format("[Incidents@%s::getCountEvents] There is no case for value: %s.",
                                this.getClass().getSimpleName(), type);
                log.error(message);
                promise.fail(message);
        }
        return promise.future();
    }

    //Map events result (resultFutures) by types {{ INCIDENT / PUNISHMENT }}
    @SuppressWarnings("unchecked")
    private Map<String, JsonObject> getSortedByTypesEventsFromFutures(List<String> types, Map<Integer, JsonObject> protagonists,
                                                                      Map<Integer, JsonObject> incidentTypes,
                                                                      List<Future<JsonArray>> resultFutures,
                                                                      Map<String, JsonObject> studentsEvents) {
        for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);
            JsonArray values = resultFutures.get(i).result();
            if (values == null || values.isEmpty()) continue;
            ((List<JsonObject>) values.getList()).forEach(event -> {
                JsonObject studentEvents;
                event.put(Field.PROTAGONIST, protagonists.get(event.getInteger(Field.PROTAGONIST_TYPE_ID)));
                if (type.equals(INCIDENT)) {
                    event.put(Field.TYPE, incidentTypes.get(event.getInteger(Field.TYPEID)));
                    studentEvents = studentsEvents.get(event.getString(Field.STUDENT_ID, ""));
                } else
                    studentEvents = studentsEvents.get(event.getJsonObject(Field.STUDENT, new JsonObject()).getString(Field.ID, ""));


                if (studentEvents != null && studentEvents.containsKey(Field.ALL))
                    studentEvents.getJsonObject(Field.ALL).getJsonArray(type, new JsonArray()).add(event);

            });
        }
        return studentsEvents;
    }

}