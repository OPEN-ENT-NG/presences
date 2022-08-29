package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.helper.init.IInitIncidentsHelper;
import fr.openent.incidents.model.*;
import fr.openent.incidents.service.InitService;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.InitTypeEnum;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.request.JsonHttpServerRequest;

import java.util.ArrayList;
import java.util.List;

public class DefaultInitService implements InitService {
    @Override
    public void getInitIncidentTypesStatement(JsonHttpServerRequest request, String structure, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Incidents.dbSchema + ".incident_type(structure_id, label) VALUES ";
        List<IncidentType> incidentTypes = IInitIncidentsHelper.getInstance(initTypeEnum).getIncidentTypes();
        for (IncidentType incidentType : incidentTypes) {
            String i18nLabel = I18n.getInstance().translate(incidentType.getLabel(), Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?),";
            params.add(structure)
                    .add(i18nLabel);
        }

        query = query.substring(0, query.length() - 1) + ";";
        handler.handle(new Either.Right<>(new JsonObject()
                .put(Field.STATEMENT, query)
                .put(Field.VALUES, params)
                .put(Field.ACTION, Field.PREPARED)));
    }

    @Override
    public void getInitIncidentPlacesStatement(JsonHttpServerRequest request, String structure, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Incidents.dbSchema + ".place(structure_id, label) VALUES ";

        List<Place> incidentPlaces = IInitIncidentsHelper.getInstance(initTypeEnum).getPlaces();
        for (Place place : incidentPlaces) {
            String i18nLabel = I18n.getInstance().translate(place.getLabel(), Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?),";
            params.add(structure)
                    .add(i18nLabel);
        }

        query = query.substring(0, query.length() - 1) + ";";
        handler.handle(new Either.Right<>(new JsonObject()
                .put(Field.STATEMENT, query)
                .put(Field.VALUES, params)
                .put(Field.ACTION, Field.PREPARED)));
    }

    @Override
    public void getInitIncidentProtagonistsStatement(JsonHttpServerRequest request, String structure, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Incidents.dbSchema + ".protagonist_type(structure_id, label) VALUES ";

        List<ProtagonistType> protagonistTypeList = IInitIncidentsHelper.getInstance(initTypeEnum).getProtagonistTypes();
        for (ProtagonistType protagonistType : protagonistTypeList) {
            String i18nLabel = I18n.getInstance().translate(protagonistType.getLabel(), Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?),";
            params.add(structure)
                    .add(i18nLabel);
        }

        query = query.substring(0, query.length() - 1) + ";";
        handler.handle(new Either.Right<>(new JsonObject()
                .put(Field.STATEMENT, query)
                .put(Field.VALUES, params)
                .put(Field.ACTION, Field.PREPARED)));
    }

    @Override
    public void getInitIncidentSeriousnessStatement(JsonHttpServerRequest request, String structure, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "INSERT INTO incidents.seriousness(structure_id, label, level) VALUES ";

        List<Seriousness> seriousnessList = IInitIncidentsHelper.getInstance(initTypeEnum).getSeriousnessTypes();
        for (Seriousness seriousness : seriousnessList) {
            String i18nLabel = I18n.getInstance().translate(seriousness.getLabel(), Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?, ?),";
            params.add(structure)
                    .add(i18nLabel)
                    .add(seriousness.getLevel());
        }

        query = query.substring(0, query.length() - 1) + ";";
        handler.handle(new Either.Right<>(new JsonObject()
                .put(Field.STATEMENT, query)
                .put(Field.VALUES, params)
                .put(Field.ACTION, Field.PREPARED)));
    }

    @Override
    public void getInitIncidentPartnerStatement(JsonHttpServerRequest request, String structure, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Incidents.dbSchema + ".partner(structure_id, label) VALUES ";

        List<Partner> partnerList = IInitIncidentsHelper.getInstance(initTypeEnum).getPartners();
        for (Partner partner : partnerList) {
            String i18nLabel = I18n.getInstance().translate(partner.getLabel(), Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?),";
            params.add(structure)
                    .add(i18nLabel);
        }

        query = query.substring(0, query.length() - 1) + ";";
        handler.handle(new Either.Right<>(new JsonObject()
                .put(Field.STATEMENT, query)
                .put(Field.VALUES, params)
                .put(Field.ACTION, Field.PREPARED)));
    }

    @Override
    public void getInitIncidentPunishmentType(JsonHttpServerRequest request, String structure, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Incidents.dbSchema + ".punishment_type(structure_id, label, type, punishment_category_id) VALUES ";
        List<PunishmentType> punishmentTypeList = IInitIncidentsHelper.getInstance(initTypeEnum).getPunishmentTypes();
        for (PunishmentType punishmentType : punishmentTypeList) {
            String i18nLabel = I18n.getInstance().translate(punishmentType.getLabel(), Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?, ?, ?),";
            params.add(structure)
                    .add(i18nLabel)
                    .add(punishmentType.getType())
                    .add(punishmentType.getPunishmentCategoryId());
        }
        query = query.substring(0, query.length() - 1) + ";";

        handler.handle(new Either.Right<>(new JsonObject()
                .put(Field.STATEMENT, query)
                .put(Field.VALUES, params)
                .put(Field.ACTION, Field.PREPARED)));
    }
}
