package fr.openent.massmailing.controller;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.MassmailingType;
import fr.openent.massmailing.security.CanAccessMassMailing;
import fr.openent.massmailing.service.MassmailingService;
import fr.openent.massmailing.service.impl.DefaultMassmailingService;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MassmailingController extends ControllerHelper {
    private GroupService groupService;
    private MassmailingService massmailingService = new DefaultMassmailingService();
    private List<MailingType> typesToCheck = Arrays.asList(MailingType.MAIL);

    public MassmailingController(EventBus eb) {
        this.groupService = new DefaultGroupService(eb);
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
        List<MassmailingType> types = new ArrayList<>();
        if (!request.params().contains("type")) {
            return types;
        }

        List<String> params = request.params().getAll("type");
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

    /**
     * Process mailing status
     *
     * @param request  Request
     * @param students Students list
     */
    private void processMassmailingStatus(HttpServerRequest request, List<String> students) {
        List<MassmailingType> types = getMassMailingTypes(request);
        List<Future> futures = new ArrayList<>();
        String structure = request.getParam("structure");
        Boolean massmailed = request.params().contains("massmailed") ? Boolean.parseBoolean(request.getParam("massmailed")) : null;
        List<Integer> reasons = parseReasons(request.params().getAll("reason"));
        Integer startAt;
        try {
            startAt = Integer.parseInt(request.getParam("start_at"));
        } catch (NumberFormatException e) {
            startAt = 1;
        }
        String startDate = request.getParam("start_date");
        String endDate = request.getParam("end_date");

        for (MassmailingType type : types) {
            Future<JsonObject> future = Future.future();
            futures.add(future);
            massmailingService.getStatus(structure, type, massmailed, reasons, startAt, startDate, endDate, students, FutureHelper.handlerJsonObject(future));
        }

        CompositeFuture.all(futures).setHandler(event -> {
            if (event.failed()) {
                log.error("[Massmailing@MassmailingController] Failed to retrieve status");
                renderError(request);
                return;
            }

            JsonObject res = new JsonObject();
            for (int i = 0; i < types.size(); i++) {
                JsonObject status = (JsonObject) futures.get(i).result();
                res.put(types.get(i).toString(), status.getInteger("status"));
            }

            renderJson(request, res);
        });
    }

    private void processStudents(HttpServerRequest request, Handler<Either<String, List<String>>> handler) {
        final List<String> students = new ArrayList<>();
        if (request.params().contains("student")) students.addAll(request.params().getAll("student"));
        if (request.params().contains("group")) {
            List<String> groups = request.params().getAll("group");
            groupService.getGroupStudents(groups, event -> {
                if (event.isLeft()) {
                    log.error("[Massmailing@MassmailingController] Failed to retrieve students for massmailing status groups");
                    handler.handle(new Either.Left("[Massmailing@MassmailingController] Failed to retrieve students for massmailing status groups"));
                    return;
                }

                JsonArray res = event.right().getValue();
                for (int i = 0; i < res.size(); i++) {
                    JsonObject o = res.getJsonObject(i);
                    students.add(o.getString("id", ""));
                }

                handler.handle(new Either.Right(students));
            });
        } else {
            handler.handle(new Either.Right(students));
        }
    }

    @Get("/massmailings/anomalies")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CanAccessMassMailing.class)
    @ApiDoc("Get massmailings anomalies for given arguments")
    public void getMassmailingsAnomalies(HttpServerRequest request) {
        if (!validParams(request) || !validMassmailingType(request)) {
            badRequest(request);
            return;
        }

        processStudents(request, event -> {
            if (event.isLeft()) {
                log.error("[Massmailing@MassmailingController] Failed to retrieve students for anomalies request");
                renderError(request);
                return;
            }

            List<String> students = event.right().getValue();
            List<MassmailingType> types = getMassMailingTypes(request);
            List<Future> futures = new ArrayList<>();
            String structure = request.getParam("structure");
            Boolean massmailed = Boolean.parseBoolean(request.getParam("massmailed"));
            List<Integer> reasons = parseReasons(request.params().getAll("reason"));
            Integer startAt;
            try {
                startAt = Integer.parseInt(request.getParam("start_at"));
            } catch (NumberFormatException e) {
                startAt = 1;
            }
            String startDate = request.getParam("start_date");
            String endDate = request.getParam("end_date");

            for (MassmailingType type : types) {
                Future<JsonArray> future = Future.future();
                futures.add(future);
                massmailingService.getCountEventByStudent(structure, type, massmailed, reasons, startAt, startDate, endDate, students, FutureHelper.handlerJsonArray(future));
            }

            CompositeFuture.all(futures).setHandler(compositeEvent -> {
                if (compositeEvent.failed()) {
                    log.error("[Massmailing@MassmailingController] Failed to retrieve count event for anomalies request", compositeEvent.cause());
                    renderError(request);
                    return;
                }

                List<String> studentList = getStudentsList(futures);
                processAnomalies(studentList, anomaliesEvent -> {
                    if (anomaliesEvent.isLeft()) {
                        log.error("[Massmailing@MassmailingController] Failed to process anomalies for anomalies request", anomaliesEvent.left().getValue());
                        renderError(request);
                        return;
                    }

                    JsonArray anomalies = anomaliesEvent.right().getValue();
                    HashMap<String, JsonObject> map = new HashMap<>();
                    for (int i = 0; i < anomalies.size(); i++) {
                        map.put(anomalies.getJsonObject(i).getString("id"), anomalies.getJsonObject(i));
                    }

                    for (int i = 0; i < types.size(); i++) {
                        JsonArray res = (JsonArray) futures.get(i).result();
                        for (int j = 0; j < res.size(); j++) {
                            if (!map.containsKey(res.getJsonObject(j).getString("student_id"))) continue;
                            JsonObject student = map.get(res.getJsonObject(j).getString("student_id"));
                            if (!student.containsKey("count")) student.put("count", new JsonObject());
                            student.getJsonObject("count").put(types.get(i).name(), res.getJsonObject(j).getInteger("count"));
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
        JsonArray array = new JsonArray();
        List<String> keys = new ArrayList<>(map.keySet());
        for (String key : keys) {
            array.add(map.get(key));
        }

        return array;
    }

    private List<String> getStudentsList(List<Future> futures) {
        List<String> students = new ArrayList<>();
        for (Future future : futures) {
            JsonArray res = (JsonArray) future.result();
            for (int i = 0; i < res.size(); i++) {
                JsonObject student = res.getJsonObject(i);
                if (!students.contains(student.getString("student_id"))) students.add(student.getString("student_id"));
            }
        }

        return students;
    }
}
