package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.service.IncidentsService;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultIncidentsService extends SqlCrudService implements IncidentsService {
    private static final Logger log = LoggerFactory.getLogger(DefaultIncidentsService.class);

    private final static String DATABASE_TABLE = "incident";

    private final UserService userService;

    public DefaultIncidentsService(EventBus eb) {
        super(Incidents.dbSchema, DATABASE_TABLE);
        this.userService = new DefaultUserService();
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


                Future<JsonArray> protagonistsFuture = Future.future();
                Future<JsonArray> ownersFuture = Future.future();

                setProtagonists(arrayIncidents, protagonistsFuture);
                setOwners(arrayIncidents, ownersFuture);

                CompositeFuture.all(protagonistsFuture, ownersFuture).setHandler(resultUsers -> {
                    if (resultUsers.failed()) {
                        handler.handle(new Either.Left<>(resultUsers.cause().getMessage()));
                        return;
                    }
                    handler.handle(new Either.Right<>(arrayIncidents));
                });
            } else {
                handler.handle(new Either.Left<>(result.left().getValue()));
            }
        }));
    }

    @Override
    public void get(String structureId, String startDate, String endDate, String userId, String limit, String offset,
                    Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT i.id, i.owner, i.structure_id, i.date, i.selected_hour, i.description, i.created, i.processed, i.place_id, " +
                " i.partner_id, i.type_id, i.seriousness_id, p.type_id as protagonist_type_id " +
                "FROM " + Incidents.dbSchema + ".incident i " +
                "INNER JOIN " + Incidents.dbSchema + ".protagonist p ON (i.id = p.incident_id) " +
                "WHERE p.user_id = ? " +
                "AND i.structure_id = ? " +
                "AND i.date >= to_date(?, 'YYYY-MM-DD') " +
                "AND i.date <= to_date(?, 'YYYY-MM-DD')" +
                "ORDER BY i.date DESC ";

        JsonArray params = new JsonArray()
                .add(userId)
                .add(structureId)
                .add(startDate)
                .add(endDate);

        if (limit != null) {
            query += " LIMIT ? ";
            params.add(limit);
        }

        if (offset != null) {
            query += " OFFSET ? ";
            params.add(offset);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void get(String structureId, String startDate, String endDate, String userId, Handler<Either<String, JsonArray>> handler) {
        get(structureId, startDate, endDate, userId, null, null, handler);
    }

    @Override
    public void get(String startDate, String endDate, List<String> users, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT incident.*, place.label as place, " +
                "incident_type.label as incident_type, protagonist.user_id as student_id, " +
                "protagonist_type.label as protagonist_type " +
                "FROM " + Incidents.dbSchema + ".incident " +
                "INNER JOIN " + Incidents.dbSchema + ".incident_type ON (incident.type_id = incident_type.id) " +
                "INNER JOIN " + Incidents.dbSchema + ".protagonist ON (incident.id = protagonist.incident_id) " +
                "INNER JOIN " + Incidents.dbSchema + ".place ON (incident.place_id = place.id) " +
                "INNER JOIN " + Incidents.dbSchema + ".protagonist_type ON (protagonist.type_id = protagonist_type.id)" +
                "WHERE protagonist.user_id IN " + Sql.listPrepared(users) +
                " AND incident.date >= to_date(?, 'YYYY-MM-DD') " +
                "AND incident.date <= to_date(?, 'YYYY-MM-DD')";
        JsonArray params = new JsonArray()
                .addAll(new JsonArray(users))
                .add(startDate)
                .add(endDate);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public Future<JsonArray> get(String structureId, String startDate, String endDate, List<String> studentIds, String limit, String offset) {
        Promise<JsonArray> promise = Promise.promise();

        JsonArray params = new JsonArray();
        String query = "SELECT incident.*, place.label as place, " +
                "incident_type.label as incident_type, protagonist.user_id as student_id, " +
                "protagonist_type.label as protagonist_type, protagonist_type.id as protagonist_type_id" +
                getFromWhereQuery(params, structureId, startDate, endDate, studentIds) +
                " ORDER BY incident.date DESC ";
        if (limit != null) {
            query += " LIMIT ? ";
            params.add(limit);
        }

        if (offset != null) {
            query += " OFFSET ? ";
            params.add(offset);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(promise)));

        return promise.future();
    }

    @Override
    public Future<JsonArray> countByStudents(String structureId, String startDate, String endDate, List<String> studentIds) {
        Promise<JsonArray> promise = Promise.promise();

        JsonArray params = new JsonArray();
        String query = "SELECT protagonist.user_id as student_id, count(*) " +
                getFromWhereQuery(params, structureId, startDate, endDate, studentIds)
                + " GROUP BY protagonist.user_id ";

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(promise)));

        return promise.future();
    }

    private String getFromWhereQuery(JsonArray params, String structureId, String startDate, String endDate, List<String> studentIds) {
        String query = " FROM " + Incidents.dbSchema + ".incident " +
                "INNER JOIN " + Incidents.dbSchema + ".incident_type ON (incident.type_id = incident_type.id) " +
                "INNER JOIN " + Incidents.dbSchema + ".protagonist ON (incident.id = protagonist.incident_id) " +
                "INNER JOIN " + Incidents.dbSchema + ".place ON (incident.place_id = place.id) " +
                "INNER JOIN " + Incidents.dbSchema + ".protagonist_type ON (protagonist.type_id = protagonist_type.id)" +
                "WHERE protagonist.user_id IN " + Sql.listPrepared(studentIds) +
                " AND incident.date >= to_date(?, 'YYYY-MM-DD') " +
                "AND incident.date <= to_date(?, 'YYYY-MM-DD') ";

        params
                .addAll(new JsonArray(studentIds))
                .add(startDate)
                .add(endDate);

        if (structureId != null) {
            query += "AND incident.structure_id = ? ";
            params.add(structureId);
        }

        return query;
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
     * @param structureId    structure identifier
     * @param startDate      start date
     * @param endDate        end date
     * @param userId         List userId []
     * @param page           page
     * @param params         Json parameters
     * @param paginationMode pagination mode
     * @param field          field order to sort
     * @param reverse        reverse data sorting
     */
    private String getQuery(String structureId, String startDate, String endDate,
                            List<String> userId, String page, JsonArray params, boolean paginationMode, String field, boolean reverse) {

        String query = setIncidentIdsCTE(params, structureId, startDate, endDate, field, reverse) + ", " + setProtagonistsCTE();

        // Retrieve number of incidents if in pagination mode
        if (paginationMode) {
            query += "SELECT COUNT(*) ";
        }
        // Retrieve incidents
        else {
            query += "SELECT i.*," +
                    "to_json(place) as place, " +
                    "to_json(partner) as partner, " +
                    "to_json(incident_type) as incident_type, " +
                    "to_json(seriousness) as seriousness, " +
                    "array_to_json(array_agg(protagonists)) as protagonists ";
        }

        query += "FROM " + Incidents.dbSchema + ".incident i ";

        //Join results with ids query result
        query = getJoinIncidents(userId, params, query);

        //For incidents results, order results and limit size according to the page
        if (!paginationMode) {
            query += "GROUP BY i.id, i.date, i.description, i.processed, i.place_id, i.partner_id, " +
                    "i.type_id, i.seriousness_id, place.id, partner.id, incident_type.id, seriousness.id " +
                    "ORDER BY " + getSqlOrderValue(field) + " " + getSqlReverseString(reverse);

            if (page != null) {
                query += " OFFSET ? LIMIT ? ";
                params.add(Incidents.PAGE_SIZE * Integer.parseInt(page));
                params.add(Incidents.PAGE_SIZE);
            }
        }

        return query;
    }

    private String setIncidentIdsCTE(JsonArray params, String structureId, String startDate, String endDate, String field, boolean reverse) {
        String query = "WITH ids AS (SELECT id FROM " + Incidents.dbSchema + ".incident i "
                + " WHERE i.structure_id = ? ";

        params.add(structureId);

        if (startDate != null && endDate != null) {
            query += " AND i.date BETWEEN ? AND ? ";
            params.add(startDate);
            params.add(endDate);
        }
        return query + " ORDER BY " + getSqlOrderValue(field) + " " + getSqlReverseString(reverse) + ") ";
    }

    private String setProtagonistsCTE() {
        return " protagonists AS ( " +
                " SELECT pt.*, to_json(protagonist_type) as type " +
                " FROM incidents.protagonist pt " +
                " INNER JOIN incidents.protagonist_type ON pt.type_id = protagonist_type.id " +
                " )";
    }

    private String getJoinIncidents(List<String> userId, JsonArray params, String query) {
        query += "INNER JOIN ids ON (ids.id = i.id) " +
                "INNER JOIN " + Incidents.dbSchema + ".place AS place ON place.id = i.place_id " +
                "INNER JOIN " + Incidents.dbSchema + ".partner AS partner ON partner.id = i.partner_id " +
                "INNER JOIN " + Incidents.dbSchema + ".incident_type AS incident_type ON incident_type.id = i.type_id " +
                "INNER JOIN " + Incidents.dbSchema + ".seriousness AS seriousness ON seriousness.id = i.seriousness_id " +
                "INNER JOIN protagonists ON (i.id = protagonists.incident_id) ";

        if (userId != null && !userId.isEmpty()) {
            query += "WHERE EXISTS(SELECT * FROM protagonists WHERE user_id IN " + Sql.listPrepared(userId.toArray()) +
                    " AND i.id = protagonists.incident_id)";
            params.addAll(new JsonArray(userId));
        }

        return query;
    }


    /**
     * Get user infos from neo4j
     *
     * @param arrayIncidents incidents []
     * @param future         future
     */
    private void setProtagonists(JsonArray arrayIncidents, Future<JsonArray> future) {
        JsonArray protagonists = new JsonArray();
        for (int i = 0; i < arrayIncidents.size(); i++) {
            JsonArray protagonist = arrayIncidents.getJsonObject(i).getJsonArray("protagonists");
            for (int j = 0; j < protagonist.size(); j++) {
                if (!protagonists.contains(protagonist.getJsonObject(j).getString("user_id"))) {
                    protagonists.add(protagonist.getJsonObject(j).getString("user_id"));
                }
            }
        }

        String query = "MATCH (u:User) WHERE u.id IN {idStudents} " +
                "RETURN (u.lastName + ' ' + u.firstName) as displayName, u.id as idEleve";
        JsonObject params = new JsonObject().put("idStudents", protagonists);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(result -> {
            if (result.isRight()) {
                JsonArray protagonistResult = result.right().getValue();

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
                future.complete(arrayIncidents);
            } else {
                future.fail("Failed to query protagonist info");
            }
        }));
    }

    private void setOwners(JsonArray arrayIncidents, Future<JsonArray> future) {
        List<String> ownerIds = ((List<JsonObject>) arrayIncidents.getList())
                .stream()
                .map(res -> res.getString("owner"))
                .collect(Collectors.toList());

        userService.getUsers(ownerIds, resUsers -> {
            if (resUsers.isLeft()) {
                String message = "[Incidents@DefaultIncidentsService::setOwner] Failed to get Owners";
                log.error(message);
                future.fail(message);
                return;
            }

            Map<String, JsonObject> ownerMap = new HashMap<>();
            resUsers.right().getValue().forEach(oStudent -> {
                JsonObject owner = (JsonObject) oStudent;
                ownerMap.put(owner.getString("id"), owner);
            });

            arrayIncidents.forEach(oRes -> {
                JsonObject res = (JsonObject) oRes;
                res.put("owner", ownerMap.get(res.getString("owner")));
            });
            future.complete(arrayIncidents);
        });
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
        JsonArray params = new JsonArray().add(structureId);
        String placeTypeQuery = "SELECT * FROM " + Incidents.dbSchema + ".place WHERE structure_id = ?";
        Sql.getInstance().prepared(placeTypeQuery, params, SqlResult.validResultHandler(handler));
    }

    private void getPartnerType(String structureId, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray().add(structureId);
        String partnerTypeQuery = "SELECT * FROM " + Incidents.dbSchema + ".partner WHERE " +
                "structure_id = ? OR structure_id = '' ORDER BY structure_id DESC";
        Sql.getInstance().prepared(partnerTypeQuery, params, SqlResult.validResultHandler(handler));

    }

    private void getIncidentType(String structureId, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray().add(structureId);
        String incidentTypeQuery = "SELECT * FROM " + Incidents.dbSchema + ".incident_type WHERE structure_id = ?";
        Sql.getInstance().prepared(incidentTypeQuery, params, SqlResult.validResultHandler(handler));

    }

    private void getProtagonistType(String structureId, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray().add(structureId);
        String protagonistTypeQuery = "SELECT * FROM " + Incidents.dbSchema + ".protagonist_type WHERE structure_id = ?";
        Sql.getInstance().prepared(protagonistTypeQuery, params, SqlResult.validResultHandler(handler));

    }

    private void getSeriousnessLevel(String structureId, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray().add(structureId);
        String seriousnessLevelQuery = "SELECT * FROM " + Incidents.dbSchema + ".seriousness WHERE structure_id = ?";
        Sql.getInstance().prepared(seriousnessLevelQuery, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject incident, Handler<Either<String, JsonArray>> handler) {
        String queryId = "SELECT nextval('" + Incidents.dbSchema + ".incident_id_seq') as id";

        Sql.getInstance().raw(queryId, SqlResult.validUniqueResultHandler(idEvent -> {
            if (idEvent.isLeft()) {
                String message = String.format("[Incidents@%s::create] Failed to query next incident identifier : %s",
                        this.getClass().getSimpleName(), idEvent.left().getValue());
                log.error(message);
                handler.handle(new Either.Left<>(message));
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
                .add(incident.getJsonObject("owner").getString("id"))
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
                .add(incident.getJsonObject("owner").getString("id"))
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
     * @param userId     user identifier
     * @param incidentId incident identifier
     * @return Statement
     */
    private JsonObject deleteProtagonistStatement(String userId, Number incidentId) {
        String query = "DELETE FROM " + Incidents.dbSchema + ".protagonist WHERE user_id = ? AND incident_id = ?";
        JsonArray values = new JsonArray().add(userId).add(incidentId);
        return new JsonObject().put("statement", query).put("values", values).put("action", "prepared");
    }

    @Override
    public void delete(String incident_id, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT " + Incidents.dbSchema + ".delete_incident(" + Integer.parseInt(incident_id) + ");";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

}
