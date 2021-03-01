package fr.openent.massmailing.service.impl;

import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.MassmailingType;
import fr.openent.massmailing.service.MassmailingService;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.incidents.Incidents;
import fr.openent.presences.common.presences.Presences;
import fr.openent.presences.enums.EventType;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.util.ArrayList;
import java.util.List;

public class DefaultMassmailingService implements MassmailingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMassmailingService.class);

    private final EventBus eb;


    public DefaultMassmailingService(EventBus eb) {
        this.eb = eb;
    }

    @Override
    public void getStatus(String structure, MassmailingType type, Boolean massmailed, List<Integer> reasons,
                          List<Integer> punishmentsTypes, List<Integer> sanctionsTypes, Integer startAt, String startDate,
                          String endDate, List<String> students, boolean noReasons,
                          Handler<Either<String, JsonObject>> handler) {
        Handler<Either<String, JsonArray>> callback = event -> {
            if (event.isLeft()) {
                String message = "[Massmailing@DefaultMassmailingService] Failed to retrieve massmailing count events";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
                return;
            }

            int count = 0;
            JsonArray res = event.right().getValue();
            for (int i = 0; i < res.size(); i++) {
                JsonObject student = res.getJsonObject(i);
                count += student.getInteger("count");
            }

            handler.handle(new Either.Right<>(new JsonObject().put("status", count)));
        };

        getCountEventByStudent(structure, type, massmailed, reasons, punishmentsTypes, sanctionsTypes, startAt, startDate, endDate, students, noReasons, callback);
    }

    @Override
    public void getCountEventByStudent(String structure, MassmailingType type, Boolean massmailed, List<Integer> reasons,
                                       List<Integer> punishmentsTypes, List<Integer> sanctionsTypes,
                                       Integer startAt, String startDate, String endDate, List<String> students,
                                       boolean noReasons, Handler<Either<String, JsonArray>> handler) {
        switch (type) {
            case REGULARIZED:
                Presences.getInstance().getCountEventByStudent(EventType.ABSENCE.getType(), students, structure, null, startAt,
                        reasons, massmailed, startDate, endDate, false, true, handler);
                break;
            case UNREGULARIZED:
                Presences.getInstance().getCountEventByStudent(EventType.ABSENCE.getType(), students, structure, null, startAt,
                        reasons, massmailed, startDate, endDate, false, false, handler);
                break;
            case NO_REASON:
                Presences.getInstance().getCountEventByStudent(EventType.ABSENCE.getType(), students, structure, null, startAt,
                        new ArrayList<>(), massmailed, startDate, endDate, true, null, handler);
                break;
            case LATENESS:
                Presences.getInstance().getCountEventByStudent(EventType.LATENESS.getType(), students, structure, null, startAt,
                        new ArrayList<>(), massmailed, startDate, endDate, noReasons, "HOUR", null, handler);
                break;
            case PUNISHMENT:
                Incidents.getInstance().getPunishmentsCountByStudent(structure, startDate + " 00:00:00", endDate + " 23:59:59",
                        students, punishmentsTypes, null, massmailed, handler);
                break;
            case SANCTION:
                Incidents.getInstance().getPunishmentsCountByStudent(structure, startDate + " 00:00:00", endDate + " 23:59:59",
                        students, sanctionsTypes, null, massmailed, handler);
                break;
            default:
                handler.handle(new Either.Left<>("[Massmailing@DefaultMassmailingService] Unknown Massmailing type"));
        }
    }

    @Override
    public void getAnomalies(MailingType type, List<String> students, Handler<Either<String, JsonArray>> handler) {
        String query;
        JsonObject params;
        switch (type) {
            case MAIL:
                query = "MATCH (u:User)-[:RELATED]->(r:User) " +
                        "WHERE u.id IN {users} " +
                        "WITH u, collect(r.email) as emails " +
                        "WHERE size(coalesce(emails)) = 0 " +
                        "RETURN DISTINCT u.id as id, (u.lastName + ' ' + u.firstName) as displayName, split(u.classes[0],'$')[1] as className";
                params = new JsonObject()
                        .put("users", new JsonArray(students));
                Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
                break;
            case SMS:
                query = "MATCH (u:User)-[:RELATED]->(r:User) " +
                        "WHERE u.id IN {users} " +
                        "WITH u, collect(r.mobile) as mobiles " +
                        "WHERE size(coalesce(mobiles)) = 0 OR ALL(x IN mobiles WHERE trim(x) = '')" +
                        "RETURN DISTINCT u.id as id, (u.lastName + ' ' + u.firstName) as displayName, split(u.classes[0],'$')[1] as className";
                params = new JsonObject()
                        .put("users", new JsonArray(students));
                Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
                break;
            case PDF:
                handler.handle(new Either.Right<>(new JsonArray()));
                break;
            default:
                handler.handle(new Either.Left<>("[Massmailing@DefaultMassmailingService] Unknown Mailing type"));
        }
    }

    @Override
    public void getRelatives(MailingType type, List<String> students, Handler<Either<String, JsonArray>> handler) {

        Future<JsonArray> relativesFuture = Future.future();
        Future<JsonArray> relativesIdsFuture = Future.future();

        CompositeFuture.all(relativesFuture, relativesIdsFuture).setHandler(asyncHandler -> {

            if (asyncHandler.failed()) {
                handler.handle(new Either.Left<>(asyncHandler.cause().toString()));
                return;
            }

            JsonArray studentsRelatives = relativesFuture.result();
            JsonArray studentPrimaryRelatives = relativesIdsFuture.result();

            for (int i = 0; i < studentsRelatives.size(); i++) {
                JsonObject student = studentsRelatives.getJsonObject(i);
                JsonArray relatives = student.getJsonArray("relative");
                JsonArray primaryRelativesIds = getRelativeIdsFromList(student.getString("id"), studentPrimaryRelatives);

                if (relatives != null) {

                    for (int j = 0; j < relatives.size(); j++) {
                        JsonObject relative = relatives.getJsonObject(j);
                        boolean primary = false;

                        for (int k = 0; k < primaryRelativesIds.size(); k++) {
                            if (primaryRelativesIds.getString(k).equals(relative.getString("id"))) {
                                primary = true;
                                break;
                            }
                        }

                        relative.put("primary", primary);
                    }
                }
            }
            handler.handle(new Either.Right<>(studentsRelatives));
        });

        getStudentRelatives(type, students, FutureHelper.handlerJsonArray(relativesFuture));
        getStudentsPrimaryRelativesIds(students, FutureHelper.handlerJsonArray(relativesIdsFuture));
    }


    private void getStudentRelatives(MailingType type, List<String> students, Handler<Either<String, JsonArray>> handler) {
        String contactValue;
        switch (type) {
            case MAIL:
            case PDF:
                contactValue = "r.email";
                break;
            case SMS:
                contactValue = "CASE WHEN r.mobilePhone is null THEN r.mobile ELSE r.mobilePhone[0] END, address: r.address";
                break;
            default:
                contactValue = "";
        }

        String query = "MATCH (u:User)-[:RELATED]->(r:User) WHERE u.id IN {students} RETURN u.id as id, " +
                "(u.lastName + ' ' + u.firstName) AS displayName, split(u.classes[0],'$')[1] AS className, " +
                "collect({id: r.id, displayName: (r.lastName + ' ' + r.firstName), " +
                "contact: " + contactValue + "}) AS relative";
        JsonObject params = new JsonObject()
                .put("students", new JsonArray(students));

        Neo4j.getInstance().execute(query, params, res -> {
            Either<String, JsonArray> resHandler = Neo4jResult.validResult(res);

            if (resHandler.isLeft()) {
                handler.handle(new Either.Left<>("[Massmailing@DefaultMassmailingService::getStudentRelatives] Error fetching " +
                        "student relatives"));
            } else {
                handler.handle(new Either.Right<>(resHandler.right().getValue()));
            }
        });
    }

    private void getStudentsPrimaryRelativesIds(List<String> students, Handler<Either<String, JsonArray>> handler) {

        JsonObject action = new JsonObject()
                .put("action", "eleve.getPrimaryRelatives")
                .put("studentIds", students);

        eb.send("viescolaire", action, relativeRes -> {

            JsonObject body = (JsonObject) relativeRes.result().body();

            if (relativeRes.failed() || "error".equals(body.getString("status"))) {
                handler.handle(new Either.Left<>("[Massmailing@DefaultMassmailingService::getStudentsPrimaryRelativesIds] " +
                        "Error fetching students primary relatives identifiers"));
            } else {
                handler.handle(new Either.Right<>(((JsonObject) relativeRes.result().body()).getJsonArray("results", new JsonArray())));
            }
        });
    }

    private JsonArray getRelativeIdsFromList(String studentId, JsonArray studentsRelativeIds) {
        if (studentsRelativeIds != null) {
            for (int i = 0; i < studentsRelativeIds.size(); i++) {
                if (studentsRelativeIds.getJsonObject(i).getString("id").equals(studentId)) {
                    return studentsRelativeIds.getJsonObject(i).getJsonArray("primaryRelatives");
                }
            }
        }
        return new JsonArray();
    }


    @Override
    public void getStatus(String structure, MassmailingType type, boolean massmailed, List<Integer> reasons,
                          List<Integer> punishmentsTypes, List<Integer> sanctionsTypes, Integer startAt, String startDate,
                          String endDate, boolean noReasons, Handler<Either<String, JsonObject>> handler) {
        getStatus(structure, type, massmailed, reasons, punishmentsTypes, sanctionsTypes, startAt, startDate, endDate, new ArrayList<>(), noReasons, handler);
    }
}
