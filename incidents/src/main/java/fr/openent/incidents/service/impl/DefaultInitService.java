package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.service.InitService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.request.JsonHttpServerRequest;

import java.util.Arrays;
import java.util.List;

public class DefaultInitService implements InitService {
    @Override
    public void getInitIncidentTypesStatement(JsonHttpServerRequest request, String structure, Handler<Either<String, JsonObject>> handler) {
        Integer occurrences = 6;
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Incidents.dbSchema + ".incident_type(structure_id, label) VALUES ";
        for (int i = 0; i < occurrences; i++) {
            String i18nLabel = I18n.getInstance().translate("incidents.init.incident.type." + i, Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?),";
            params.add(structure)
                    .add(i18nLabel);
        }

        query = query.substring(0, query.length() - 1) + ";";
        handler.handle(new Either.Right<>(new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared")));
    }

    @Override
    public void getInitIncidentPlacesStatement(JsonHttpServerRequest request, String structure, Handler<Either<String, JsonObject>> handler) {
        Integer occurrences = 6;
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Incidents.dbSchema + ".place(structure_id, label) VALUES ";
        for (int i = 0; i < occurrences; i++) {
            String i18nLabel = I18n.getInstance().translate("incidents.init.incident.place." + i, Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?),";
            params.add(structure)
                    .add(i18nLabel);
        }

        query = query.substring(0, query.length() - 1) + ";";
        handler.handle(new Either.Right<>(new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared")));
    }

    @Override
    public void getInitIncidentProtagonistsStatement(JsonHttpServerRequest request, String structure, Handler<Either<String, JsonObject>> handler) {
        Integer occurrences = 4;
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Incidents.dbSchema + ".protagonist_type(structure_id, label) VALUES ";
        for (int i = 0; i < occurrences; i++) {
            String i18nLabel = I18n.getInstance().translate("incidents.init.incident.protagonist.type." + i, Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?),";
            params.add(structure)
                    .add(i18nLabel);
        }

        query = query.substring(0, query.length() - 1) + ";";
        handler.handle(new Either.Right<>(new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared")));
    }

    @Override
    public void getInitIncidentSeriousnessStatement(JsonHttpServerRequest request, String structure, Handler<Either<String, JsonObject>> handler) {
        List<Integer> seriousness = Arrays.asList(1, 3, 5, 6, 9);
        JsonArray params = new JsonArray();
        String query = "INSERT INTO incidents.seriousness(structure_id, label, level) VALUES ";
        for (int i = 0; i < seriousness.size(); i++) {
            String i18nLabel = I18n.getInstance().translate("incident.init.incident.seriousness." + i, Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?, ?),";
            params.add(structure)
                    .add(i18nLabel)
                    .add(seriousness.get(i));
        }

        query = query.substring(0, query.length() - 1) + ";";
        handler.handle(new Either.Right<>(new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared")));
    }

    @Override
    public void getInitIncidentPartnerStatement(JsonHttpServerRequest request, String structure, Handler<Either<String, JsonObject>> handler) {
        Integer occurrences = 4;
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Incidents.dbSchema + ".partner(structure_id, label) VALUES ";
        for (int i = 0; i < occurrences; i++) {
            String i18nLabel = I18n.getInstance().translate("incident.init.incident.partner." + i, Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?),";
            params.add(structure)
                    .add(i18nLabel);
        }

        query = query.substring(0, query.length() - 1) + ";";
        handler.handle(new Either.Right<>(new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared")));
    }
}
