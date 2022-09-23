package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.WorkflowActionsCouple;
import fr.openent.presences.export.PresencesCSVExport;
import fr.openent.presences.security.PresenceReadRight;
import fr.openent.presences.security.presence.ManagePresenceRight;
import fr.openent.presences.service.CommonPresencesServiceFactory;
import fr.openent.presences.service.PresenceService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.user.UserUtils;

import java.util.Collections;
import java.util.List;

public class PresencesController extends ControllerHelper {

    private static final String END_DATE = "endDate";
    private static final String START_DATE = "startDate";
    private static final String STRUCTURE_ID = "structureId";

    private final PresenceService presencesService;
    private final EventStore eventStore;

    public PresencesController(CommonPresencesServiceFactory commonPresencesServiceFactory) {
        this.presencesService = commonPresencesServiceFactory.presenceService();
        this.eventStore = EventStoreFactory.getFactory().getEventStore(Presences.class.getSimpleName());
    }

    @Get("")
    @ApiDoc("Render view")
    @SecuredAction("view")
    public void view(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            JsonObject action = new JsonObject()
                    .put("action", "user.getActivesStructure")
                    .put("module", Presences.dbSchema)
                    .put("structures", new JsonArray(user.getStructures()));
            eb.send(Presences.ebViescoAddress, action, event -> {
                JsonObject body = (JsonObject) event.result().body();
                if (event.failed() || "error".equals(body.getString("status"))) {
                    log.error("[Presences@PresencesController] Failed to retrieve actives structures");
                    renderError(request);
                } else {
                    renderView(request, new JsonObject().put("structures", body.getJsonArray("results", new JsonArray())));
                }
            });
        });
        eventStore.createAndStoreEvent("ACCESS", request);
    }

    @Get("/presences")
    @ApiDoc("Retrieve presences")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PresenceReadRight.class)
    public void getPresences(final HttpServerRequest request) {
        get(request, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/presence")
    @ApiDoc("Create presence")
    @SecuredAction(Presences.CREATE_PRESENCE)
    @Trace(Actions.PRESENCE_CREATION)
    public void createPresence(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, presences -> {
            if (!isValidBody(presences)) {
                badRequest(request);
                return;
            }
            UserUtils.getUserInfos(eb, request, user ->
                    presencesService.create(user, presences, DefaultResponseHandler.defaultResponseHandler(request)));
        });
    }

    @Put("/presence")
    @ApiDoc("Update presence")
    @SecuredAction(Presences.MANAGE_PRESENCE)
    @Trace(Actions.PRESENCE_UPDATE)
    public void updatePresence(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, presences -> {
            if (!isValidBody(presences)) {
                badRequest(request);
                return;
            }
            presencesService.update(presences, DefaultResponseHandler.defaultResponseHandler(request));
        });
    }

    private boolean isValidBody(JsonObject presence) {
        return presence.containsKey(STRUCTURE_ID) &&
                presence.containsKey(START_DATE) &&
                presence.containsKey(END_DATE) &&
                presence.containsKey("markedStudents") &&
                !presence.getJsonArray("markedStudents").isEmpty();
    }

    @Delete("/presence")
    @ApiDoc("Delete presence")
    @ResourceFilter(ManagePresenceRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.PRESENCE_DELETE)
    public void deletePresence(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        String presenceId = request.getParam("id");
        presencesService.delete(presenceId, DefaultResponseHandler.defaultResponseHandler(request));
    }

    @Get("/presences/export")
    @ApiDoc("Export presences")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PresenceReadRight.class)
    public void exportPresences(final HttpServerRequest request) {
        get(request, result -> {
            if (result.isLeft()) {
                log.error("[PresencesController@exportPresences] Failed to export Presences", result.left());
                renderError(request);
                return;
            }
            JsonArray presences = result.right().getValue();

            PresencesCSVExport pce = new PresencesCSVExport(presences);
            pce.setRequest(request);
            pce.export();
        });
    }

    private void get(final HttpServerRequest request, Handler<Either<String, JsonArray>> handler) {
        UserUtils.getUserInfos(eb, request, user -> {
            MultiMap params = request.params();
            String structureId = params.get(STRUCTURE_ID);
            String startDate = params.get(START_DATE);
            String endDate = params.get(END_DATE);
            List<String> userIds = params.getAll(Field.STUDENTID);
            List<String> audienceIds = params.getAll(Field.AUDIENCEID);
            List<String> ownerIds = params.getAll(Field.OWNERID);
            boolean hasRestrictedRight = WorkflowActionsCouple.READ_PRESENCE.hasOnlyRestrictedRight(user, UserType.TEACHER.equals(user.getType()));
            ownerIds = hasRestrictedRight ? Collections.singletonList(user.getUserId()) :
                    ownerIds;

            if (!request.params().contains(STRUCTURE_ID) || !request.params().contains(START_DATE) ||
                    !request.params().contains(END_DATE)) {
                badRequest(request);
                return;
            }

            presencesService.get(structureId, startDate, endDate, userIds, ownerIds, audienceIds, handler);
        });
    }

}
