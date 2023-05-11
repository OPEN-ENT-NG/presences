package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.*;
import fr.openent.presences.common.incidents.*;
import fr.openent.presences.common.massmailing.*;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.InitTypeEnum;
import fr.openent.presences.helper.init.IInitPresencesHelper;
import fr.openent.presences.model.*;
import fr.openent.presences.service.InitService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.*;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.*;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class DefaultInitService implements InitService {

    private static final Logger log = LoggerFactory.getLogger(DefaultInitService.class);

    public DefaultInitService() {

    }

    @Override
    public Future<JsonObject> initPresences(HttpServerRequest request, String structureId, String userId,
                                            Optional<InitTypeEnum> initTypeEnum) {
        Promise<JsonObject> promise = Promise.promise();

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
                    log.error(message);
                }
            }

            Sql.getInstance().transaction(statements, SqlResult.validUniqueResultHandler(FutureHelper.handlerEitherPromise(promise)));
        });

        this.getReasonsStatement(request, structureId, initTypeEnum.get(), reasonsFuture);
        this.getActionsStatement(request, structureId, initTypeEnum.get(), actionsFuture);
        this.getSettingsStatement(structureId, initTypeEnum.get(), settingsFuture);
        this.getPresencesDisciplinesStatement(request, structureId, initTypeEnum.get(), disciplinesFuture);
        Massmailing.getInstance().getInitTemplatesStatement(request, structureId, userId, initTypeEnum.get(),
                FutureHelper.handlerEitherPromise(massmailingTemplatesFuture));
        Incidents.getInstance().getInitIncidentTypesStatement(structureId, initTypeEnum.get(),
                FutureHelper.handlerEitherPromise(incidentTypeFuture));
        Incidents.getInstance().getInitIncidentPlacesStatement(structureId, initTypeEnum.get(),
                FutureHelper.handlerEitherPromise(incidentPlacesFuture));
        Incidents.getInstance().getInitIncidentProtagonistTypeStatement(structureId, initTypeEnum.get(),
                FutureHelper.handlerEitherPromise(incidentProtagonists));
        Incidents.getInstance().getInitIncidentSeriousnessStatement(structureId, initTypeEnum.get(),
                FutureHelper.handlerEitherPromise(incidentSeriousness));
        Incidents.getInstance().getInitIncidentPartnerStatement(structureId, initTypeEnum.get(),
                FutureHelper.handlerEitherPromise(incidentPartner));
        Incidents.getInstance().getInitIncidentPunishmentTypeStatement(structureId, initTypeEnum.get(),
                FutureHelper.handlerEitherPromise(incidentPunishmentType));

        return promise.future();
    }

    @Override
    public void retrieveInitializationStatus(String structure, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT initialized FROM " + Presences.dbSchema + ".settings WHERE structure_id = ?;";
        JsonArray params = new JsonArray().add(structure);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public Future<Boolean> retrieveInitializationStatus(String structureId) {
        Promise<Boolean> promise = Promise.promise();

        this.retrieveInitializationStatus(structureId, res -> {
            if (res.isLeft()) {
                String message = String.format("[Presences@%s::retrieveInitializationStatus] Failed to retrieve " +
                                "initialization status for structure %s : %s",
                        this.getClass().getSimpleName(), structureId, res.left().getValue());
                log.error(message);
                promise.fail(res.left().getValue());
            } else {
                promise.complete(res.right().getValue().getBoolean(Field.INITIALIZED));
            }
        });
        return promise.future();
    }


    @Override
    public void getReasonsStatement(HttpServerRequest request, String structure, InitTypeEnum initTypeEnum, Promise<JsonObject> promise) {
        List<ReasonModel> reasons = IInitPresencesHelper.getDefaultInstance(initTypeEnum).getReasonsInit();

        JsonArray params = new JsonArray();
        StringBuilder query = new StringBuilder();

        reasons.forEach(reasonModel -> {
            query.append("INSERT INTO ")
                    .append(Presences.dbSchema)
                    .append(".reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id)" +
                            " VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);");
            params.add(structure)
                    .add(I18n.getInstance().translate(reasonModel.getLabel(), Renders.getHost(request), I18n.acceptLanguage(request)))
                    .add(reasonModel.isProving())
                    .add(reasonModel.isAbsenceCompliance())
                    .add(reasonModel.getReasonTypeId().getValue());
            if (reasonModel.isRegularizedAlertExclude() || reasonModel.isUnregularizedAlertExclude() || reasonModel.isLatenessAlertExclude()) {
                query.append("INSERT INTO ")
                        .append(Presences.dbSchema)
                        .append(".reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) VALUES ");
                if (reasonModel.isRegularizedAlertExclude()) {
                    query.append("(?, currval('presences.reason_id_seq'), 1),");
                    params.add(structure);
                }
                if (reasonModel.isUnregularizedAlertExclude()) {
                    query.append("(?, currval('presences.reason_id_seq'), 2),");
                    params.add(structure);
                }
                if (reasonModel.isLatenessAlertExclude()) {
                    query.append("(?, currval('presences.reason_id_seq'), 3),");
                    params.add(structure);
                }
                query.deleteCharAt(query.length() - 1);
                query.append(";");
            }
        });

        promise.complete(new JsonObject()
                .put(Field.STATEMENT, query)
                .put(Field.VALUES, params)
                .put(Field.ACTION, Field.PREPARED));
    }

    @Override
    public void getActionsStatement(HttpServerRequest request, String structure, InitTypeEnum typeEnum, Promise<JsonObject> promise) {
        List<Action> actions = IInitPresencesHelper.getDefaultInstance(typeEnum).getActionsInit();
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Presences.dbSchema + ".actions(structure_id, label, abbreviation) VALUES ";
        for (Action action : actions) {
            String label = I18n.getInstance().translate(action.getLabel(), Renders.getHost(request), I18n.acceptLanguage(request));
            String abbr = I18n.getInstance().translate(action.getAbbreviation(), Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?, ?),";
            params.add(structure)
                    .add(label)
                    .add(abbr);
        }

        query = query.substring(0, query.length() - 1) + ";";
        promise.complete(new JsonObject()
                .put(Field.STATEMENT, query)
                .put(Field.VALUES, params)
                .put(Field.ACTION, Field.PREPARED));
    }

    @Override
    public void getSettingsStatement(String structure, InitTypeEnum typeEnum, Promise<JsonObject> promise) {
        Settings settings = IInitPresencesHelper.getDefaultInstance(typeEnum).getSettingsInit();
        String query = "INSERT INTO " + Presences.dbSchema + ".settings(structure_id, alert_absence_threshold, " +
                "alert_lateness_threshold, alert_incident_threshold, alert_forgotten_notebook_threshold, initialized, allow_multiple_slots) " +
                "VALUES (?, ?, ?, ?, ?, true, true) ON CONFLICT ON CONSTRAINT settings_pkey DO UPDATE SET initialized = true," +
                " alert_absence_threshold = ?, alert_lateness_threshold = ?, alert_incident_threshold = ?," +
                " alert_forgotten_notebook_threshold = ? WHERE settings.structure_id = ? ;";
        JsonArray params = new JsonArray()
                .add(structure)
                .add(settings.alertAbsenceThreshold())
                .add(settings.alertLatenessThreshold())
                .add(settings.alertIncidentThreshold())
                .add(settings.alertForgottenThreshold())
                .add(settings.alertAbsenceThreshold())
                .add(settings.alertLatenessThreshold())
                .add(settings.alertIncidentThreshold())
                .add(settings.alertForgottenThreshold())
                .add(structure);
        promise.complete(new JsonObject()
                .put(Field.STATEMENT, query)
                .put(Field.VALUES, params)
                .put(Field.ACTION, Field.PREPARED));
    }

    @Override
    public void getPresencesDisciplinesStatement(HttpServerRequest request, String structure, InitTypeEnum typeEnum, Promise<JsonObject> promise) {
        List<Discipline> disciplines = IInitPresencesHelper.getDefaultInstance(typeEnum).getDisciplinesInit();
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Presences.dbSchema + ".discipline(structure_id, label) VALUES ";
        for (Discipline discipline : disciplines) {
            String label = I18n.getInstance().translate(discipline.getLabel(), Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?),";
            params.add(structure).add(label);
        }

        query = query.substring(0, query.length() - 1) + ";";
        promise.complete(new JsonObject()
                .put(Field.STATEMENT, query)
                .put(Field.VALUES, params)
                .put(Field.ACTION, Field.PREPARED));
    }
}
