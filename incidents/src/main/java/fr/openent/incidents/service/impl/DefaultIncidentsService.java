package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.service.IncidentsService;
import fr.openent.presences.common.helper.FutureHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultIncidentsService extends SqlCrudService implements IncidentsService {
    private final static String DATABASE_TABLE = "incident";
    private EventBus eb;

    public DefaultIncidentsService(EventBus eb) {
        super(Incidents.dbSchema, DATABASE_TABLE);
        this.eb = eb;
    }

    @Override
    public void get(String structureId, String startDate, String endDate,
                    List<String> userId, String page, boolean paginationMode,
                    String order, boolean reverse, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(this.getQuery(structureId, startDate, endDate,
                userId, page, params, paginationMode, order, reverse), params, SqlResult.validResultHandler(result -> {
            if (result.isRight()) {
                JsonArray arrayIncidents = result.right().getValue();
                for (int i = 0; i < arrayIncidents.size(); i++) {
                    toFormatJson(arrayIncidents.getJsonObject(i));
                }
                getUsersInfo(structureId, arrayIncidents, handler);
            } else {
                handler.handle(new Either.Left<>(result.left().getValue()));
            }
        }));
    }

    private void toFormatJson(JsonObject incidents) {
        incidents.put("place", new JsonObject(incidents.getString("place")));
        incidents.put("partner", new JsonObject(incidents.getString("partner")));
        incidents.put("incident_type", new JsonObject(incidents.getString("incident_type")));
        incidents.put("seriousness", new JsonObject(incidents.getString("seriousness")));
        incidents.put("protagonists", new JsonArray(incidents.getString("protagonists")));
    }


    @Override
    public void getPageNumber(String structureId, String startDate, String endDate, List<String> userId,
                              String page, String order, boolean reverse, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(this.getQuery(structureId, startDate, endDate,
                userId, page, params, true, order, reverse),
                params, SqlResult.validUniqueResultHandler(handler));
    }


    private String getSqlOrderValue(String field) {
        String typeField;
        switch (field) {
            case "date":
                typeField = "i.date";
                break;
            case "place":
                typeField = "i.place_id";
                break;
            case "type":
                typeField = "i.type_id";
                break;
            case "seriousness":
                typeField = "i.seriousness_id";
                break;
            case "treated":
                typeField = "i.processed";
                break;
            default:
                typeField = "i.date";
        }
        return typeField;
    }

    private String getSqlReverseString(Boolean reverse) {
        return reverse ? "ASC" : "DESC";
    }

    /**
     * GET query to fetch incidents
     *
     * @param structureId structure identifier
     * @param startDate   start date
     * @param endDate     end date
     * @param userId      List userId []
     * @param page        page
     */
    private String getQuery(String structureId, String startDate, String endDate,
                            List<String> userId, String page, JsonArray params, boolean paginationMode, String field, boolean reverse) {
        String query = "WITH ids AS (SELECT id FROM " + Incidents.dbSchema + ".incident i ";

        if (userId != null && !userId.isEmpty()) {
            query += "INNER JOIN " + Incidents.dbSchema + ".protagonist AS userProtagonist " +
                    "ON (i.id = userProtagonist.incident_id AND userProtagonist.user_id IN " + Sql.listPrepared(userId.toArray()) + " ) ";
            params.addAll(new JsonArray(userId));
        }

        query += "WHERE i.structure_id = ? ";
        params.add(structureId);

        if (startDate != null && endDate != null) {
            query += "AND i.date >= to_date(?, 'YYYY-MM-DD') ";
            query += "AND i.date <= to_date(?, 'YYYY-MM-DD') ";
            params.add(startDate);
            params.add(endDate);
        }

        query += "ORDER BY " + getSqlOrderValue(field) + " " + getSqlReverseString(reverse);

        if (page != null && !paginationMode) {
            query += " OFFSET ? LIMIT ? ";
            params.add(Incidents.PAGE_SIZE * Integer.parseInt(page));
            params.add(Incidents.PAGE_SIZE);
        }

        if (!paginationMode) {
            query += ") SELECT i.*," +
                    "to_json(place) as place, " +
                    "to_json(partner) as partner, " +
                    "to_json(incident_type) as incident_type, " +
                    "to_json(seriousness) as seriousness, " +
                    "array_to_json(array_agg(protagonists)) as protagonists " +
                    "FROM " + Incidents.dbSchema + ".incident i " +
                    "INNER JOIN ids ON (ids.id = i.id) " +
                    "INNER JOIN " + Incidents.dbSchema + ".place AS place ON place.id = i.place_id " +
                    "INNER JOIN " + Incidents.dbSchema + ".partner AS partner ON partner.id = i.partner_id " +
                    "INNER JOIN " + Incidents.dbSchema + ".incident_type AS incident_type ON incident_type.id = i.type_id " +
                    "INNER JOIN " + Incidents.dbSchema + ".seriousness AS seriousness ON seriousness.id = i.seriousness_id " +
                    "INNER JOIN (SELECT pt.*, to_json(protagonist_type) as type FROM incidents.protagonist pt " +
                    "INNER JOIN " + Incidents.dbSchema + ".protagonist_type ON pt.type_id = protagonist_type.id) " +
                    "AS protagonists ON (i.id = protagonists.incident_id) " +

                    "GROUP BY i.id, i.date, i.description, i.processed, i.place_id, i.partner_id, " +
                    "i.type_id, i.seriousness_id, place.id, partner.id, incident_type.id, seriousness.id " +
                    "ORDER BY " + getSqlOrderValue(field) + " " + getSqlReverseString(reverse);
        } else {
            query += ") SELECT count(*) from ids";
        }

        return query;
    }


    /**
     * Get user infos using eventBus
     * @param structure_id  structure identifier
     * @param arrayIncidents incidents []
     * @param handler handler
     */
    private void getUsersInfo(String structure_id, JsonArray arrayIncidents, Handler<Either<String, JsonArray>> handler) {
        JsonArray protagonists = new JsonArray();
        for (int i = 0; i < arrayIncidents.size(); i++) {
            JsonArray protagonist = arrayIncidents.getJsonObject(i).getJsonArray("protagonists");
            for (int j = 0; j < protagonist.size(); j++) {
                if (!protagonists.contains(protagonist.getJsonObject(j).getString("user_id"))) {
                    protagonists.add(protagonist.getJsonObject(j).getString("user_id"));
                }
            }
        }

        JsonObject action = new JsonObject()
                .put("action", "eleve.getInfoEleve")
                .put("idEleves", protagonists)
                .put("idEtablissement", structure_id);

        eb.send(Incidents.ebViescoAddress, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if ("ok".equals(body.getString("status"))) {
                JsonArray protagonistResult = body.getJsonArray("results");

                for (int i = 0; i < arrayIncidents.size(); i++) {
                    JsonArray protagonist = arrayIncidents.getJsonObject(i).getJsonArray("protagonists");
                    for (int j = 0; j < protagonist.size(); j++) {
                        for (int k = 0; k < protagonistResult.size(); k++) {
                            if (protagonist.getJsonObject(j).getString("user_id").
                                    equals(protagonistResult.getJsonObject(k).getString("idEleve"))) {
                                protagonist.getJsonObject(j).put("student", protagonistResult.getJsonObject(k));
                            }
                        }
                    }
                }
                handler.handle(new Either.Right<>(arrayIncidents));
            } else {
                handler.handle(new Either.Left<>("Failed to query protagonist info"));
            }
        }));
    }

    @Override
    public void getIncidentParameter(String structureId, Handler<Either<String, JsonObject>> handler) {
        Future<JsonArray> placeTypeFuture = Future.future();
        Future<JsonArray> partnerTypeFuture = Future.future();
        Future<JsonArray> incidentTypeFuture = Future.future();
        Future<JsonArray> seriousnessLevelFuture = Future.future();
        Future<JsonArray> protagonistTypeFuture = Future.future();

        CompositeFuture.all(placeTypeFuture, partnerTypeFuture, incidentTypeFuture, seriousnessLevelFuture, protagonistTypeFuture).setHandler(event -> {
            if (event.failed()) {
                handler.handle(new Either.Left<>(event.cause().toString()));
            } else {
                JsonObject res = new JsonObject()
                        .put("place", placeTypeFuture.result())
                        .put("partner", partnerTypeFuture.result())
                        .put("incidentType", incidentTypeFuture.result())
                        .put("protagonistType", protagonistTypeFuture.result())
                        .put("seriousnessLevel", seriousnessLevelFuture.result());
                handler.handle(new Either.Right<>(res));
            }
        });
        getPlaceType(structureId, FutureHelper.handlerJsonArray(placeTypeFuture));
        getPartnerType(structureId, FutureHelper.handlerJsonArray(partnerTypeFuture));
        getIncidentType(structureId, FutureHelper.handlerJsonArray(incidentTypeFuture));
        getProtagonistType(structureId, FutureHelper.handlerJsonArray(protagonistTypeFuture));
        getSeriousnessLevel(structureId, FutureHelper.handlerJsonArray(seriousnessLevelFuture));
    }

    private void getPlaceType(String structureId, Handler<Either<String, JsonArray>> handler) {
        String placeTypeQuery = "SELECT * FROM " + Incidents.dbSchema + ".place where structure_id = '" + structureId + "'";
        Sql.getInstance().raw(placeTypeQuery, SqlResult.validResultHandler(handler));
    }

    private void getPartnerType(String structureId, Handler<Either<String, JsonArray>> handler) {
        String partnerTypeQuery = "SELECT * FROM " + Incidents.dbSchema + ".partner where " +
                "structure_id = '" + structureId + "' OR structure_id = '' ORDER BY structure_id desc";
        Sql.getInstance().raw(partnerTypeQuery, SqlResult.validResultHandler(handler));

    }

    private void getIncidentType(String structureId, Handler<Either<String, JsonArray>> handler) {
        String incidentTypeQuery = "SELECT * FROM " + Incidents.dbSchema + ".incident_type where structure_id = '" + structureId + "'";
        Sql.getInstance().raw(incidentTypeQuery, SqlResult.validResultHandler(handler));

    }

    private void getProtagonistType(String structureId, Handler<Either<String, JsonArray>> handler) {
        String incidentTypeQuery = "SELECT * FROM " + Incidents.dbSchema + ".protagonist_type where structure_id = '" + structureId + "'";
        Sql.getInstance().raw(incidentTypeQuery, SqlResult.validResultHandler(handler));

    }

    private void getSeriousnessLevel(String structureId, Handler<Either<String, JsonArray>> handler) {
        String seriousnessLevelQuery = "SELECT * FROM " + Incidents.dbSchema + ".seriousness where structure_id = '" + structureId + "'";
        Sql.getInstance().raw(seriousnessLevelQuery, SqlResult.validResultHandler(handler));
    }


    @Override
    public void create(JsonObject incident, Handler<Either<String, JsonArray>> handler) {
        String queryId = "SELECT nextval('" + Incidents.dbSchema + ".incident_id_seq') as id";

        Sql.getInstance().raw(queryId, SqlResult.validUniqueResultHandler(idEvent -> {
            if (idEvent.isLeft()) {
                handler.handle(new Either.Left<>("Failed to query next incident identifier"));
                return;
            }
            Number id = idEvent.right().getValue().getInteger("id");

            JsonArray statements = new JsonArray();
            statements.add(createIncidentStatement(id, incident));
            for (int i = 0; i < incident.getJsonArray("students").size(); i++) {
                statements.add(createOrUpdateProtagonistStatement(id,
                        incident.getJsonArray("students").getJsonObject(i).getString("user_id"),
                        incident.getJsonArray("students").getJsonObject(i).getInteger("type_id")));
            }
            Sql.getInstance().transaction(statements, SqlResult.validResultHandler(handler));
        }));

    }
    /**
     * Get statement that create Incident
     *
     * @param id       incident identifier
     * @param incident incident object
     * @return Statement
     */
    private JsonObject createIncidentStatement(Number id, JsonObject incident) {

        String query = "INSERT INTO " + Incidents.dbSchema + "." + DATABASE_TABLE +
                    " (id, owner, structure_id, date, selected_hour, description, created, processed, place_id," +
                    "partner_id, type_id, seriousness_id)" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            JsonArray values = new JsonArray()
                    .add(id)
                    .add(incident.getString("owner"))
                    .add(incident.getString("structure_id"))
                    .add(incident.getString("date"))
                    .add(incident.getBoolean("selected_hour"))
                    .add(incident.getString("description"))
                    .add(incident.getString("created"))
                    .add(incident.getBoolean("processed"))
                    .add(incident.getInteger("place_id"))
                    .add(incident.getInteger("partner_id"))
                    .add(incident.getInteger("type_id"))
                    .add(incident.getInteger("seriousness_id"));

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    @Override
    public void update(Number incidentId, JsonObject incident, Handler<Either<String, JsonObject>> handler) {

        JsonArray statements = new JsonArray();
        statements.add(updateIncidentStatement(incidentId, incident));

        String queryProtagonist = "SELECT * FROM incidents.protagonist where incident_id = " + incidentId;

        Sql.getInstance().raw(queryProtagonist, SqlResult.validResultHandler(currentBody -> {
            if (currentBody.isLeft()) {
                handler.handle(new Either.Left<>("Failed to query protagonist"));
                return;
            }

            // erase old protagonist data
            for (int i = 0; i < currentBody.right().getValue().size(); i++) {
                statements.add(deleteProtagonistStatement(
                        currentBody.right().getValue().getJsonObject(i).getString("user_id"), incidentId));
            }

            // create or update new protagonist data from new body received
            for (int i = 0; i < incident.getJsonArray("students").size(); i++) {
                statements.add(createOrUpdateProtagonistStatement(incidentId,
                        incident.getJsonArray("students").getJsonObject(i).getString("user_id"),
                        incident.getJsonArray("students").getJsonObject(i).getInteger("type_id")));
            }

            Sql.getInstance().transaction(statements, SqlResult.validUniqueResultHandler(handler));
        }));

    }

    /**
     * statement that update Incident
     *
     * @param id       incident identifier
     * @param incident incident object
     * @return Statement
     */
    private JsonObject updateIncidentStatement(Number id, JsonObject incident) {

        String query = "UPDATE " + Incidents.dbSchema + ".incident SET " +
                "owner = ?, structure_id = ?, date = ?, selected_hour = ?, description = ?, created = ?," +
                " processed = ?, place_id = ?, partner_id = ?, type_id = ?, seriousness_id = ?" +
                " WHERE id = ?";
        JsonArray values = new JsonArray()
                .add(incident.getString("owner"))
                .add(incident.getString("structure_id"))
                .add(incident.getString("date"))
                .add(incident.getBoolean("selected_hour"))
                .add(incident.getString("description"))
                .add(incident.getString("created"))
                .add(incident.getBoolean("processed"))
                .add(incident.getInteger("place_id"))
                .add(incident.getInteger("partner_id"))
                .add(incident.getInteger("type_id"))
                .add(incident.getInteger("seriousness_id"))
                .add(id);

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    /**
     * Get statement that create Protagonist
     * If protagonist already exist, it updates its incident_type
     *
     * @param incidentId   incident identifier
     * @param userId       user identifier
     * @param incidentType incident type
     * @return Statement
     */
    private JsonObject createOrUpdateProtagonistStatement(Number incidentId, String userId, Number incidentType) {

        String query = "INSERT INTO " + Incidents.dbSchema + ".protagonist" +
                " (user_id, incident_id, type_id) VALUES (?, ?, ?)" +
                "ON CONFLICT (user_id, incident_id) " +
                "DO UPDATE SET type_id = ?";
        JsonArray values = new JsonArray()
                .add(userId)
                .add(incidentId)
                .add(incidentType)
                .add(incidentType);

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    /**
     * statement that delete Protagonist
     *
     * @param userId       user identifier
     * @param incidentId   incident identifier
     * @return Statement
     */
    private JsonObject deleteProtagonistStatement(String userId, Number incidentId) {
        String query = "DELETE FROM " + Incidents.dbSchema + ".protagonist WHERE user_id = ? AND incident_id = ?";
        JsonArray values = new JsonArray().add(userId).add(incidentId);
        return new JsonObject().put("statement", query).put("values", values).put("action", "prepared");
    }

    @Override
    public void delete(String incident_id, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Incidents.dbSchema + "." + DATABASE_TABLE + " WHERE id = "
                + Integer.parseInt(incident_id);
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

}
