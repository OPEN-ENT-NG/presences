package fr.openent.massmailing.controller;

import fr.openent.massmailing.Massmailing;
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
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.List;

public class MassmailingController extends ControllerHelper {
    private GroupService groupService;
    private MassmailingService massmailingService = new DefaultMassmailingService();

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
        return params.contains("type") && params.contains("structure") && params.contains("massmailed")
                && params.contains("start_at") && params.contains("start_date") && params.contains("end_date");
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
        final List<String> students = new ArrayList<>();
        if (request.params().contains("student")) students.addAll(request.params().getAll("student"));
        if (request.params().contains("group")) {
            List<String> groups = request.params().getAll("group");
            groupService.getGroupStudents(groups, event -> {
                if (event.isLeft()) {
                    log.error("[Massmailing@MassmailingController] Failed to retrieve students for massmailing status groups");
                    renderError(request);
                    return;
                }

                JsonArray res = event.right().getValue();
                for (int i = 0; i < res.size(); i++) {
                    JsonObject o = res.getJsonObject(i);
                    students.add(o.getString("id", ""));
                }

                processMassmailingStatus(request, students);
            });
        } else {
            processMassmailingStatus(request, students);
        }
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

    @Get("/massmailings/anomalies")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CanAccessMassMailing.class)
    @ApiDoc("Get massmailings anomalies for given arguments")
    public void getMassmailingsAnomalies(HttpServerRequest request) {
        if (!validParams(request) || !validMassmailingType(request)) {
            badRequest(request);
            return;
        }
    }
}
