package fr.openent.massmailing.service.impl;

import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.MassmailingType;
import fr.openent.massmailing.service.MassmailingService;
import fr.openent.presences.common.presences.Presences;
import fr.openent.presences.enums.EventType;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
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


    @Override
    public void getStatus(String structure, MassmailingType type, Boolean massmailed, List<Integer> reasons, Integer startAt, String startDate,
                          String endDate, List<String> students, boolean noReasons, Handler<Either<String, JsonObject>> handler) {
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

        getCountEventByStudent(structure, type, massmailed, reasons, startAt, startDate, endDate, students, noReasons, callback);
    }

    @Override
    public void getCountEventByStudent(String structure, MassmailingType type, Boolean massmailed, List<Integer> reasons, Integer startAt, String startDate,
                                       String endDate, List<String> students, boolean noReasons, Handler<Either<String, JsonArray>> handler) {
        switch (type) {
            case JUSTIFIED:
                Presences.getInstance().getCountEventByStudent(EventType.ABSENCE.getType(), students, structure, true, startAt,
                        reasons, massmailed, startDate, endDate, noReasons, handler);
                break;
            case UNJUSTIFIED:
                Presences.getInstance().getCountEventByStudent(EventType.ABSENCE.getType(), students, structure, false, startAt,
                        reasons, massmailed, startDate, endDate, noReasons, handler);
                break;
            case LATENESS:
                Presences.getInstance().getCountEventByStudent(EventType.LATENESS.getType(), students, structure, false, startAt,
                        reasons, massmailed, startDate, endDate, noReasons, handler);
                break;
            default:
                handler.handle(new Either.Left<>("[Massmailing@DefaultMassmailingService] Unknown Massmailing type"));
        }
    }

    @Override
    public void getAnomalies(MailingType type, List<String> students, Handler<Either<String, JsonArray>> handler) {
        switch (type) {
            case MAIL:
                String query = "MATCH (u:User)-[:RELATED]->(r:User) WHERE NOT(HAS(r.email)) " +
                        "AND u.id IN {users} RETURN DISTINCT u.id as id, (u.lastName + ' ' + u.firstName) as displayName, split(u.classes[0],'$')[1] as className";
                JsonObject params = new JsonObject()
                        .put("users", new JsonArray(students));
                Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
                break;
            default:
                handler.handle(new Either.Left<>("[Massmailing@DefaultMassmailingService] Unknown Mailing type"));
        }
    }

    @Override
    public void getRelatives(MailingType type, List<String> students, Handler<Either<String, JsonArray>> handler) {
        String contactValue;
        switch (type) {
            case MAIL:
                contactValue = "r.email";
                break;
            case SMS:
                contactValue = "r.mobile";
                break;
            case PDF:
            default:
                contactValue = "";
        }

        String query = "MATCH (u:User)-[:RELATED]->(r:User) WHERE u.id IN {students} RETURN u.id as id, " +
                "(u.lastName + ' ' + u.firstName) as displayName, split(u.classes[0],'$')[1] as className, " +
                "collect({id: r.id, displayName: (r.lastName + ' ' + r.firstName), contact: " + contactValue + "}) as relative";
        JsonObject params = new JsonObject()
                .put("students", new JsonArray(students));
        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));

    }

    @Override
    public void getStatus(String structure, MassmailingType type, boolean massmailed, List<Integer> reasons, Integer startAt, String startDate, String endDate, boolean noReasons, Handler<Either<String, JsonObject>> handler) {
        getStatus(structure, type, massmailed, reasons, startAt, startDate, endDate, new ArrayList<>(), noReasons, handler);
    }
}