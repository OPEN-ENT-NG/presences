package fr.openent.presences.common.helper;

import fr.openent.presences.enums.EventType;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class RegisterHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterHelper.class);
    private EventBus eb;
    private String presenceDbSchema;

    public RegisterHelper(EventBus eb, String presenceDbSchema) {
        this.presenceDbSchema = presenceDbSchema;
        this.eb = eb;
    }

    /**
     * Get register event history. From the register users list, it retrieve all day events
     *
     * @param registerDate  Register date
     * @param endDate       endDate optional parameter which is end_date
     * @param registerUsers Register users list
     * @param handler       Function handler returning data
     */
    public void getRegisterEventHistory(String registerDate, String endDate, JsonArray registerUsers, Handler<Either<String, JsonArray>> handler) {
        if (registerUsers.isEmpty()) {
            handler.handle(new Either.Right<>(new JsonArray()));
            return;
        }
        String query = "SELECT student_id, json_agg(jsonb_build_object" +
                "('id', event.id, 'counsellor_input', event.counsellor_input, 'type_id'," +
                " event.type_id, 'start_date', event.start_date, 'end_date', event.end_date," +
                " 'comment', event.comment, 'register_id', register.id, 'reason_id', reason_id)) as events " +
                "FROM " + presenceDbSchema + ".event " +
                "INNER JOIN " + presenceDbSchema + ".register ON (register.id = event.register_id) " +
                "WHERE student_id IN " + Sql.listPrepared(registerUsers.getList()) +
                " AND register.start_date > ? " +
                "AND register.end_date < ? " +
                "GROUP BY student_id;";

        JsonArray params = new JsonArray()
                .addAll(registerUsers);

        if (endDate != null && !endDate.isEmpty()) {
            params.add(registerDate + " 00:00:00").add(endDate + " 23:59:59");
        } else {
            params.add(registerDate + " 00:00:00").add(registerDate + " 23:59:59");
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    /**
     * Squash register student event history and structure slot profile.
     *
     * @param register    Current register
     * @param structureId Structure identifier
     * @param handler     Function handler returning data
     */
    public void matchSlots(JsonObject register, String structureId, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "timeslot.getSlotProfiles")
                .put("structureId", structureId);

        eb.send("viescolaire", action, (Handler<AsyncResult<Message<JsonObject>>>) event -> {
            String status = event.result().body().getString("status");
            JsonObject body = event.result().body();
            JsonArray slots = new JsonArray();
            if ("error".equals(status)) {
                LOGGER.error("[Presences@DefaultRegistrerService] Failed to retrieve slot profile");
            } else if (body.getJsonObject("result").containsKey("slots") && !body.getJsonObject("result").getJsonArray("slots").isEmpty()) {
                slots = body.getJsonObject("result").getJsonArray("slots");
            }
            JsonArray students = register.getJsonArray("students");

            try {
                JsonArray clone = cloneSlots(slots, register.getString("start_date"));
                for (int i = 0; i < students.size(); i++) {
                    JsonObject student = students.getJsonObject(i);
                    JsonArray history = student.getJsonArray("day_history");

                    JsonArray userSlots = clone.copy();
                    if (history.size() == 0) {
                        student.put("day_history", userSlots);
                    } else {
                        student.put("day_history", mergeEventsSlots(student.getJsonArray("day_history"), userSlots));
                    }
                }
                handler.handle(new Either.Right<>(register));
            } catch (Exception e) {
                String message = "[Presences@DefaultRegisterService] Failed to parse slots";
                LOGGER.error(message, e);
                handler.handle(new Either.Left<>(message));
                return;
            }
        });
    }

    /**
     * Merge User events into slots
     *
     * @param events User events
     * @param slots  User slots
     * @return Squashed slots and events
     */
    public JsonArray mergeEventsSlots(JsonArray events, JsonArray slots) {
        for (int i = 0; i < slots.size(); i++) {
            JsonObject slot = slots.getJsonObject(i);
            JsonArray slotEvents = slot.getJsonArray("events");
            try {
                for (int j = 0; j < events.size(); j++) {
                    JsonObject event = events.getJsonObject(j);
                    Integer type = event.getInteger("type_id");
                    if (matchSlot(type, event, slot)) {
                        slotEvents.add(event);
                    }
                }
            } catch (ParseException e) {
                LOGGER.error("[Presences@DefaultRegisterService] Failed to get Time diff", e);
                return slots;
            }
        }

        return slots;
    }

    /**
     * Check if event match slot
     *
     * @param type  Event type
     * @param event event object
     * @param slot  slot object
     * @return if event match slot
     * @throws ParseException Throws when dates can not be parsed
     */
    private Boolean matchSlot(Integer type, JsonObject event, JsonObject slot) throws ParseException {
        boolean lateness = type.equals(EventType.LATENESS.getType())
                && DateHelper.getAbsTimeDiff(event.getString("start_date"), slot.getString("start")) < DateHelper.TOLERANCE
                && DateHelper.isBefore(event.getString("end_date"), slot.getString("end"));

        boolean departure = type.equals(EventType.DEPARTURE.getType())
                && DateHelper.getAbsTimeDiff(event.getString("end_date"), slot.getString("end")) < DateHelper.TOLERANCE
                && DateHelper.isAfter(event.getString("start_date"), slot.getString("start"));

        boolean absence_remark = (type.equals(EventType.ABSENCE.getType()) || type.equals(EventType.REMARK.getType()))
                && DateHelper.getAbsTimeDiff(event.getString("start_date"), slot.getString("start")) < DateHelper.TOLERANCE
                && DateHelper.getAbsTimeDiff(event.getString("end_date"), slot.getString("end")) < DateHelper.TOLERANCE;

        return lateness || departure || absence_remark;
    }

    /**
     * Clone slots. Return a JsonArray of JsonObject containing start time, end time and name. All times are formatted as SQL date
     *
     * @param slots        Slots array
     * @param registerDate Register date.
     * @return Slots cloned and formatted for history day
     * @throws Exception ParseException and NumberFormatException can be throw
     */
    public JsonArray cloneSlots(JsonArray slots, String registerDate) throws Exception {
        JsonArray clone = new JsonArray();
        Calendar cal = new GregorianCalendar();
        SimpleDateFormat sdf = DateHelper.getPsqlSimpleDateFormat();
        Date date = sdf.parse(registerDate);
        cal.setTime(date);

        for (int i = 0; i < slots.size(); i++) {
            JsonObject slot = slots.getJsonObject(i);
            String[] start = slot.getString("startHour").split(":");
            String[] end = slot.getString("endHour").split(":");

            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(start[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(start[1]));
            String slotStart = sdf.format(cal.getTime());

            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(end[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(end[1]));
            String slotEnd = sdf.format(cal.getTime());

            clone.add(new JsonObject()
                    .put("events", new JsonArray())
                    .put("start", slotStart)
                    .put("end", slotEnd)
                    .put("name", slot.getString("name")));
        }

        return clone;
    }
}
