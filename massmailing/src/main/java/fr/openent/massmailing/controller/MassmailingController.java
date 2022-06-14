package fr.openent.massmailing.controller;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.actions.*;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.MassmailingType;
import fr.openent.massmailing.mailing.*;
import fr.openent.massmailing.security.BodyCanAccessMassMailing;
import fr.openent.massmailing.security.CanAccessMassMailing;
import fr.openent.massmailing.service.MassmailingService;
import fr.openent.massmailing.service.impl.DefaultMassmailingService;
import fr.openent.presences.common.helper.*;
import fr.openent.presences.common.service.*;
import fr.openent.presences.common.service.impl.*;
import fr.openent.presences.core.constants.*;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.*;

public class MassmailingController extends ControllerHelper {
    private final GroupService groupService;
    private final JsonObject config;
    private final Vertx vertx;
    private final Storage storage;
    private final MassmailingService massmailingService;
    private final UserService userService;
    private final List<MailingType> typesToCheck = Arrays.asList(MailingType.MAIL, MailingType.SMS);

    public MassmailingController(EventBus eb, Vertx vertx, JsonObject config, Storage storage) {
        this.config = config;
        this.vertx = vertx;
        this.groupService = new DefaultGroupService(eb);
        this.massmailingService = new DefaultMassmailingService(eb);
        this.userService = new DefaultUserService();
        this.storage = storage;
    }

    @Get("")
    @SecuredAction(Massmailing.VIEW)
    @ApiDoc("Render mass mailer view")
    public void view(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            JsonObject action = new JsonObject()
                    .put("action", "user.getActivesStructure")
                    .put("module", "presences")
                    .put("structures", new JsonArray(user.getStructures()));
            eb.send("viescolaire", action, event -> {
                JsonObject body = (JsonObject) event.result().body();
                if (event.failed() || "error".equals(body.getString("status"))) {
                    log.error("[Massmailer@MassmailerController] Failed to retrieve actives structures");
                    renderError(request);
                } else {
                    renderView(request, new JsonObject().put("structures", body.getJsonArray("results", new JsonArray())));
                }
            });
        });
    }

    private Boolean validParams(HttpServerRequest request) {
        MultiMap params = request.params();
        return params.contains("type") && params.contains("structure") && params.contains("start_at")
                && params.contains("start_date") && params.contains("end_date");
    }

    private Boolean validMassmailingType(HttpServerRequest request) {
        boolean state = false;
        List<String> reasons = request.params().getAll("type");
        for (String reason : reasons) {
            try {
                MassmailingType.valueOf(reason);
                state = true;
            } catch (IllegalArgumentException e) {
                state = false;
                break;
            }
        }

        return state;
    }

    private List<MassmailingType> getMassMailingTypes(HttpServerRequest request) {
        if (!request.params().contains("type")) {
            return new ArrayList<>();
        }

        return getMassMailingTypes(request.params().getAll("type"));
    }

    private List<MassmailingType> getMassMailingTypes(List<String> params) {
        List<MassmailingType> types = new ArrayList<>();
        for (String type : params) {
            try {
                types.add(MassmailingType.valueOf(type));
            } catch (IllegalArgumentException e) {
                continue;
            }
        }

        return types;
    }

    @Get("/massmailings/status")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CanAccessMassMailing.class)
    @ApiDoc("Get massmailings status for given arguments")
    public void getMassmailingsStatus(HttpServerRequest request) {
        if (!validParams(request) || !validMassmailingType(request)) {
            badRequest(request);
            return;
        }

        // Create student list.
        // If params contains student then add those params to students list
        // If params contains group then retrieve students and add it to student list
        // With that list, call Presence mod to retrieve status
        processStudents(request, event -> {
            if (event.isLeft()) {
                log.error("[Massmailing@MassmailingController] Failed to retrieve students for status request");
                renderError(request);
                return;
            }

            List<String> students = event.right().getValue();
            processMassmailingStatus(request, students);
        });
    }


    private List<Integer> parseReasons(List<String> reasons) {
        List<Integer> values = new ArrayList<>();
        for (String reason : reasons) {
            try {
                values.add(Integer.parseInt(reason));
            } catch (NumberFormatException e) {
                continue;
            }
        }

        return values;
    }

    private List<Integer> parsePunishmentsTypes(List<String> punishmentsTypes) {
        List<Integer> values = new ArrayList<>();
        for (String punishmentType : punishmentsTypes) {
            try {
                values.add(Integer.parseInt(punishmentType));
            } catch (NumberFormatException e) {
                continue;
            }
        }

        return values;
    }

    /**
     * Process mailing status
     *
     * @param request  Request
     * @param students Students list
     */
    private void processMassmailingStatus(HttpServerRequest request, List<String> students) {
        List<MassmailingType> types = getMassMailingTypes(request);
        List<Future<JsonObject>> futures = new ArrayList<>();
        String structure = request.getParam(Field.STRUCTURE);
        Boolean massmailed = request.params().contains(Field.MASSMAILED) ?
                Boolean.parseBoolean(request.getParam(Field.MASSMAILED)) : null;
        List<Integer> reasons = parseReasons(request.params().getAll(Field.REASON));
        List<Integer> punishmentsTypes = parsePunishmentsTypes(request.params().getAll(Field.PUNISHMENTTYPE));
        List<Integer> sanctionsTypes = parsePunishmentsTypes(request.params().getAll(Field.SANCTIONTYPE));
        boolean noReasons = !request.params().contains(Field.NO_REASON)
                || Boolean.parseBoolean(request.getParam(Field.NO_REASONS));
        boolean noLatenessReasons = Boolean.parseBoolean(request.getParam(Field.NO_LATENESS_REASONS));
        Integer startAt;
        try {
            startAt = Integer.parseInt(request.getParam(Field.START_AT));
        } catch (NumberFormatException e) {
            startAt = 1;
        }
        String startDate = request.getParam(Field.START_DATE);
        String endDate = request.getParam(Field.END_DATE);

        for (MassmailingType type : types) {
            Promise<JsonObject> promise = Promise.promise();
            futures.add(promise.future());
            massmailingService.getStatus(structure, type, massmailed, reasons, punishmentsTypes, sanctionsTypes, startAt, startDate,
                    endDate, students, noLatenessReasons, FutureHelper.handlerJsonObject(promise));
        }

        FutureHelper.all(futures)
                .onFailure(fail -> {
                    String message = String.format("[Massmailing@%s::processMassmailingStatus] Failed to retrieve status]",
                            this.getClass().getSimpleName());
                    log.error(message);
                    renderError(request);
                })
                .onSuccess(event -> {
                    // If user has restricted right and is searching forbidden class/student return 0
                    boolean hasFilterWithRestrictedParam = students.isEmpty() &&
                            ((request.params().contains(Field.GROUP) && !request.params().getAll(Field.GROUP).isEmpty())
                                    || (request.params().contains(Field.STUDENT) && !request.params().getAll(Field.STUDENT).isEmpty()));


                    JsonObject res = new JsonObject();
                    for (int i = 0; i < types.size(); i++) {
                        JsonObject status = futures.get(i).result();
                        res.put(types.get(i).toString(), hasFilterWithRestrictedParam ? 0 : status.getInteger(Field.STATUS));
                    }

                    renderJson(request, res);
                });


    }

    private void processStudents(HttpServerRequest request, Handler<Either<String, List<String>>> handler) {
        UserUtils.getUserInfos(eb, request, userInfos -> {

            String teacherId = (WorkflowHelper.hasRight(userInfos, WorkflowActions.MANAGE_RESTRICTED.toString())
                    && UserType.TEACHER.equals(userInfos.getType())) ?
                    userInfos.getUserId() : null;

            String structureId = request.getParam(Field.STRUCTURE);


            this.userService.getStudentsFromTeacher(teacherId, structureId)
                    .onFailure(fail -> renderError(request))
                    .onSuccess(restrictedStudentIds -> {

                        List<String> students = request.params().contains(Field.STUDENT) ?
                                request.params().getAll(Field.STUDENT) : new ArrayList<>();
                        List<String> groups = request.params().contains(Field.GROUP) ?
                                request.params().getAll(Field.GROUP) : new ArrayList<>();

                        groupService.getGroupStudents(groups, event -> {

                            if (event.isLeft()) {
                                String message = String.format("[Massmailing@%s::processStudents] Failed to retrieve " +
                                        "students for massmailing status groups", this.getClass().getSimpleName());
                                log.error(message);
                                handler.handle(new Either.Left<>(message));
                                return;
                            }

                            JsonArray res = event.right().getValue();
                            for (int i = 0; i < res.size(); i++) {
                                JsonObject o = res.getJsonObject(i);
                                students.add(o.getString(Field.ID, ""));
                            }


                            if (students.isEmpty()) {
                                students.addAll(restrictedStudentIds.isEmpty() ? new ArrayList<>() : restrictedStudentIds);
                            }

                            handler.handle(new Either.Right<>(restrictedStudentIds.isEmpty() ? students :
                                    students.stream().filter(restrictedStudentIds::contains).collect(Collectors.toList())));
                        });
                    });
        });
    }

    @Get("/massmailings/anomalies")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CanAccessMassMailing.class)
    @ApiDoc("Get massmailings anomalies for given arguments")
    public void getMassmailingsAnomalies(HttpServerRequest request) {
        if (Boolean.TRUE.equals(!validParams(request)) || Boolean.TRUE.equals(!validMassmailingType(request))) {
            badRequest(request);
            return;
        }

        processStudents(request, event -> {
            if (event.isLeft()) {
                String message = String.format("[Massmailing@%s::getMassmailingsAnomalies] Failed to " +
                        "retrieve students for anomalies request", this.getClass().getSimpleName());
                log.error(message);
                renderError(request);
                return;
            }

            List<String> students = event.right().getValue();
            List<MassmailingType> types = getMassMailingTypes(request);
            List<Future<JsonArray>> futures = new ArrayList<>();
            String structure = request.getParam(Field.STRUCTURE);
            Boolean massmailed = request.params().contains(Field.MASSMAILED) ?
                    Boolean.parseBoolean(request.getParam(Field.MASSMAILED)) : null;
            List<Integer> reasons = parseReasons(request.params().getAll(Field.REASON));
            List<Integer> punishmentsTypes = parsePunishmentsTypes(request.params().getAll(Field.PUNISHMENTTYPE));
            List<Integer> sanctionsTypes = parsePunishmentsTypes(request.params().getAll(Field.SANCTIONTYPE));
            boolean noReasons = !request.params().contains(Field.NOREASON)
                    || Boolean.parseBoolean(request.getParam(Field.NO_REASONS));
            Integer startAt;
            try {
                startAt = Integer.parseInt(request.getParam(Field.START_AT));
            } catch (NumberFormatException e) {
                startAt = 1;
            }
            String startDate = request.getParam(Field.START_DATE);
            String endDate = request.getParam(Field.END_DATE);

            for (MassmailingType type : types) {
                Promise<JsonArray> promise = Promise.promise();
                futures.add(promise.future());
                massmailingService.getCountEventByStudent(structure, type, massmailed, reasons, punishmentsTypes, sanctionsTypes,
                        startAt, startDate, endDate, students, noReasons, FutureHelper.handlerJsonArray(promise));
            }

            FutureHelper.all(futures)
                    .onFailure(fail -> {
                        String message = String.format("[Massmailing@%s::getMassmailingsAnomalies] Failed to retrieve count " +
                                "event for anomalies request", this.getClass().getSimpleName());
                        log.error(message, fail.getCause().getMessage());
                        renderError(request);
                    })
                    .onSuccess(evt -> {
                        List<String> studentList = getStudentsList(futures);
                        processAnomalies(studentList, anomaliesEvent -> {
                            if (anomaliesEvent.isLeft()) {
                                String message = String.format("[Massmailing@%s::getMassmailingsAnomalies] Failed to process " +
                                        "anomalies for anomalies request", this.getClass().getSimpleName());
                                log.error(message, anomaliesEvent.left().getValue());
                                renderError(request);
                                return;
                            }

                            boolean hasFilterWithRestrictedParam = students.isEmpty() &&
                                    ((request.params().contains(Field.GROUP) && !request.params().getAll(Field.GROUP).isEmpty())
                                    || (request.params().contains(Field.STUDENT) && !request.params().getAll(Field.STUDENT).isEmpty()));

                            // If user has restricted right and is searching forbidden class/student return empty array
                            JsonArray anomalies = hasFilterWithRestrictedParam ? new JsonArray() : anomaliesEvent.right().getValue();

                            HashMap<String, JsonObject> map = mapById(anomalies);
                            for (int i = 0; i < types.size(); i++) {
                                JsonArray res = futures.get(i).result();
                                for (int j = 0; j < res.size(); j++) {
                                    if (!map.containsKey(res.getJsonObject(j).getString(Field.STUDENT_ID))) continue;
                                    JsonObject student = map.get(res.getJsonObject(j).getString(Field.STUDENT_ID));
                                    if (!student.containsKey(Field.COUNT)) student.put(Field.COUNT, new JsonObject());
                                    student.getJsonObject(Field.COUNT).put(types.get(i).name(),
                                            res.getJsonObject(j).getInteger(Field.COUNT));
                                }
                            }

                            renderJson(request, transformMapToArray(map));
                        });
                    });
        });
    }

    /**
     * Retrieve all anomalies for student list based on typesToCheck List
     *
     * @param students Student list
     * @param handler  Function handler returning data
     */
    private void processAnomalies(List<String> students, Handler<Either<String, JsonArray>> handler) {
        List<Future> futures = new ArrayList<>();

        for (MailingType type : typesToCheck) {
            Future<JsonArray> future = Future.future();
            futures.add(future);
            massmailingService.getAnomalies(type, students, FutureHelper.handlerJsonArray(future));
        }

        CompositeFuture.all(futures).setHandler(event -> {
            if (event.failed()) {
                String message = "[Massmailing@MassmailingController] Failed to retrieve anomalies";
                log.error(message, event.cause());
                handler.handle(new Either.Left<>(message));
                return;
            }

            HashMap<String, JsonObject> map = new HashMap<>();
            for (int i = 0; i < futures.size(); i++) {
                JsonArray result = (JsonArray) futures.get(i).result();
                for (int j = 0; j < result.size(); j++) {
                    JsonObject anomaly = result.getJsonObject(j);
                    if (!map.containsKey(anomaly.getString("id"))) {
                        map.put(anomaly.getString("id"), new JsonObject().put("bug", new JsonObject()).put("id", anomaly.getString("id")));
                    }
                    JsonObject student = map.get(anomaly.getString("id"));
                    student.getJsonObject("bug").put(typesToCheck.get(i).name(), true);
                    student.put("displayName", anomaly.getString("displayName"));
                    student.put("className", anomaly.getString("className"));
                }
            }

            handler.handle(new Either.Right<>(transformMapToArray(map)));
        });
    }

    private JsonArray transformMapToArray(HashMap<String, JsonObject> map) {
        List<JsonObject> array = new ArrayList<>();
        List<String> keys = new ArrayList<>(map.keySet());
        for (String key : keys) {
            array.add(map.get(key));
        }

        array.sort((o1, o2) -> o1.getString("displayName").compareToIgnoreCase(o2.getString("displayName")));
        return new JsonArray(array);
    }

    private List<String> getStudentsList(List<Future<JsonArray>> futures) {
        List<String> students = new ArrayList<>();
        for (Future<JsonArray> future : futures) {
            for (int i = 0; i < future.result().size(); i++) {
                JsonObject student = future.result().getJsonObject(i);
                if (!students.contains(student.getString(Field.STUDENT_ID)))
                    students.add(student.getString(Field.STUDENT_ID));
            }
        }

        return students;
    }

    @Get("/massmailings/prefetch/:mailingType")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CanAccessMassMailing.class)
    @ApiDoc("Prefetch massmailing")
    public void prefetch(HttpServerRequest request) {
        if (Boolean.TRUE.equals(!validParams(request) || !validMassmailingType(request)) || !validMailingType(request)) {
            badRequest(request);
            return;
        }

        processStudents(request, studentEvent -> {
            if (studentEvent.isLeft()) {
                String message = String.format("[Massmailing@%s::prefetch] Failed to retrieve students for prefetch request",
                        this.getClass().getSimpleName());
                log.error(message);
                renderError(request);
                return;
            }

            List<String> students = studentEvent.right().getValue();
            List<MassmailingType> types = getMassMailingTypes(request);
            MailingType mailingType = getMailingType(request);
            List<Future<JsonArray>> futures = new ArrayList<>();
            String structure = request.getParam(Field.STRUCTURE);
            Boolean massmailed = request.params().contains(Field.MASSMAILED)
                    ? Boolean.parseBoolean(request.getParam(Field.MASSMAILED)) : null;
            List<Integer> reasons = parseReasons(request.params().getAll(Field.REASON));
            List<Integer> punishmentsTypes = parsePunishmentsTypes(request.params().getAll(Field.PUNISHMENTTYPE));
            List<Integer> sanctionsTypes = parsePunishmentsTypes(request.params().getAll(Field.SANCTIONTYPE));
            boolean noReasons = !request.params().contains(Field.NO_REASON)
                    || Boolean.parseBoolean(request.getParam(Field.NO_REASONS));
            Integer startAt;
            try {
                startAt = Integer.parseInt(request.getParam(Field.START_AT));
            } catch (NumberFormatException e) {
                startAt = 1;
            }
            String startDate = request.getParam(Field.START_DATE);
            String endDate = request.getParam(Field.END_DATE);

            for (MassmailingType type : types) {
                Promise<JsonArray> promise = Promise.promise();
                futures.add(promise.future());
                massmailingService.getCountEventByStudent(structure, type, massmailed, reasons, punishmentsTypes, sanctionsTypes,
                        startAt, startDate, endDate, students, noReasons, FutureHelper.handlerJsonArray(promise));
            }

            FutureHelper.all(futures)
                    .onFailure(fail -> {
                        String message = String.format("[Massmailing@%s::prefetch] Failed to retrieve count " +
                                "event for prefetch request", this.getClass().getSimpleName());
                        log.error(message, fail.getCause().getMessage());
                        renderError(request);
                    })
                    .onSuccess(event -> {
                        processRelatives(mailingType, getStudentsList(futures), relativesEvent -> {
                            if (relativesEvent.isLeft()) {
                                String message = String.format("[Massmailing@%s::prefetch] Failed to retrieve relatives",
                                        this.getClass().getSimpleName());
                                log.error(message);
                                renderError(request);
                                return;
                            }

                            HashMap<String, JsonObject> relativesMap = mapById(relativesEvent.right().getValue()
                                    .getJsonArray(Field.VALUES, new JsonArray()));

                            for (int i = 0; i < futures.size(); i++) {
                                JsonArray result = futures.get(i).result();

                                for (int j = 0; j < result.size(); j++) {
                                    JsonObject count = result.getJsonObject(j);
                                    if (!relativesMap.containsKey(count.getString(Field.STUDENT_ID))) continue;
                                    JsonObject student = relativesMap.get(count.getString(Field.STUDENT_ID));
                                    if (!student.containsKey(Field.EVENTS)) student.put(Field.EVENTS, new JsonObject());
                                    student.getJsonObject(Field.EVENTS).put(types.get(i).name(), count.getInteger(Field.COUNT));
                                }
                            }

                            boolean hasFilterWithRestrictedParam = students.isEmpty() &&
                                    ((request.params().contains(Field.GROUP) && !request.params().getAll(Field.GROUP).isEmpty())
                                            || (request.params().contains(Field.STUDENT)
                                            && !request.params().getAll(Field.STUDENT).isEmpty()));


                            JsonArray studentsObject = hasFilterWithRestrictedParam ? new JsonArray() :
                                    transformMapToArray(relativesMap);

                            JsonObject response = new JsonObject()
                                    .put(Field.TYPE, mailingType.name());


                            JsonObject countObject = new JsonObject()
                                    .put(Field.ANOMALIES, hasFilterWithRestrictedParam ?
                                            0 : relativesEvent.right().getValue().getInteger(Field.ANOMALIES_COUNT))
                                    .put(Field.STUDENTS, hasFilterWithRestrictedParam ? 0 : studentsObject.size())
                                    .put(Field.MASSMAILING, hasFilterWithRestrictedParam ? 0 : countMassmailing(studentsObject));

                            response.put(Field.COUNTS, countObject);
                            response.put(Field.STUDENTS, ArrayHelper.sort(studentsObject, Field.DISPLAYNAME));

                            renderJson(request, response);
                        });
                    });
        });
    }

    private Integer countMassmailing(JsonArray students) {
        Integer count = 0;
        for (int i = 0; i < students.size(); i++) {
            JsonObject student = students.getJsonObject(i);
            JsonArray relative = student.getJsonArray("relative");
            for (int j = 0; j < relative.size(); j++) {
                JsonObject rel = relative.getJsonObject(j);
                if (rel.getValue("contact") != null) count++;
            }
        }

        return count;
    }

    private HashMap<String, JsonObject> mapById(JsonArray arr) {
        HashMap<String, JsonObject> map = new HashMap<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject o = arr.getJsonObject(i);
            map.put(o.getString("id", ""), o);
        }

        return map;
    }

    private void processRelatives(MailingType mailingType, List<String> students, Handler<Either<String, JsonObject>> handler) {
        Future<JsonArray> anomaliesFuture = Future.future();
        Future<JsonArray> relativesFuture = Future.future();
        massmailingService.getAnomalies(mailingType, students, FutureHelper.handlerJsonArray(anomaliesFuture));
        massmailingService.getRelatives(mailingType, students, FutureHelper.handlerJsonArray(relativesFuture));
        CompositeFuture.all(anomaliesFuture, relativesFuture).setHandler(futureEvent -> {
            if (futureEvent.failed()) {
                String message = "[Massmailing@processRelatives] Failed to retrieve data";
                log.error(message, futureEvent.cause());
                handler.handle(new Either.Left<>(message));
                return;
            }

            List<String> anomalies = getIdsList(anomaliesFuture.result());
            JsonObject result = new JsonObject()
                    .put("values", filterRelatives(relativesFuture.result(), anomalies))
                    .put("anomalies_count", anomalies.size());
            handler.handle(new Either.Right<>(result));
        });
    }

    private JsonArray filterRelatives(JsonArray relatives, List<String> anomalies) {
        JsonArray res = new JsonArray();
        for (int i = 0; i < relatives.size(); i++) {
            String id = relatives.getJsonObject(i).getString("id");
            if (!anomalies.contains(id)) res.add(relatives.getJsonObject(i));
        }
        return res;
    }

    private List<String> getIdsList(JsonArray arr) {
        List<String> identifiers = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject o = arr.getJsonObject(i);
            identifiers.add(o.getString("id", ""));
        }

        return identifiers;
    }

    private boolean validMailingType(HttpServerRequest request) {
        boolean state = false;
        List<String> types = request.params().getAll("mailingType");
        for (String type : types) {
            try {
                MailingType.valueOf(type);
                state = true;
            } catch (IllegalArgumentException e) {
                state = false;
                break;
            }
        }

        return state;
    }

    private MailingType getMailingType(HttpServerRequest request) {
        try {
            String mailingType = request.getParam("mailingType").toUpperCase();
            return MailingType.valueOf(mailingType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("[Massmailing@MassmailingController] Failed to parse mailing type", e);
            return MailingType.MAIL;
        }
    }

    @Post("/massmailings/:mailingType")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(BodyCanAccessMassMailing.class)
    @Trace(Action.MASSMAILING_DELIVERY)
    @ApiDoc("Post massmailing. In case of MAIL or SMS type, send mails or SMS. In case of PDF, generate PDF and download it")
    public void postMassmailing(HttpServerRequest request) {
        if (!validMailingType(request)) {
            badRequest(request);
            return;
        }

        MailingType mailingType = getMailingType(request);

        RequestUtils.bodyToJson(request, pathPrefix + Field.MASSMAILING, body -> {
            String structure = body.getString(Field.STRUCTURE);
            Template template = new Template(mailingType, body.getInteger(Field.TEMPLATE), structure, config);
            Boolean massmailed = body.containsKey(Field.MASSMAILED) ? body.getBoolean(Field.MASSMAILED) : null;
            List<MassmailingType> massmailingTypeList = getMassMailingTypes(body.getJsonArray(Field.EVENT_TYPES).getList());
            List<Integer> reasons = body.getJsonArray(Field.REASONS).getList();
            List<Integer> punishmentsTypes = body.getJsonArray(Field.PUNISHMENTSTYPES).getList();
            List<Integer> sanctionsTypes = body.getJsonArray(Field.SANCTIONSTYPES).getList();
            String start = body.getString(Field.START);
            String end = body.getString(Field.END);
            boolean noReason = body.getBoolean(Field.NO_REASON, true);
            boolean isMultiple = body.getBoolean(Field.ISMULTIPLE, false);
            JsonObject students = body.getJsonObject(Field.STUDENTS);
            template.setLocale(I18n.acceptLanguage(request));
            template.setDomain(getHost(request));

            MassMailingProcessor mailing;
            switch (mailingType) {
                case MAIL:
                    mailing = new Mail(structure, template, massmailed, massmailingTypeList, reasons, punishmentsTypes,
                            sanctionsTypes, start, end, noReason, isMultiple, students);
                    break;
                case SMS:
                    mailing = new Sms(eb, structure, template, massmailed, massmailingTypeList, reasons, punishmentsTypes,
                            sanctionsTypes, start, end, noReason, isMultiple, students);
                    break;
                case PDF:
                    mailing = new Pdf(eb, vertx, storage, config, request, structure, template, massmailed, massmailingTypeList,
                            reasons, punishmentsTypes, sanctionsTypes, start, end, noReason, isMultiple, students);
                    break;
                default:
                    badRequest(request);
                    return;
            }

            mailing.massmail(event -> {
                if (event.isLeft()) {
                    String message = "[Massmailing@MassmailingController] An error occurred with massmailing";
                    log.error(String.format("%s %s", message, event.left().getValue()));
                    renderError(request);
                }
                else log.info("[Massmailing@MassmailingController] Massmailing completed with success");
            });
            if (!(mailing instanceof Pdf)) {
                request.response().setStatusCode(202).end();
            }
        });
    }
}
