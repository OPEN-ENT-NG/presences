package fr.openent.presences.common.presences;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.message.MessageResponseHandler;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class Presences {
    private String address = "fr.openent.presences";
    private EventBus eb;

    private Presences() {
    }

    public static Presences getInstance() {
        return PresencesHolder.instance;
    }

    public void init(EventBus eb) {
        this.eb = eb;
    }

    public void getCountEventByStudent(Integer eventType, List<String> students, String structure, Boolean justified, Integer startAt, List<Integer> reasonsId, Boolean massmailed,
                                       String startDate, String endDate, boolean noReasons, String recoveryMethod, Boolean regularized, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("eventType", eventType)
                .put("justified", justified)
                .put("students", new JsonArray(students))
                .put("structure", structure)
                .put("startAt", startAt)
                .put("reasonsId", reasonsId)
                .put("massmailed", massmailed)
                .put("startDate", startDate)
                .put("endDate", endDate)
                .put("regularized", regularized)
                .put("noReasons", noReasons)
                .put("recoveryMethod", recoveryMethod)
                .put("action", "get-count-event-by-student");
        eb.send(address, action, MessageResponseHandler.messageJsonArrayHandler(handler));
    }

    public void getCountEventByStudent(Integer eventType, List<String> students, String structure, Boolean justified, Integer startAt, List<Integer> reasonsId, Boolean massmailed,
                                       String startDate, String endDate, boolean noReasons, Boolean regularized, Handler<Either<String, JsonArray>> handler) {
        getCountEventByStudent(eventType, students, structure, justified, startAt, reasonsId, massmailed,
                startDate, endDate, noReasons, null, regularized, handler);
    }

    public void getEventsByStudent(Integer eventType, List<String> students, String structure, Boolean justified, List<Integer> reasonsId, Boolean massmailed,
                                   String startDate, String endDate, Boolean noReasons, String recoveryMethod, Boolean regularized, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("eventType", eventType)
                .put("justified", justified)
                .put("students", new JsonArray(students))
                .put("structure", structure)
                .put("reasonsId", reasonsId)
                .put("massmailed", massmailed)
                .put("startDate", startDate)
                .put("endDate", endDate)
                .put("noReasons", noReasons)
                .put("recoveryMethod", recoveryMethod)
                .put("regularized", regularized)
                .put("action", "get-events-by-student");
        eb.send(address, action, MessageResponseHandler.messageJsonArrayHandler(handler));
    }


    public void getEventsByStudent(Integer eventType, List<String> students, String structure, Boolean justified, List<Integer> reasonsId, Boolean massmailed,
                                   String startDate, String endDate, Boolean noReasons, String recoveryMethod, Handler<Either<String, JsonArray>> handler) {
        getEventsByStudent(eventType, students, structure, justified, reasonsId, massmailed, startDate, endDate, noReasons, recoveryMethod, null, handler);
    }

    public void getAbsences(List<String> studentIds, String startDate, String endDate, Handler<AsyncResult<JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("startDate", startDate)
                .put("endDate", endDate)
                .put("studentIds", studentIds)
                .put("action", "get-absences");
        eb.send(address, action, MessageResponseHandler.messageJsonArrayHandler(FutureHelper.handlerJsonArray(handler)));
    }

    public void createAbsences(String structureId, List<String> studentIds, String userInfoId, Long reasonId, String startDate, String endDate, boolean editEvents,
                               Handler<AsyncResult<JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("structure_id", structureId)
                .put("reason_id", reasonId)
                .put("start_date", startDate)
                .put("end_date", endDate)
                .put("userId", userInfoId)
                .put("studentIds", studentIds)
                .put("editEvents", editEvents)
                .put("action", "create-absences");
        eb.send(address, action, MessageResponseHandler.messageJsonObjectHandler(FutureHelper.handlerJsonObject(handler)));
    }

    public void updateAbsence(String structureId, Long absenceId, String userInfoId, Long reasonId, String studentId,
                              String startDate, String endDate, boolean editEvents, Handler<AsyncResult<JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("structure_id", structureId)
                .put("reason_id", reasonId)
                .put("student_id", studentId)
                .put("start_date", startDate)
                .put("end_date", endDate)
                .put("userId", userInfoId)
                .put("absenceId", absenceId)
                .put("editEvents", editEvents)
                .put("action", "update-absence");
        eb.send(address, action, MessageResponseHandler.messageJsonObjectHandler(FutureHelper.handlerJsonObject(handler)));
    }

    public void deleteAbsence(Long absenceId, Handler<AsyncResult<JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("absenceId", absenceId)
                .put("action", "delete-absence");
        eb.send(address, action, MessageResponseHandler.messageJsonObjectHandler(FutureHelper.handlerJsonObject(handler)));
    }

    public void getReasons(String structure, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("structure", structure)
                .put("action", "get-reasons");
        eb.send(address, action, MessageResponseHandler.messageJsonArrayHandler(handler));
    }

    public void getSettings(String structure, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("structure", structure)
                .put("action", "get-settings");
        eb.send(address, action, MessageResponseHandler.messageJsonObjectHandler(handler));
    }

    private static class PresencesHolder {
        private static final Presences instance = new Presences();

        private PresencesHolder() {
        }
    }
}
