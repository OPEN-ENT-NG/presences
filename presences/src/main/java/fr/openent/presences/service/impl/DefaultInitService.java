package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.service.InitService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.Arrays;
import java.util.List;

public class DefaultInitService implements InitService {

    @Override
    public void retrieveInitializationStatus(String structure, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT initialized FROM " + Presences.dbSchema + ".settings WHERE structure_id = ?;";
        JsonArray params = new JsonArray().add(structure);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getReasonsStatement(HttpServerRequest request, String structure, Future<JsonObject> future) {
        List<Boolean> proving = Arrays.asList(false, true, false, true, false, false, false, true, false, false);
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Presences.dbSchema + ".reason(structure_id, label, proving, comment, absence_compliance) VALUES ";
        for (int i = 0; i < proving.size(); i++) {
            String i18nKey = "presences.reasons.init." + i;
            query += "(?, ?, ?, '', true),";
            params.add(structure)
                    .add(I18n.getInstance().translate(i18nKey, Renders.getHost(request), I18n.acceptLanguage(request)))
                    .add(proving.get(i));
        }

        query = query.substring(0, query.length() - 1) + ";";
        future.complete(new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared"));
    }

    @Override
    public void getActionsStatement(HttpServerRequest request, String structure, Future<JsonObject> future) {
        Integer occurrences = 5;
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Presences.dbSchema + ".actions(structure_id, label, abbreviation) VALUES ";
        for (int i = 0; i < occurrences; i++) {
            String label = I18n.getInstance().translate("presences.actions.init." + i, Renders.getHost(request), I18n.acceptLanguage(request));
            String abbr = I18n.getInstance().translate("presences.actions.abbr.init." + i, Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?, ?),";
            params.add(structure)
                    .add(label)
                    .add(abbr);
        }

        query = query.substring(0, query.length() - 1) + ";";
        future.complete(new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared"));
    }

    @Override
    public void getSettingsStatement(String structure, Future<JsonObject> future) {
        String query = "INSERT INTO " + Presences.dbSchema + ".settings(structure_id, alert_absence_threshold, alert_lateness_threshold, alert_incident_threshold, alert_forgotten_notebook_threshold, initialized) " +
                "VALUES (?, ?, ?, ?, ?, true) ON CONFLICT ON CONSTRAINT settings_pkey DO UPDATE SET initialized = true WHERE settings.structure_id = ? ;";
        JsonArray params = new JsonArray().add(structure).add(5).add(3).add(3).add(3).add(structure);
        future.complete(new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared"));
    }

    @Override
    public void getPresencesDisciplinesStatement(HttpServerRequest request, String structure, Future<JsonObject> future) {
        Integer occurrences = 3;
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Presences.dbSchema + ".discipline(structure_id, label) VALUES ";
        for (int i = 0; i < occurrences; i++) {
            String label = I18n.getInstance().translate("presences.discipline." + i, Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?),";
            params.add(structure).add(label);
        }

        query = query.substring(0, query.length() - 1) + ";";
        future.complete(new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared"));
    }
}
