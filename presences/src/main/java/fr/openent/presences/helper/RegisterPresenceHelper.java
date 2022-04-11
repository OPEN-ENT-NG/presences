package fr.openent.presences.helper;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.helper.PersonHelper;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.enums.Events;
import fr.openent.presences.model.Event.RegisterEvent;
import fr.openent.presences.model.Person.User;
import fr.openent.presences.model.Register;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterPresenceHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterPresenceHelper.class);
    private final UserService userService;
    private final PersonHelper personHelper;

    public RegisterPresenceHelper() {
        this.userService = new DefaultUserService();
        this.personHelper = new PersonHelper();
    }

    /**
     * Convert JsonArray into register list
     *
     * @param array               JsonArray response
     * @param mandatoryAttributes List of mandatory attributes
     * @return new list of events
     */
    public static List<Register> getRegisterListFromJsonArray(JsonArray array, List<String> mandatoryAttributes) {
        List<Register> registerList = new ArrayList<>();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            Register register = new Register((JsonObject) o, mandatoryAttributes);
            registerList.add(register);
        }
        return registerList;
    }

    /**
     * Convert JsonArray into register events list
     *
     * @param array JsonArray response
     * @return new list of events
     */
    public static List<RegisterEvent> getRegisterEventListFromJsonArray(JsonArray array) {
        List<RegisterEvent> registerEventList = new ArrayList<>();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            RegisterEvent registerEvent = new RegisterEvent((JsonObject) o);
            registerEventList.add(registerEvent);
        }
        return registerEventList;
    }

    @SuppressWarnings("unchecked")
    public void addOwnerToStudentEvents(JsonObject register, Handler<AsyncResult<JsonObject>> handler) {
        List<String> userIds = this.getAllOwnerIds(register.getJsonArray("students"));
        userService.getUsers(userIds, result -> {
            if (result.isLeft()) {
                String message = "[Presences@RegisterPresenceHelper::addOwnerToStudentEvents] Failed to retrieve users info";
                LOGGER.error(message);
                handler.handle(Future.failedFuture(message));
            }
            List<User> owners = personHelper.getUserListFromJsonArray(result.right().getValue());
            Map<String, User> userMap = new HashMap<>();
            owners.forEach(owner -> userMap.put(owner.getId(), owner));
            ((List<JsonObject>) register.getJsonArray("students").getList()).forEach(student ->
                    ((List<JsonObject>) student.getJsonArray("day_history").getList()).forEach(dayHistory ->
                            ((List<JsonObject>) dayHistory.getJsonArray("events").getList()).forEach(event -> {
                                if (!event.getString("type").toUpperCase().equals(Events.ABSENCE.toString()) && event.getValue("owner") instanceof String) {
                                    JsonObject owner = userMap.getOrDefault(event.getString("owner"), new User(event.getString("owner"))).toJSON();
                                    event.put("owner", owner);
                                }
                            })
                    )
            );
            handler.handle(Future.succeededFuture());
        });
    }

    @SuppressWarnings("unchecked")
    private List<String> getAllOwnerIds(JsonArray students) {
        List<String> userIds = new ArrayList<>();
        ((List<JsonObject>) students.getList()).forEach(student ->
                ((List<JsonObject>) student.getJsonArray("day_history").getList()).forEach(dayHistory ->
                        ((List<JsonObject>) dayHistory.getJsonArray("events").getList()).forEach(event -> {
                            if (event.containsKey("owner") && !userIds.contains(event.getString("owner"))) {
                                userIds.add(event.getString("owner"));
                            }
                        })
                )
        );
        return userIds;
    }

    public static void getEventHistory(String structureId, String startDate, String endDate, String startTime, String endTime,
                                 List<String> registerUsers, List<String> typeIds, List<String> reasonIds, Boolean noAbsenceReason, Boolean noLatenessReason,
                                 Boolean regularized, Boolean followed, Handler<AsyncResult<JsonArray>> handler) {

        if (registerUsers.isEmpty()) {
            handler.handle(Future.succeededFuture(new JsonArray()));
            return;
        }
        JsonArray params = new JsonArray();

        String query = "SELECT student_id, json_agg(jsonb_build_object( " +
                " 'id', e.id, 'counsellor_input', e.counsellor_input, 'counsellor_regularisation', e.counsellor_regularisation, " +
                " 'followed', e.followed, 'massmailed', e.massmailed, 'type_id', " +
                " e.type_id, 'start_date', e.start_date, 'end_date', e.end_date, " +
                " 'comment', e.comment, 'owner', e.owner, 'register_id', r.id, 'reason_id', e.reason_id)) as events " +
                " FROM " + Presences.dbSchema + ".event as e " +
                EventQueryHelper.joinRegister(structureId, params) +
                filterDates(startDate, endDate, params) +
                filterTimes(startTime, endTime, params) +
                EventQueryHelper.filterStudentIds(registerUsers, params) +
                EventQueryHelper.filterReasons(reasonIds, noAbsenceReason, noLatenessReason,regularized, typeIds, params) +
                EventQueryHelper.filterFollowed(followed, params) +
                filterTypes(typeIds, params) +
                " GROUP BY student_id; ";

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(handler)));
    }

    private static String filterDates(String startDate, String endDate, JsonArray params) {
        if (endDate != null && !endDate.isEmpty())
            params.add(startDate + " " + EventQueryHelper.DEFAULT_START_TIME)
                    .add(endDate + " " + EventQueryHelper.DEFAULT_END_TIME);
        else
            params.add(startDate + " " + EventQueryHelper.DEFAULT_START_TIME)
                    .add(startDate + " " + EventQueryHelper.DEFAULT_END_TIME);
        return " WHERE r.start_date > ? " +
                " AND r.end_date < ? ";
    }

    private static String filterTypes(List<String> typeIds, JsonArray params) {
        if (typeIds != null && !typeIds.isEmpty()) {
            params.addAll(new JsonArray(typeIds));
            return " AND e.type_id IN "
                    + Sql.listPrepared(typeIds.toArray());
        }
        return "";
    }

    public static String filterTimes(String startTime, String endTime, JsonArray params) {
        String query = "";

        if (endTime != null) {
            query += " AND r.start_date::time < ? ";
            params.add(endTime);
        }

        if (startTime != null) {
            query += " AND r.end_date::time > ? ";
            params.add(startTime);
        }

        return query;
    }
}
