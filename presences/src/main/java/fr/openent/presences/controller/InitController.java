package fr.openent.presences.controller;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.incidents.Incidents;
import fr.openent.presences.common.massmailing.Massmailing;
import fr.openent.presences.security.Manage;
import fr.openent.presences.service.InitService;
import fr.openent.presences.service.impl.DefaultInitService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserUtils;

import java.util.Arrays;
import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class InitController extends ControllerHelper {
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
        UserUtils.getUserInfos(eventBus, request, user -> {
            String structure = request.getParam("id");
            Future<JsonObject> reasonsFuture = Future.future();
            Future<JsonObject> actionsFuture = Future.future();
            Future<JsonObject> settingsFuture = Future.future();
            Future<JsonObject> disciplinesFuture = Future.future();
            Future<JsonObject> massmailingTemplatesFuture = Future.future();
            Future<JsonObject> incidentTypeFuture = Future.future();
            Future<JsonObject> incidentPlacesFuture = Future.future();
            Future<JsonObject> incidentProtagonists = Future.future();
            Future<JsonObject> incidentSeriousness = Future.future();
            Future<JsonObject> incidentPartner = Future.future();
            List<Future> futures = Arrays.asList(reasonsFuture, actionsFuture, settingsFuture, disciplinesFuture, massmailingTemplatesFuture, incidentTypeFuture, incidentPlacesFuture, incidentProtagonists, incidentSeriousness, incidentPartner);
            CompositeFuture.all(futures).setHandler(res -> {
                JsonArray statements = new JsonArray();
                for (Future<JsonObject> future : futures) {
                    statements.add(future.result());
                }

                Sql.getInstance().transaction(statements, SqlResult.validUniqueResultHandler(defaultResponseHandler(request)));
            });

            initService.getReasonsStatement(request, structure, reasonsFuture);
            initService.getActionsStatement(request, structure, actionsFuture);
            initService.getSettingsStatement(structure, settingsFuture);
            initService.getPresencesDisciplinesStatement(request, structure, disciplinesFuture);
            Massmailing.getInstance().getInitTemplatesStatement(request, structure, user.getUserId(), FutureHelper.handlerJsonObject(massmailingTemplatesFuture));
            Incidents.getInstance().getInitIncidentTypesStatement(structure, FutureHelper.handlerJsonObject(incidentTypeFuture));
            Incidents.getInstance().getInitIncidentPlacesStatement(structure, FutureHelper.handlerJsonObject(incidentPlacesFuture));
            Incidents.getInstance().getInitIncidentProtagonistTypeStatement(structure, FutureHelper.handlerJsonObject(incidentProtagonists));
            Incidents.getInstance().getInitIncidentSeriousnessStatement(structure, FutureHelper.handlerJsonObject(incidentSeriousness));
            Incidents.getInstance().getInitIncidentPartnerStatement(structure, FutureHelper.handlerJsonObject(incidentPartner));
        });
    }
}
