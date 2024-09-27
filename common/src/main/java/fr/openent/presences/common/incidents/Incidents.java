package fr.openent.presences.common.incidents;

import fr.openent.presences.common.message.MessageResponseHandler;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.InitTypeEnum;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class Incidents {
    private final String address = "fr.openent.incidents";
    private EventBus eb;

    private Incidents() {
    }

    public static Incidents getInstance() {
        return IncidentsHolder.instance;
    }

    public void init(EventBus eb) {
        this.eb = eb;
    }

    /**
     * Retrieve incidents for given users in date range
     *
     * @param startDate range start date
     * @param endDate   range end date
     * @param users     user list
     * @param handler   Function handler returning data
     */
    public void getIncidents(String startDate, String endDate, List<String> users, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "get-incidents-users-range")
                .put("startDate", startDate)
                .put("endDate", endDate)
                .put("users", new JsonArray(users));

        eb.request(address, action, MessageResponseHandler.messageJsonArrayHandler(handler));
    }

    public void getInitIncidentTypesStatement(String structure, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put(Field.ACTION, "init-get-incident-type-statement")
                .put(Field.INITTYPE, initTypeEnum.getValue())
                .put(Field.STRUCTURE, structure);

        eb.request(address, action, MessageResponseHandler.messageJsonObjectHandler(handler));
    }

    public void getInitIncidentPlacesStatement(String structure, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put(Field.ACTION, "init-get-incident-places-statement")
                .put(Field.INITTYPE, initTypeEnum.getValue())
                .put(Field.STRUCTURE, structure);

        eb.request(address, action, MessageResponseHandler.messageJsonObjectHandler(handler));
    }

    public void getInitIncidentProtagonistTypeStatement(String structure, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put(Field.ACTION, "init-get-incident-protagonist-type-statement")
                .put(Field.INITTYPE, initTypeEnum.getValue())
                .put(Field.STRUCTURE, structure);

        eb.request(address, action, MessageResponseHandler.messageJsonObjectHandler(handler));
    }

    public void getInitIncidentSeriousnessStatement(String structure, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put(Field.ACTION, "init-get-incident-seriousness-statement")
                .put(Field.INITTYPE, initTypeEnum.getValue())
                .put(Field.STRUCTURE, structure);

        eb.request(address, action, MessageResponseHandler.messageJsonObjectHandler(handler));
    }

    public void getInitIncidentPartnerStatement(String structure, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put(Field.ACTION, "init-get-incident-partner-statement")
                .put(Field.INITTYPE, initTypeEnum.getValue())
                .put(Field.STRUCTURE, structure);

        eb.request(address, action, MessageResponseHandler.messageJsonObjectHandler(handler));
    }

    public void getInitIncidentPunishmentTypeStatement(String structure, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put(Field.ACTION, "init-get-incident-punishment-type")
                .put(Field.INITTYPE, initTypeEnum.getValue())
                .put(Field.STRUCTURE, structure);

        eb.request(address, action, MessageResponseHandler.messageJsonObjectHandler(handler));
    }

    /**
     * Retrieve punishments data
     */

    public void getPunishmentType(String structure, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "get-punishment-type")
                .put("structure", structure);
        eb.request(address, action, MessageResponseHandler.messageJsonArrayHandler(handler));
    }

    public void getPunishmentsCountByStudent(String structure, String start_at, String end_at, List<String> students, List<Integer> type_id,
                                             Boolean processed, Boolean massmailed, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("structure", structure)
                .put("start_at", start_at)
                .put("end_at", end_at)
                .put("studentIds", students)
                .put("punishmentTypeIds", type_id)
                .put("processed", processed)
                .put("massmailed", massmailed)
                .put("action", "get-count-punishment-by-student");
        eb.request(address, action, MessageResponseHandler.messageJsonArrayHandler(handler));
    }

    public void getPunishmentsByStudent(String structure, String start_at, String end_at, List<String> students, List<Integer> type_id,
                                        Boolean processed, Boolean massmailed, Handler<Either<String, JsonArray>> handler) {
        getPunishmentsByStudent(structure, start_at, end_at, students, type_id, null, processed, massmailed, handler);
    }

    public void getPunishmentsByStudent(String structure, String startAt, String endAt, List<String> students, List<Integer> type_id,
                                        String eventType, Boolean processed, Boolean massmailed, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("structure", structure)
                .put("start_at", startAt)
                .put("end_at", endAt)
                .put("students", students)
                .put("punishmentTypeIds", type_id)
                .put("eventType", eventType)
                .put("processed", processed)
                .put("massmailed", massmailed)
                .put("action", "get-punishment-by-student");
        eb.request(address, action, MessageResponseHandler.messageJsonArrayHandler(handler));
    }

    public void updatePunishmentMassmailing(List<String> punishmentsIds, Boolean isMassmailed, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("punishmentsIds", punishmentsIds)
                .put("massmailed", isMassmailed)
                .put("action", "update-punishments-massmailing");
        eb.request(address, action, MessageResponseHandler.messageJsonObjectHandler(handler));
    }


    private static class IncidentsHolder {
        private static final Incidents instance = new Incidents();

        private IncidentsHolder() {
        }
    }
}
