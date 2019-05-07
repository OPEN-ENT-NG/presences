package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.enums.GroupType;
import fr.openent.presences.service.GroupService;
import fr.openent.presences.service.RegisterService;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class DefaultRegisterService implements RegisterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRegisterService.class);
    private GroupService groupService;
    private EventBus eb;

    public DefaultRegisterService(EventBus eb) {
        this.eb = eb;
        this.groupService = new DefaultGroupService(eb);
    }

    @Override
    public void list(String structureId, String start, String end, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, start_date, end_date, course_id " +
                "FROM " + Presences.dbSchema + ".register " +
                "WHERE register.structure_id = ? " +
                "AND register.start_date > ? " +
                "AND register.end_date < ?";
        JsonArray params = new JsonArray()
                .add(structureId)
                .add(start)
                .add(end);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject register, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT nextval('" + Presences.dbSchema + ".register_id_seq') as id";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(idEvent -> {
            if (idEvent.isLeft()) {
                handler.handle(new Either.Left<>("[Presences@DefaultRegisterService] Failed to query next register identifier"));
                return;
            }

            try {
                Number id = idEvent.right().getValue().getInteger("id");
                groupService.getGroupsId(register.getString("structure_id"), register.getJsonArray("groups"), register.getJsonArray("classes"), groupsEvent -> {
                    if (groupsEvent.isLeft()) {
                        String message = "[Presences@DefaultRegisterService] Failed to retrieve group identifiers";
                        LOGGER.error(message, groupsEvent.left().getValue());
                        handler.handle(new Either.Left<>(message));
                        return;
                    }

                    JsonArray statements = new JsonArray();
                    statements.add(getRegisterCreationStatement(id, register, user));
                    JsonArray classes = groupsEvent.right().getValue().getJsonArray("classes");
                    for (int i = 0; i < classes.size(); i++) {
                        statements.add(getGroupCreationStatement(classes.getJsonObject(i).getString("id"), GroupType.CLASS));
                        statements.add(getRelRegisterGroupStatement(id, classes.getJsonObject(i).getString("id")));
                    }

                    JsonArray groups = groupsEvent.right().getValue().getJsonArray("groups");
                    for (int i = 0; i < groups.size(); i++) {
                        statements.add(getGroupCreationStatement(groups.getJsonObject(i).getString("id"), GroupType.GROUP));
                        statements.add(getRelRegisterGroupStatement(id, groups.getJsonObject(i).getString("id")));
                    }

                    Sql.getInstance().transaction(statements, event -> {
                        Either<String, JsonObject> result = SqlResult.validUniqueResult(0, event);
                        if (result.isLeft()) {
                            String message = "Failed to create register";
                            LOGGER.error(message, result.left().getValue());
                            handler.handle(new Either.Left<>(message));
                        } else {
                            handler.handle(new Either.Right<>(result.right().getValue()));
                        }
                    });
                });
            } catch (ClassCastException e) {
                handler.handle(new Either.Left<>("[Presences@DefaultRegisterService] Failed cast next register identifier"));
            }
        }));
    }

    @Override
    public void updateStatus(Integer registerId, Integer status, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Presences.dbSchema + ".register SET state_id = ? WHERE id = ?";
        JsonArray params = new JsonArray()
                .add(status)
                .add(registerId);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void get(Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT personnel_id, proof_id, course_id, subject_id, start_date, end_date, structure_id, counsellor_input, state_id, json_agg(\"group\".*) as groups " +
                "FROM " + Presences.dbSchema + ".register " +
                "INNER JOIN " + Presences.dbSchema + ".rel_group_register ON (register.id = rel_group_register.register_id) " +
                "INNER JOIN " + Presences.dbSchema + ".\"group\" ON (rel_group_register.group_id = \"group\".id) " +
                "WHERE register.id = ? " +
                "GROUP BY register.id;";
        JsonArray params = new JsonArray()
                .add(id);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(registerEither -> {
            if (registerEither.isLeft()) {
                String message = "[Presences@DefaultRegisterService] Failed to retrieve register " + id;
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonObject register = registerEither.right().getValue();
            if (!register.containsKey("start_date")) {
                handler.handle(new Either.Left<>("404"));
                return;
            }
            String day;
            try {
                day = getDay(register);
            } catch (ParseException e) {
                handler.handle(new Either.Left<>("[Presences@DefaultRegister] Failed to parse register date"));
                return;
            }
            JsonArray groups = new JsonArray(register.getString("groups"));
            Future<JsonArray> usersFuture = Future.future();
            Future<JsonArray> lastAbsentsFuture = Future.future();
            Future<JsonArray> groupsNameFuture = Future.future();

            CompositeFuture.all(usersFuture, lastAbsentsFuture, groupsNameFuture).setHandler(asyncEvent -> {
                if (asyncEvent.failed()) {
                    String message = "[Presences@DefaultRegisterService] Failed to retrieve groups users or last absents students";
                    LOGGER.error(message);
                    handler.handle(new Either.Left<>(message));
                    return;
                }
                JsonArray users = usersFuture.result();
                JsonArray lastAbsentUsers = reduce(lastAbsentsFuture.result(), "student_id");
                JsonObject groupsNameMap = mapGroupsName(groupsNameFuture.result());
                JsonArray userIds = new JsonArray();
                for (int i = 0; i < users.size(); i++) {
                    userIds.add(users.getJsonObject(i).getString("id"));
                }

                getRegisterEventHistory(day, userIds, historyEvent -> {
                    if (historyEvent.isLeft()) {
                        String message = "[Presences@DefaultRegisterService] Failed to retrieve register history";
                        LOGGER.error(message);
                        handler.handle(new Either.Left<>(message));
                        return;
                    }
                    JsonArray events = historyEvent.right().getValue();
                    JsonObject historyMap = extractUsersEvents(events);

                    JsonArray formattedUsers = new JsonArray();
                    for (int i = 0; i < users.size(); i++) {
                        JsonObject user = users.getJsonObject(i);
                        formattedUsers.add(formatStudent(id, user, historyMap.getJsonArray(user.getString("id"), new JsonArray()),
                                lastAbsentUsers.contains(user.getString("id")), groupsNameMap.getString(user.getString("groupId"))));
                    }
                    register.put("students", formattedUsers);
                    register.put("groups", groups);

                    matchSlots(register, register.getString("structure_id"), slotEvent -> {
                        if (slotEvent.isLeft()) {
                            String message = "[Presences@DefaultRegisterService] Failed to match slots";
                            LOGGER.error(message, slotEvent.left().getValue());
                            handler.handle(new Either.Left<>(message));
                        } else {
                            handler.handle(new Either.Right<>(slotEvent.right().getValue()));
                        }
                    });
                });
            });
            getUsers(groups, FutureHelper.handlerJsonArray(usersFuture));
            getLastAbsentsStudent(register.getString("personnel_id"), id, FutureHelper.handlerJsonArray(lastAbsentsFuture));
            getGroupsName(groups, FutureHelper.handlerJsonArray(groupsNameFuture));
        }));
    }

    /**
     * Reduce json array into an other json array containing all keys values
     *
     * @param values values that need to be reduced
     * @param key    key name
     * @return Reduced array
     */
    private JsonArray reduce(JsonArray values, String key) {
        JsonArray reduced = new JsonArray();
        JsonObject obj;
        for (int i = 0; i < values.size(); i++) {
            obj = values.getJsonObject(i);
            if (obj.containsKey(key)) {
                reduced.add(obj.getValue(key));
            }
        }
        return reduced;
    }

    private JsonObject mapGroupsName(JsonArray groups) {
        JsonObject map = new JsonObject();
        for (int i = 0; i < groups.size(); i++) {
            map.put(groups.getJsonObject(i).getString("id"), groups.getJsonObject(i).getString("name"));
        }

        return map;
    }

    private JsonObject extractUsersEvents(JsonArray events) {
        JsonObject map = new JsonObject();
        for (int i = 0; i < events.size(); i++) {
            map.put(events.getJsonObject(i).getString("student_id"), new JsonArray(events.getJsonObject(i).getString("events")));
        }

        return map;
    }

    private void getGroupsName(JsonArray groupIds, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (g: Group) WHERE g.id IN {ids} return g.name as name, g.id as id " +
                "UNION " +
                "MATCH (c: Class) WHERE c.id IN {ids} return c.name as name, c.id as id";
        JsonObject params = new JsonObject()
                .put("ids", reduce(groupIds, "id"));

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    /**
     * Get register event history. From the register users list, it retrieve all day events
     *
     * @param registerDate  Register date
     * @param registerUsers Register users list
     * @param handler       Function handler returning data
     */
    private void getRegisterEventHistory(String registerDate, JsonArray registerUsers, Handler<Either<String, JsonArray>> handler) {
        if (registerUsers.isEmpty()) {
            handler.handle(new Either.Right<>(new JsonArray()));
            return;
        }
        String query = "SELECT student_id, json_agg(jsonb_build_object" +
                "('id', event.id, 'counsellor_input', event.counsellor_input, 'type_id', event.type_id, 'start_date', event.start_date, 'end_date', event.end_date, 'comment', event.comment, 'register_id', register.id)) as events " +
                "FROM " + Presences.dbSchema + ".event " +
                "INNER JOIN " + Presences.dbSchema + ".register ON (register.id = event.register_id) " +
                "WHERE student_id IN " + Sql.listPrepared(registerUsers.getList()) +
                " AND register.start_date > ? " +
                "AND register.end_date < ? " +
                "GROUP BY student_id;";

        JsonArray params = new JsonArray()
                .addAll(registerUsers)
                .add(registerDate + " 00:00:00")
                .add(registerDate + " 23:59:59");

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    /**
     * Format user. It creates an object containing its identifier, its name, its group, its events and its event day history
     *
     * @param registerId Register identifier. The function needs the register identifier to extract events that concern the current register.
     * @param student    Student
     * @param events     Student events list
     * @param lastCourseAbsent Define if user was absent during last teacher course$
     * @param groupName User group name
     * @return Formatted student
     */
    private JsonObject formatStudent(Integer registerId, JsonObject student, JsonArray events, boolean lastCourseAbsent, String groupName) {
        JsonArray registerEvents = new JsonArray();
        for (int i = 0; i < events.size(); i++) {
            JsonObject event = events.getJsonObject(i);
            if (registerId.equals(event.getInteger("register_id"))) {
                registerEvents.add(event);
            }
        }
        return new JsonObject()
                .put("id", student.getString("id"))
                .put("name", student.getString("lastName") + " " + student.getString("firstName"))
                .put("birth_date", student.getString("birthDate"))
                .put("group", student.getString("groupId"))
                .put("group_name", groupName)
                .put("events", registerEvents)
                .put("last_course_absent", lastCourseAbsent)
                .put("day_history", events);
    }

    /**
     * Get register day. Format date as YYYY-MM-DD.
     *
     * @param register Register
     * @return Register date
     * @throws ParseException
     */
    private String getDay(JsonObject register) throws ParseException {
        Calendar cal = new GregorianCalendar();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-d'T'k:m:s.S");
        Date startDate = sdf.parse(register.getString("start_date"));
        cal.setTime(startDate);
        return cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Get all users from groups array
     *
     * @param groups  groups list
     * @param handler Function handler returning data
     */
    private void getUsers(JsonArray groups, Handler<Either<String, JsonArray>> handler) {
        List<Future> futures = new ArrayList<>();

        for (int i = 0; i < groups.size(); i++) {
            Future future = Future.future();
            JsonObject group = groups.getJsonObject(i);
            GroupType type = "CLASS".equals(group.getString("type")) ? GroupType.CLASS : GroupType.GROUP;
            groupService.getGroupUsers(group.getString("id"), type, FutureHelper.handlerJsonArray(future));
            futures.add(future);
        }

        CompositeFuture.all(futures).setHandler(event -> {
            if (event.failed()) {
                LOGGER.error(event.cause());
                handler.handle(new Either.Left<>(event.cause().toString()));
            } else {
                JsonArray res = new JsonArray();
                HashMap<String, Boolean> map = new HashMap<>();
                for (int i = 0; i < futures.size(); i++) {
                    JsonArray users = (JsonArray) futures.get(i).result();
                    JsonObject user;
                    for (int j = 0; j < users.size(); j++) {
                        user = users.getJsonObject(j);
                        user.put("groupId", groups.getJsonObject(i).getString("id"));
                        if (!map.containsKey(user.getString("id"))) {
                            map.put(user.getString("id"), true);
                            res.add(user);
                        }
                    }
                }
                handler.handle(new Either.Right<>(res));
            }
        });
    }

    private void getLastAbsentsStudent(String personnelId, Integer registerIdentifier, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH previous_register as (SELECT register.id as id " +
                "FROM presences.register " +
                "INNER JOIN presences.rel_group_register ON (register.id = rel_group_register.register_id) " +
                "WHERE register.personnel_id = ? " +
                "AND rel_group_register.group_id IN ( " +
                "SELECT group_id " +
                "FROM presences.register " +
                "INNER JOIN presences.rel_group_register ON (register.id = rel_group_register.register_id) " +
                "WHERE register.id = ?) " +
                "AND register.id < ? " +
                "ORDER BY start_date DESC " +
                "LIMIT 1) " +
                "SELECT student_id " +
                "FROM presences.event " +
                "INNER JOIN previous_register ON (previous_register.id = event.register_id) " +
                "AND type_id = ?;";

        JsonArray params = new JsonArray()
                .add(personnelId)
                .add(registerIdentifier)
                .add(registerIdentifier)
                .add(EventType.ABSENCE.getType());

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    /**
     * Squash register student event history and structure slot profile.
     *
     * @param register    Current register
     * @param structureId Structure identifier
     * @param handler     Function handler returning data
     */
    private void matchSlots(JsonObject register, String structureId, Handler<Either<String, JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "timeslot.getSlotProfiles")
                .put("structureId", structureId);

        eb.send("viescolaire", action, (Handler<AsyncResult<Message<JsonObject>>>) event -> {
            String status = event.result().body().getString("status");
            JsonArray slots = new JsonArray();
            if ("error".equals(status)) {
                LOGGER.error("[Presences@DefaultRegistrerService] Failed to retrieve slot profile");
            } else {
                slots = event.result().body().getJsonArray("results");
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
    private JsonArray mergeEventsSlots(JsonArray events, JsonArray slots) {
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

        boolean absence = type.equals(EventType.ABSENCE.getType())
                && DateHelper.getAbsTimeDiff(event.getString("start_date"), slot.getString("start")) < DateHelper.TOLERANCE
                && DateHelper.getAbsTimeDiff(event.getString("end_date"), slot.getString("end")) < DateHelper.TOLERANCE;

        return lateness || departure || absence;
    }

    /**
     * Clone slots. Return a JsonArray of JsonObject containing start time, end time and name. All times are formatted as SQL date
     *
     * @param slots        Slots array
     * @param registerDate Register date.
     * @return Slots cloned and formatted for history day
     * @throws Exception ParseException and NumberFormatException can be throw
     */
    private JsonArray cloneSlots(JsonArray slots, String registerDate) throws Exception {
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

    /**
     * Get statement that create register
     *
     * @param id       register identifier
     * @param register register
     * @param user     current user
     * @return Statement
     */
    private JsonObject getRegisterCreationStatement(Number id, JsonObject register, UserInfos user) {
        String query = "INSERT INTO " + Presences.dbSchema + ".register (id, structure_id, personnel_id, course_id, state_id, owner, start_date, end_date, subject_id) " +
                "VALUES (?, ?, ?, ?, 1, ?, ?, ?, ?) RETURNING id, structure_id, course_id, subject_id, start_date, end_date, counsellor_input, state_id;";

        JsonArray params = new JsonArray()
                .add(id)
                .add(register.getString("structure_id"))
                .add(user.getUserId())
                .add(register.getString("course_id"))
                .add(user.getUserId())
                .add(register.getString("start_date"))
                .add(register.getString("end_date"))
                .add(register.getString("subject_id"));

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    /**
     * Get statement that inster group
     *
     * @param id   group identifier
     * @param type group type
     * @return Statement
     */
    private JsonObject getGroupCreationStatement(String id, GroupType type) {
        String query = "INSERT INTO " + Presences.dbSchema + ".group (id, type) VALUES (?, ?) ON CONFLICT DO NOTHING";
        JsonArray params = new JsonArray()
                .add(id)
                .add(type);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    /**
     * Get statement that create relation between register and group
     *
     * @param id      register identifier
     * @param groupId group identifier
     * @return Statement
     */
    private JsonObject getRelRegisterGroupStatement(Number id, String groupId) {
        String query = "INSERT INTO " + Presences.dbSchema + ".rel_group_register (register_id, group_id) VALUES (?, ?) ON CONFLICT DO NOTHING;";
        JsonArray params = new JsonArray()
                .add(id)
                .add(groupId);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

}
