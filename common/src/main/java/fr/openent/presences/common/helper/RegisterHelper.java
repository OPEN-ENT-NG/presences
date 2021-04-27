package fr.openent.presences.common.helper;

import fr.openent.presences.enums.EventType;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
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
     * @param structureId   Structure identifier
     * @param registerDate  Register date
     * @param endDate       endDate optional parameter which is end_date
     * @param registerUsers Register users list
     * @param handler       Function handler returning data
     */
    public void getRegisterEventHistory(String structureId, String registerDate, String endDate, JsonArray registerUsers, Handler<Either<String, JsonArray>> handler) {
        if (registerUsers.isEmpty()) {
            handler.handle(new Either.Right<>(new JsonArray()));
            return;
        }
        String query = "SELECT student_id, json_agg(jsonb_build_object" +
                "('id', event.id, 'counsellor_input', event.counsellor_input, 'counsellor_regularisation', event.counsellor_regularisation," +
                " 'followed', event.followed, 'massmailed', event.massmailed, 'type_id'," +
                " event.type_id, 'start_date', event.start_date, 'end_date', event.end_date," +
                " 'comment', event.comment, 'owner', event.owner, 'register_id', register.id, 'reason_id', reason_id)) as events " +
                "FROM " + presenceDbSchema + ".event " +
                "INNER JOIN " + presenceDbSchema + ".register ON (register.id = event.register_id AND register.structure_id = ?) " +
                "WHERE student_id IN " + Sql.listPrepared(registerUsers.getList()) +
                " AND register.start_date > ? " +
                "AND register.end_date < ? " +
                "GROUP BY student_id;";

        JsonArray params = new JsonArray()
                .add(structureId)
                .addAll(registerUsers);

        if (endDate != null && !endDate.isEmpty()) {
            params.add(registerDate + " 00:00:00").add(endDate + " 23:59:59");
        } else {
            params.add(registerDate + " 00:00:00").add(registerDate + " 23:59:59");
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    /**
     * get absence event history.
     *
     * @param startDate     startDate absence
     * @param endDate       endDate absence
     * @param registerUsers Register users list
     * @param handler       Function handler returning data
     */
    public void getAbsence(String startDate, String endDate, JsonArray registerUsers, Handler<Either<String, JsonArray>> handler) {
        if (registerUsers.isEmpty() && (startDate != null && !startDate.isEmpty()) && (endDate != null && !endDate.isEmpty())) {
            handler.handle(new Either.Right<>(new JsonArray()));
            return;
        }
        String query = "SELECT * FROM presences.absence " +
                "WHERE student_id IN " + Sql.listPrepared(registerUsers.getList()) +
                "AND start_date > ? " +
                "AND end_date < ? ";

        JsonArray params = new JsonArray()
                .addAll(registerUsers)
                .add(startDate + " 00:00:00")
                .add(endDate + " 23:59:59");

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    /**
     * Future way to get absence event history.
     *
     * @param startDate     startDate absence
     * @param endDate       endDate absence
     * @param idStudents    Users list retrieved
     * @param future        Function handler returning data
     */
    public void getAbsence(String startDate, String endDate, JsonArray idStudents, Future<JsonArray> future) {
        getAbsence(startDate, endDate, idStudents, FutureHelper.handlerJsonArray(future));
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
                    JsonObject event = events.getJsonObject(j).put("type", "event");
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
        boolean lateness = type.equals(EventType.LATENESS.getType()) &&
                DateHelper.isBetween(event.getString("start_date"), event.getString("end_date"), slot.getString("start"), slot.getString("end"));

        boolean departure = type.equals(EventType.DEPARTURE.getType())
                && DateHelper.getAbsTimeDiff(event.getString("end_date"), slot.getString("end")) < DateHelper.TOLERANCE
                && DateHelper.isAfter(event.getString("start_date"), slot.getString("start"));

//        boolean absence_remark = (type.equals(EventType.ABSENCE.getType()) || type.equals(EventType.REMARK.getType()))
//                && DateHelper.getAbsTimeDiff(event.getString("start_date"), slot.getString("start")) < DateHelper.TOLERANCE
//                && DateHelper.getAbsTimeDiff(event.getString("end_date"), slot.getString("end")) < DateHelper.TOLERANCE;

        boolean absence_remark = (type.equals(EventType.ABSENCE.getType()) || type.equals(EventType.REMARK.getType()))
                && DateHelper.isBetween(event.getString("start_date"), event.getString("end_date"), slot.getString("start"), slot.getString("end"));

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
