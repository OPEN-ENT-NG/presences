package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.constants.EventStores;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.RegisterStatus;
import fr.openent.presences.security.CreateRegisterRight;
import fr.openent.presences.service.*;
import fr.openent.presences.service.impl.*;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.user.UserUtils;

import java.util.List;

public class RegisterController extends ControllerHelper {

    private final RegisterService registerService;
    private final SettingsService settingsService;
    private final EventBus eb;
    private final EventStore eventStore;


    public RegisterController(EventBus eb) {
        super();
        this.registerService = new DefaultRegisterService(eb);
        this.settingsService = new DefaultSettingsService();
        this.eb = eb;
        this.eventStore = EventStoreFactory.getFactory().getEventStore(Presences.class.getSimpleName());
    }

    @Get("/registers/:id")
    @ApiDoc("Retrieve given register")
    @SecuredAction(Presences.READ_REGISTER)
    public void getRegister(HttpServerRequest request) {
        try {
            Integer id = Integer.parseInt(request.params().get("id"));
            registerService.get(id, either -> {
                if (either.isLeft()) {
                    if ("404".equals(either.left().getValue())) {
                        notFound(request);
                    } else {
                        renderError(request);
                    }
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        } catch (NumberFormatException e) {
            log.error("[Presences@RegisterController] Failed to parse register identifier", e);
            badRequest(request);
        }
    }

    @Post("/registers")
    @ApiDoc("Create given register")
    @SecuredAction(Presences.CREATE_REGISTER)
    @Trace(Actions.REGISTER_CREATION)
    public void postRegister(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "register", register -> UserUtils.getUserInfos(eb, request, user -> {
            registerService.create(register, user, either -> {
                if (either.isLeft()) {
                    log.error("[Presences@RegisterController] Failed to create register", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue(), 201);
                }
            });
        }));
    }

    @Put("/registers/:id/status")
    @ApiDoc("Update given register status")
    @ResourceFilter(CreateRegisterRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.REGISTER_VALIDATION)
    public void updateStatus(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, body -> {
            if (!body.containsKey("state_id") && !RegisterStatus.TODO.getStatus().equals(body.getInteger("state_id"))
                    && !RegisterStatus.IN_PROGRESS.getStatus().equals(body.getInteger("state_id"))
                    && !RegisterStatus.DONE.getStatus().equals(body.getInteger("state_id"))) {
                badRequest(request);
                return;
            }

            try {
                Integer registerId = Integer.parseInt(request.getParam("id"));
                Integer state = body.getInteger("state_id");
                registerService.updateStatus(registerId, state, either -> {
                    if (either.isLeft()) {
                        log.error("[Presences@RegisterController::updateStatus] Failed to update register status for register "
                                + registerId, either.left().getValue());
                        renderError(request);
                    } else {
                        noContent(request);
                    }
                });
                if (RegisterStatus.DONE.getStatus().equals(state)) {
                    eventStore.createAndStoreEvent(EventStores.VALIDATE_REGISTER, request);
                }
            } catch (ClassCastException | NumberFormatException e) {
                log.error("[Presences@RegisterController::updateStatus] Failed to parse register identifier", e);
                renderError(request);
            }
        });
    }

    @Get("/structures/:structureId/registers/forgotten")
    @ApiDoc("Get last forgotten registers of the current day")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getLastForgottenRegisters(HttpServerRequest request) {

        if (!request.params().contains("startDate") || !request.params().contains("endDate")) {
            badRequest(request);
            return;
        }

        String structureId = request.getParam("structureId");
        String startDate = request.getParam("startDate");
        String endDate = request.getParam("endDate");
        List<String> groupNames = request.params().getAll("groupName");
        List<String> teacherIds = request.params().getAll("teacherId");

        settingsService.retrieveMultipleSlots(request.getParam("structureId"))
                .onFailure(fail -> {
                    String message = String.format("[Presences@%s::getLastForgottenRegisters] Failed to get " +
                            "multiple slot setting : %s", this.getClass().getSimpleName(), fail.getMessage());
                    log.error(message, fail.getMessage());
                    renderError(request);
                })
                .onSuccess(res -> registerService.getLastForgottenRegistersCourses(structureId, teacherIds, groupNames,
                        startDate, endDate, res.getBoolean(Field.ALLOW_MULTIPLE_SLOTS, true),
                        either -> {
                            if (either.failed()) {
                                log.error("[Presences@CourseController::getLastForgottenRegisters] Failed to retrieve " +
                                        "last forgotten course registers: " + either.cause().getMessage());
                                renderError(request);
                            } else {
                                renderJson(request, either.result());
                            }
                }));
    }
}
