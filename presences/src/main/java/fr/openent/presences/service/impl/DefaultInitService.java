package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.InitTypeEnum;
import fr.openent.presences.helper.init.IInitPresencesHelper;
import fr.openent.presences.model.Action;
import fr.openent.presences.model.Discipline;
import fr.openent.presences.model.Reason;
import fr.openent.presences.model.Settings;
import fr.openent.presences.service.InitService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

public class DefaultInitService implements InitService {

    @Override
    public void retrieveInitializationStatus(String structure, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT initialized FROM " + Presences.dbSchema + ".settings WHERE structure_id = ?;";
        JsonArray params = new JsonArray().add(structure);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getReasonsStatement(HttpServerRequest request, String structure, InitTypeEnum initTypeEnum, Promise<JsonObject> promise) {
        List<Reason> reasons = IInitPresencesHelper.getInstance(initTypeEnum).getReasonsInit();

        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Presences.dbSchema + ".reason(structure_id, label, proving, comment, absence_compliance, reason_type_id) VALUES ";
        for (Reason reason : reasons) {
            query += "(?, ?, ?, '', ?, ?),";
            params.add(structure)
                    .add(I18n.getInstance().translate(reason.getLabel(), Renders.getHost(request), I18n.acceptLanguage(request)))
                    .add(reason.isProving())
                    .add(reason.isAbsenceCompliance())
                    .add(reason.getReasonTypeId());
        }

        query = query.substring(0, query.length() - 1) + ";";
        promise.complete(new JsonObject()
                .put(Field.STATEMENT, query)
                .put(Field.VALUES, params)
                .put(Field.ACTION, Field.PREPARED));
    }

    @Override
    public void getActionsStatement(HttpServerRequest request, String structure, InitTypeEnum typeEnum, Promise<JsonObject> promise) {
        List<Action> actions = IInitPresencesHelper.getInstance(typeEnum).getActionsInit();
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
        Settings settings = IInitPresencesHelper.getInstance(typeEnum).getSettingsInit();
        String query = "INSERT INTO " + Presences.dbSchema + ".settings(structure_id, alert_absence_threshold, " +
                "alert_lateness_threshold, alert_incident_threshold, alert_forgotten_notebook_threshold, initialized, allow_multiple_slots) " +
                "VALUES (?, ?, ?, ?, ?, true, true) ON CONFLICT ON CONSTRAINT settings_pkey DO UPDATE SET initialized = true WHERE settings.structure_id = ? ;";
        JsonArray params = new JsonArray().add(structure).add(settings.alertAbsenceThreshold()).add(settings.alertLatenessThreshold())
                .add(settings.alertIncidentThreshold()).add(settings.alertForgottenThreshold()).add(structure);
        promise.complete(new JsonObject()
                .put(Field.STATEMENT, query)
                .put(Field.VALUES, params)
                .put(Field.ACTION, Field.PREPARED));
    }

    @Override
    public void getPresencesDisciplinesStatement(HttpServerRequest request, String structure, InitTypeEnum typeEnum, Promise<JsonObject> promise) {
        List<Discipline> disciplines = IInitPresencesHelper.getInstance(typeEnum).getDisciplinesInit();
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
