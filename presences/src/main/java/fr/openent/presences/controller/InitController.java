package fr.openent.presences.controller;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.common.incidents.Incidents;
import fr.openent.presences.common.massmailing.Massmailing;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.InitTypeEnum;
import fr.openent.presences.enums.WorkflowActions;
import fr.openent.presences.security.Manage;
import fr.openent.presences.service.InitService;
import fr.openent.presences.service.impl.DefaultAbsenceService;
import fr.openent.presences.service.impl.DefaultInitService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class InitController extends ControllerHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitController.class);

    private EventBus eventBus;
    InitService initService = new DefaultInitService();

    public InitController(EventBus eb) {
        super();
        this.eventBus = eb;
    }

    @Get("/initialization/structures/:id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(Manage.class)
    @ApiDoc("Retrieve structure initialization status")
    public void getInitializationStatus(HttpServerRequest request) {
        String structure = request.getParam("id");
        initService.retrieveInitializationStatus(structure, defaultResponseHandler(request));
    }


    @Post("/initialization/structures/:id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(Manage.class)
    @ApiDoc("Init given structure with default settings")
    public void init(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "initialization", body -> {
            Optional<InitTypeEnum> initTypeEnum = Arrays.stream(InitTypeEnum.values())
                    .filter(initTypeEnum1 -> initTypeEnum1.name().equals(body.getString(Field.INIT_TYPE)))
                    .findFirst();

            if (!initTypeEnum.isPresent()) {
                badRequest(request);
                return;
            }

            UserUtils.getUserInfos(eventBus, request, user -> {
                if ((initTypeEnum.get().equals(InitTypeEnum.ONE_D) && !WorkflowHelper.hasRight(user, WorkflowActions.INIT_SETTINGS_1D.toString()))
                        || (initTypeEnum.get().equals(InitTypeEnum.TWO_D) && !WorkflowHelper.hasRight(user, WorkflowActions.INIT_SETTINGS_2D.toString()))) {
                    unauthorized(request);
                    return;
                }

                String structure = request.getParam(Field.ID);
                Promise<JsonObject> reasonsFuture = Promise.promise();
                Promise<JsonObject> actionsFuture = Promise.promise();
                Promise<JsonObject> settingsFuture = Promise.promise();
                Promise<JsonObject> disciplinesFuture = Promise.promise();
                Promise<JsonObject> massmailingTemplatesFuture = Promise.promise();
                Promise<JsonObject> incidentTypeFuture = Promise.promise();
                Promise<JsonObject> incidentPlacesFuture = Promise.promise();
                Promise<JsonObject> incidentProtagonists = Promise.promise();
                Promise<JsonObject> incidentSeriousness = Promise.promise();
                Promise<JsonObject> incidentPartner = Promise.promise();
                Promise<JsonObject> incidentPunishmentType = Promise.promise();
                List<Future> futures = Arrays.asList(reasonsFuture.future(), actionsFuture.future(), settingsFuture.future(),
                        disciplinesFuture.future(), massmailingTemplatesFuture.future(), incidentTypeFuture.future(),
                        incidentPlacesFuture.future(), incidentProtagonists.future(), incidentSeriousness.future(),
                        incidentPartner.future(), incidentPunishmentType.future());
                CompositeFuture.all(futures).onComplete(res -> {
                    JsonArray statements = new JsonArray();
                    for (Future<JsonObject> future : futures) {
                        if (future.succeeded()) {
                            statements.add(future.result());
                        } else {
                            String message = String.format("[Presences@%s::init] Failed to init %s ",
                                    this.getClass().getSimpleName(), future.cause().getCause());
                            LOGGER.error(message);
                        }
                    }

                    Sql.getInstance().transaction(statements, SqlResult.validUniqueResultHandler(defaultResponseHandler(request)));
                });

                initService.getReasonsStatement(request, structure, initTypeEnum.get(), reasonsFuture);
                initService.getActionsStatement(request, structure, initTypeEnum.get(), actionsFuture);
                initService.getSettingsStatement(structure, initTypeEnum.get(), settingsFuture);
                initService.getPresencesDisciplinesStatement(request, structure, initTypeEnum.get(), disciplinesFuture);
                Massmailing.getInstance().getInitTemplatesStatement(request, structure, user.getUserId(), initTypeEnum.get(), FutureHelper.handlerEitherPromise(massmailingTemplatesFuture));
                Incidents.getInstance().getInitIncidentTypesStatement(structure, initTypeEnum.get(), FutureHelper.handlerEitherPromise(incidentTypeFuture));
                Incidents.getInstance().getInitIncidentPlacesStatement(structure, initTypeEnum.get(), FutureHelper.handlerEitherPromise(incidentPlacesFuture));
                Incidents.getInstance().getInitIncidentProtagonistTypeStatement(structure, initTypeEnum.get(), FutureHelper.handlerEitherPromise(incidentProtagonists));
                Incidents.getInstance().getInitIncidentSeriousnessStatement(structure, initTypeEnum.get(), FutureHelper.handlerEitherPromise(incidentSeriousness));
                Incidents.getInstance().getInitIncidentPartnerStatement(structure, initTypeEnum.get(), FutureHelper.handlerEitherPromise(incidentPartner));
                Incidents.getInstance().getInitIncidentPunishmentTypeStatement(structure, initTypeEnum.get(), FutureHelper.handlerEitherPromise(incidentPunishmentType));
            });
        });
    }
}
