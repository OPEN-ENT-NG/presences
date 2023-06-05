package fr.openent.presences.common.viescolaire;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.message.MessageResponseHandler;
import fr.openent.presences.core.constants.Field;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

public class Viescolaire {
    private static final Logger LOGGER = LoggerFactory.getLogger(Viescolaire.class);

    private String address = "viescolaire";
    private EventBus eb;

    private Viescolaire() {
    }

    public static Viescolaire getInstance() {
        return ViescolaireHolder.instance;
    }

    public void init(EventBus eb) {
        this.eb = eb;
    }

    /**
     * Retrieve all exclusions days (holidays/vacations/day's off...)
     *
     * @param structureId structure identifier
     * @param handler     Function handler returning data
     */
    public void getExclusionDays(String structureId, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "periode.getExclusionDays")
                .put("idEtablissement", structureId);

        eb.send(address, action, result -> {
            JsonObject body = (JsonObject) result.result().body();
            if (result.failed() || "error".equals(body.getString("status"))) {
                String err = "[Common@ViescolaireHelper] Failed to retrieve exclusion days";
                LOGGER.error(err);
                handler.handle(new Either.Left<>(err));
            } else {
                handler.handle(new Either.Right<>(body.getJsonArray("results")));
            }
        });
    }

    /**
     * fetch structure's school year period
     * @param structure structure identifier {@link String}
     * @return {@link Future <JsonObject>}
     */
    public Future<JsonObject> getSchoolYear(String structure) {
        Promise<JsonObject> promise = Promise.promise();
        JsonObject action = new JsonObject()
                .put("action", "periode.getSchoolYearPeriod")
                .put("structureId", structure);

        eb.request("viescolaire", action, MessageResponseHandler.messageJsonObjectHandler(FutureHelper.handlerEitherPromise(promise)));

        return promise.future();
    }

    public void getSlotProfileSetting(String structureId, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "timeslot.getSlotProfileSettings")
                .put("structureId", structureId);

        eb.send(address, action, MessageResponseHandler.messageJsonObjectHandler(handler));
    }

    public Future<JsonObject> getSlotProfileSetting(String structureId) {
        Promise<JsonObject> promise = Promise.promise();
        getSlotProfileSetting(structureId, FutureHelper.handlerJsonObject(promise));
        return promise.future();
    }

    public void getSlotProfiles(String structureId, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "timeslot.getSlotProfiles")
                .put("structureId", structureId);

        eb.send(address, action, MessageResponseHandler.messageJsonObjectHandler(handler));
    }

    public void getSlotsFromProfile(String structureId, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "timeslot.getSlotProfiles")
                .put("structureId", structureId);

        eb.send(address, action, MessageResponseHandler.messageJsonObjectHandler(event -> {
            if (event.isLeft()) {
                String err = "[Common@ViescolaireHelper::getSlotsFromProfile] Failed to retrieve slots from profile";
                LOGGER.error(err);
                handler.handle(new Either.Left<>(event.left().getValue()));
            } else {
                // send handler empty array if none data
                if (event.right().getValue().isEmpty() || (event.right().getValue().containsKey("slots") &&
                        event.right().getValue().getJsonArray("slots").isEmpty())) {
                    handler.handle(new Either.Right<>(new JsonArray()));
                } else {
                    // change format start and end Hour ( e.g old "08:25" => into now "08:25:00")
                    event.right().getValue().getJsonArray("slots").forEach(slotObj -> {
                        JsonObject slot = ((JsonObject) slotObj);
                        String parsedStartHour = DateHelper.fetchTimeString(slot.getString("startHour"), DateHelper.HOUR_MINUTES);
                        String parsedEndHour = DateHelper.fetchTimeString(slot.getString("endHour"), DateHelper.HOUR_MINUTES);
                        slot.put("startHour", parsedStartHour);
                        slot.put("endHour", parsedEndHour);
                    });
                    handler.handle(new Either.Right<>(event.right().getValue().getJsonArray("slots")));
                }
            }
        }));
    }

    public void getDefaultSlots(String structureId, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "timeslot.getDefaultSlots")
                .put("structureId", structureId);

        eb.send(address, action, result -> {
            JsonObject body = (JsonObject) result.result().body();
            if (result.failed() || "error".equals(body.getString("status"))) {
                String err = "[Common@ViescolaireHelper] Failed to retrieve default slots";
                LOGGER.error(err);
                handler.handle(new Either.Left<>(err));
            } else {
                handler.handle(new Either.Right<>(body.getJsonArray("results")));
            }
        });
    }

    public Future<JsonArray> getCountStudentsByAudiences(List<String> audienceIds) {
        Promise<JsonArray> promise = Promise.promise();
        JsonObject action = new JsonObject()
                .put(Field.ACTION, "classe.getNbElevesGroupe")
                .put(Field.IDGROUPES, audienceIds);

        eb.request(address, action, MessageResponseHandler.messageJsonArrayHandler(FutureHelper.handlerJsonArray(promise)));
        return promise.future();
    }

    public Future<JsonArray> getAudienceTimeslots(String structureId, List<String> audienceIds) {
        Promise<JsonArray> promise = Promise.promise();
        JsonObject action = new JsonObject()
                .put(Field.ACTION, "timeslot.getAudienceTimeslot")
                .put(Field.STRUCTUREID, structureId)
                .put(Field.AUDIENCEIDS, audienceIds);

        eb.request(address, action, MessageResponseHandler.messageJsonArrayHandler(FutureHelper.handlerJsonArray(promise)));
        return promise.future();
    }

    public Future<JsonArray> getGroupingStructure(String structureId, String searchValue) {
        Promise<JsonArray> promise = Promise.promise();
        JsonObject action = new JsonObject()
                .put(Field.ACTION, "grouping.getGroupingStructure")
                .put(Field.STRUCTUREID, structureId)
                .put(Field.SEARCHVALUE, searchValue);

        eb.request(address, action, MessageResponseHandler.messageJsonArrayHandler(FutureHelper.handlerEitherPromise(promise)));
        return promise.future();
    }

    public Future<JsonObject> getStudentTimeslot(List<String> studentIds, String structureId) {
        Promise<JsonObject> promise = Promise.promise();
        JsonObject action = new JsonObject()
                .put(Field.ACTION, "timeslot.getTimeslotFromStudentIds")
                .put(Field.STUDENT_ID_LIST, studentIds)
                .put(Field.STRUCTUREID, structureId);

        eb.request(address, action, MessageResponseHandler.messageJsonObjectHandler(FutureHelper.handlerEitherPromise(promise)));
        return promise.future();
    }

    private static class ViescolaireHolder {
        private static final Viescolaire instance = new Viescolaire();

        private ViescolaireHolder() {
        }
    }
}
