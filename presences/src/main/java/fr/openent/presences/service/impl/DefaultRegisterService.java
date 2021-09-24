package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.helper.RegisterHelper;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.db.DBService;
import fr.openent.presences.enums.*;
import fr.openent.presences.helper.*;
import fr.openent.presences.model.*;
import fr.openent.presences.service.ExemptionService;
import fr.openent.presences.service.NotebookService;
import fr.openent.presences.service.RegisterService;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class DefaultRegisterService extends DBService implements RegisterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRegisterService.class);
    private final EventBus eb;
    private final GroupService groupService;
    private final ExemptionService exemptionService;
    private final RegisterHelper registerHelper;
    private final CourseHelper courseHelper;
    private final RegisterPresenceHelper registerPresenceHelper;
    private final NotebookService notebookService;

    public DefaultRegisterService(EventBus eb) {
        this.eb = eb;
        this.groupService = new DefaultGroupService(eb);
        this.exemptionService = new DefaultExemptionService(eb);
        this.registerHelper = new RegisterHelper(eb, Presences.dbSchema);
        this.registerPresenceHelper = new RegisterPresenceHelper();
        this.courseHelper = new CourseHelper(eb);
        this.notebookService = new DefaultNotebookService();
    }

    @Override
    public void list(String structureId, String start, String end, Handler<Either<String, JsonArray>> handler) {
        this.list(structureId, start, end, null, null, null,
                false, null, null, handler);
    }

    @Override
    public void list(String structureId, String start, String end, List<String> courseIds,
                     List<String> teacherIds, List<String> groupIds, boolean forgottenFilter,
                     String limit, String offset, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, start_date, end_date, course_id, state_id, notified, split_slot " +
                "FROM " + Presences.dbSchema + ".register AS register ";

        if (groupIds != null && !groupIds.isEmpty()) {
            query += "INNER JOIN presences.rel_group_register AS rg ON (register.id = rg.register_id) ";
        }

        query += "WHERE register.structure_id = ? " +
                "AND register.start_date > ? " +
                "AND register.end_date < ?";

        JsonArray params = new JsonArray()
                .add(structureId)
                .add(start + " " + EventQueryHelper.DEFAULT_START_TIME)
                .add(end + " " + EventQueryHelper.DEFAULT_END_TIME);

        if (teacherIds != null && !teacherIds.isEmpty()) {
            query += " AND register.personnel_id IN " + Sql.listPrepared(teacherIds.toArray());
            params.addAll(new JsonArray(teacherIds));
        }

        if (groupIds != null && !groupIds.isEmpty()) {
            query += " AND rg.group_id IN " + Sql.listPrepared(groupIds.toArray());
            params.addAll(new JsonArray(groupIds));
        }

        if (courseIds != null && !courseIds.isEmpty()) {
            query += " AND register.course_id IN " + Sql.listPrepared(courseIds.toArray());
            params.addAll(new JsonArray(courseIds));
        }

        if (forgottenFilter) {
            query += " AND register.state_id != " + RegisterStatus.DONE.getStatus();
        }

        if (limit != null && offset != null) {
            query += " ORDER BY register.start_date, register.id";
            query += " OFFSET ? LIMIT ? ";
            params.add(offset);
            params.add(limit);
        }

        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public Future<JsonArray> list(String structureId, String start, String end, List<String> courseIds,
                                  List<String> teacherIds, List<String> groupIds, boolean forgottenFilter,
                                  String limit, String offset) {

        Promise<JsonArray> promise = Promise.promise();

        this.list(structureId, start, end, courseIds, teacherIds, groupIds, forgottenFilter,
                limit, offset, FutureHelper.handlerJsonArray(promise));

        return promise.future();
    }

    @Override
    public void list(String structureId, List<String> coursesId, Handler<Either<String, JsonArray>> handler) {
        if (coursesId.isEmpty()) {
            /* sending empty array since we did not fetch any course_id */
            handler.handle(new Either.Right<>(new JsonArray()));
        } else {
            String query = "SELECT id, start_date, end_date, course_id, state_id, notified, split_slot " +
                    "FROM " + Presences.dbSchema + ".register " +
                    "WHERE register.structure_id = ? " +
                    "AND course_id IN " + Sql.listPrepared(coursesId);
            JsonArray params = new JsonArray()
                    .add(structureId)
                    .addAll(new JsonArray(coursesId));

            Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
        }
    }

    @Override
    public void create(JsonObject register, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        fetchIfRegisterExists(register, existsEither -> {
            if (existsEither.isLeft()) {
                handler.handle(existsEither.left());
                return;
            }

            JsonObject existingRegister = existsEither.right().getValue();
            if (!existingRegister.fieldNames().isEmpty()) {
                handler.handle(existsEither.right());
                return;
            }

            String query = "SELECT nextval('" + Presences.dbSchema + ".register_id_seq') as id";
            Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(idEvent -> {
                if (idEvent.isLeft()) {
                    handler.handle(new Either.Left<>("[Presences@DefaultRegisterService] Failed to query next register identifier"));
                    return;
                }

                try {
                    Number id = idEvent.right().getValue().getInteger("id");
                    groupService.getGroupsId(register.getString("structure_id"), register.getJsonArray("groups"),
                            register.getJsonArray("classes"), groupsEvent -> {
                                if (groupsEvent.isLeft()) {
                                    String message = "[Presences@DefaultRegisterService] Failed to retrieve group identifiers";
                                    LOGGER.error(message, groupsEvent.left().getValue());
                                    handler.handle(new Either.Left<>(message));
                                    return;
                                }

                                JsonArray classes = groupsEvent.right().getValue().getJsonArray("classes");
                                JsonArray groups = groupsEvent.right().getValue().getJsonArray("groups");
                                JsonArray manualGroups = groupsEvent.right().getValue().getJsonArray("manualGroups");
                                JsonArray audiences = new JsonArray().addAll(classes).addAll(groups);

                                List<String> audienceIds = ((List<JsonObject>) audiences.getList())
                                        .stream()
                                        .map(audience -> audience.getString("id"))
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());

                                groupService.getGroupStudents(audienceIds, studentsEvt -> {
                                    if (studentsEvt.isLeft()) {
                                        LOGGER.error("[Presences@DefaultRegisterService::create] Failed to retrieve student groups for ids: "
                                                + new JsonArray(audienceIds).encode(), studentsEvt.left().getValue());
                                        handler.handle(new Either.Left<>(studentsEvt.left().getValue()));
                                        return;
                                    }

                                    JsonArray statements = new JsonArray();
                                    statements.add(getRegisterCreationStatement(id, register, user));
                                    for (int i = 0; i < classes.size(); i++) {
                                        String classId = classes.getJsonObject(i).getString("id");
                                        statements.add(getGroupCreationStatement(classId, GroupType.CLASS));
                                        statements.add(getRelRegisterGroupStatement(id, classId));
                                    }

                                    for (int i = 0; i < groups.size(); i++) {
                                        String groupId = groups.getJsonObject(i).getString("id");
                                        statements.add(getGroupCreationStatement(groupId, GroupType.GROUP));
                                        statements.add(getRelRegisterGroupStatement(id, groupId));
                                    }

                                    for (int i = 0; i < manualGroups.size(); i++) {
                                        String manualGroupId = manualGroups.getJsonObject(i).getString("id");
                                        statements.add(getGroupCreationStatement(manualGroupId, GroupType.GROUP));
                                        statements.add(getRelRegisterGroupStatement(id, manualGroupId));
                                    }

                                    List<String> students = extractStudentIdentifiers(studentsEvt.right().getValue());
                                    if (!students.isEmpty()) {
                                        register.put("id", id);
                                        statements.add(absenceToEventStatement(register, students, user));
                                    }

                                    Sql.getInstance().transaction(statements, event -> {
                                        Either<String, JsonObject> result = SqlResult.validUniqueResult(0, event);
                                        if (result.isLeft()) {
                                            String message = String.format("Failed to create register: %s", result.left().getValue());
                                            LOGGER.error(message, result.left().getValue());
                                            handler.handle(new Either.Left<>(message));
                                        } else {
                                            if (!students.isEmpty()) {
                                                // tricks to correct trigger that force our event counsellor regularisation with our reason.proving
                                                // we fix by SQL querying our event regularized with absences
                                                processEventStudent(register, students, handler, result);
                                            } else {
                                                handler.handle(result);
                                            }
                                        }
                                    });

                                });

                            });
                } catch (ClassCastException e) {
                    handler.handle(new Either.Left<>("[Presences@DefaultRegisterService] Failed cast next register identifier"));
                }
            }));
        });
    }

    private void processEventStudent(JsonObject register, List<String> students, Handler<Either<String, JsonObject>> handler,
                                     Either<String, JsonObject> result) {
        JsonArray params = new JsonArray();
        String query = "UPDATE presences.event SET counsellor_regularisation = (" +
                " SELECT absence.counsellor_regularisation FROM presences.absence WHERE absence.structure_id = ? " +
                " AND absence.student_id = event.student_id" +
                " AND absence.start_date <= ? AND absence.end_date >= ? LIMIT 1" +
                ")" +
                "WHERE register_id = ? AND event.student_id IN " + Sql.listPrepared(students.toArray()) + "";

        params.add(register.getString("structure_id"));
        params.add(register.getString("start_date"));
        params.add(register.getString("end_date"));
        params.add(register.getLong("id"));
        params.addAll(new JsonArray(students));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(event -> {
            if (event.isLeft()) {
                String message = "[Presences@DefaultRegisterService::processEventStudent] Failed to process student event " +
                        "for counsellor regularisation register " + event.left().getValue();
                LOGGER.error(message);
                handler.handle(new Either.Left<>(event.left().getValue()));
            } else {
                handler.handle(result);
            }
        }));
    }


    private void fetchIfRegisterExists(JsonObject register, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT id, structure_id, course_id, subject_id, start_date, end_date, counsellor_input, state_id " +
                "FROM " + Presences.dbSchema + ".register WHERE course_id = ? AND start_date = ? AND end_date = ?;";
        JsonArray params = new JsonArray()
                .add(register.getString("course_id"))
                .add(register.getString("start_date"))
                .add(register.getString("end_date"));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private List<String> extractStudentIdentifiers(JsonArray students) {
        if (students.isEmpty()) return new ArrayList<>();
        return ((List<JsonObject>) students.getList())
                .stream()
                .map(student -> student.getString("id"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private JsonObject absenceToEventStatement(JsonObject register, List<String> users, UserInfos user) {
        JsonArray params = new JsonArray();
        String query = "WITH absence as (SELECT absence.id, absence.start_date, absence.end_date, " +
                "absence.student_id, absence.reason_id, absence.owner, absence.counsellor_regularisation" +
                " FROM presences.absence WHERE absence.structure_id = ? AND absence.student_id IN " + Sql.listPrepared(users.toArray()) +
                " AND absence.start_date <= ?" +
                " AND absence.end_date >= ?)" +
                " INSERT INTO presences.event (start_date, end_date, comment, counsellor_input, " +
                "student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)" +
                " (SELECT ?, ?, '', ?, absence.student_id, ?, 1, absence.reason_id, CASE WHEN absence.owner " +
                " IS NULL THEN '' ELSE absence.owner END, absence.counsellor_regularisation FROM absence) ";

        params.add(register.getString("structure_id"));
        params.addAll(new JsonArray(users));
        params.add(register.getString("start_date"));
        params.add(register.getString("end_date"));
        params.add(register.getString("start_date"));
        params.add(register.getString("end_date"));
        params.add(true);
        params.add(register.getLong("id"));

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
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
        Future<JsonObject> registerFuture = Future.future();
        Future<JsonObject> registerGroupFuture = Future.future();

        fetchRegister(id, FutureHelper.handlerJsonObject(registerFuture));
        fetchGroupRegister(id, FutureHelper.handlerJsonObject(registerGroupFuture));

        CompositeFuture.all(registerFuture, registerGroupFuture).setHandler(registerAsync -> {
            if (registerAsync.failed()) {
                String message = "[Presences@DefaultRegisterService::get] Failed to retrieve register " + id;
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonObject register = registerFuture.result().put("groups", registerGroupFuture.result().getString("groups"));
            if (!register.containsKey("start_date")) {
                handler.handle(new Either.Left<>("404"));
                return;
            }
            String day;
            try {
                day = getDay(register);
            } catch (ParseException e) {
                handler.handle(new Either.Left<>("[Presences@DefaultRegisterService::get] Failed to parse register date"));
                return;
            }
            JsonArray groups = register.getString("groups") != null ? new JsonArray(register.getString("groups")) : new JsonArray();
            getUsers(groups, userEither -> {
                if (userEither.isLeft()) {
                    LOGGER.error("[Presences@DefaultRegisterService::get] Failed to retrieve users", userEither.left().getValue());
                    handler.handle(new Either.Left<>(userEither.left().getValue()));
                    return;
                }
                JsonArray users = userEither.right().getValue();
                List<String> userIds = new ArrayList<>();
                for (int i = 0; i < users.size(); i++) {
                    userIds.add(users.getJsonObject(i).getString("id"));
                }

                List<Future<JsonArray>> futures = new ArrayList<>();

                Future<JsonArray> lastAbsentsFuture = Future.future();
                Future<JsonArray> groupsNameFuture = Future.future();
                Future<JsonArray> teachersFuture = Future.future();
                Future<JsonArray> exemptionFuture = Future.future();
                Future<JsonArray> forgottenNotebookFuture = Future.future();
                Future<JsonArray> registerEventHistoryFuture = Future.future();

                futures.add(lastAbsentsFuture);
                futures.add(groupsNameFuture);
                futures.add(teachersFuture);
                futures.add(exemptionFuture);
                futures.add(forgottenNotebookFuture);
                futures.add(registerEventHistoryFuture);

                FutureHelper.all(futures).setHandler(asyncEvent -> {
                    if (asyncEvent.failed()) {
                        String message = "[Presences@DefaultRegisterService::get] Failed to retrieve groups users " +
                                "or last absents students";
                        LOGGER.error(message);
                        handler.handle(new Either.Left<>(message));
                        return;
                    }

                    JsonObject exemptionsMap = mapExemptions(exemptionFuture.result());
                    JsonArray lastAbsentUsers = reduce(lastAbsentsFuture.result(), "student_id");
                    JsonArray forgottenNotebooks = forgottenNotebookFuture.result();
                    JsonObject groupsNameMap = mapGroupsName(groupsNameFuture.result());

                    JsonArray events = registerEventHistoryFuture.result();
                    JsonObject historyMap = extractUsersEvents(events);

                    formatRegister(id, register, groups, users, teachersFuture, exemptionsMap, lastAbsentUsers,
                            forgottenNotebooks, groupsNameMap, historyMap);

                    matchSlots(register, register.getString("structure_id"), slotEvent -> {
                        if (slotEvent.isLeft()) {
                            String message = "[Presences@DefaultRegisterService::get] Failed to match slots";
                            LOGGER.error(message, slotEvent.left().getValue());
                            handler.handle(new Either.Left<>(message));
                        } else {
                            registerPresenceHelper.addOwnerToStudentEvents(slotEvent.right().getValue(), eventResult -> {
                                if (eventResult.failed()) {
                                    String message = "[Presences@DefaultRegisterService::get] Failed to add course or owner to student events";
                                    LOGGER.error(message);
                                    handler.handle(new Either.Left<>(message));
                                } else {
                                    handler.handle(new Either.Right<>(slotEvent.right().getValue()));
                                }
                            });
                        }
                    });
                });
                exemptionService.getRegisterExemptions(userIds, register.getString("structure_id"), register.getString("start_date"), register.getString("end_date"), FutureHelper.handlerJsonArray(exemptionFuture));
                getLastAbsentsStudent(register.getString("subject_id"),
                        DateHelper.getDateString(register.getString("start_date"), DateHelper.MONGO_FORMAT),
                        id,
                        FutureHelper.handlerJsonArray(lastAbsentsFuture)
                );
                notebookService.get(userIds, day, day, FutureHelper.handlerJsonArray(forgottenNotebookFuture));
                getGroupsName(groups, FutureHelper.handlerJsonArray(groupsNameFuture));
                getCourseTeachers(register.getString("course_id"), FutureHelper.handlerJsonArray(teachersFuture));
                registerHelper.getRegisterEventHistory(register.getString("structure_id"), day, null, new JsonArray(userIds),
                        FutureHelper.handlerJsonArray(registerEventHistoryFuture));
            });
        });
    }

    private void fetchRegister(Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT personnel_id, proof_id, course_id, owner, notified, subject_id, start_date, end_date, " +
                "structure_id, counsellor_input, state_id FROM " + Presences.dbSchema + ".register " +
                "WHERE register.id = ?";
        JsonArray params = new JsonArray().add(id);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private void fetchGroupRegister(Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT json_agg(\"group\".*) as groups FROM " + Presences.dbSchema + ".rel_group_register " +
                "INNER JOIN presences.\"group\" ON (rel_group_register.group_id = \"group\".id) " +
                "WHERE rel_group_register.register_id = ?";
        JsonArray params = new JsonArray().add(id);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private void formatRegister(Integer id, JsonObject register, JsonArray groups, JsonArray users, Future<JsonArray> teachersFuture,
                                JsonObject exemptionsMap, JsonArray lastAbsentUsers, JsonArray forgottenNotebooks,
                                JsonObject groupsNameMap, JsonObject historyMap) {
        JsonArray formattedUsers = new JsonArray();
        for (int i = 0; i < users.size(); i++) {
            JsonObject user = users.getJsonObject(i);
            boolean exempted = exemptionsMap.containsKey(user.getString("id"));
            String exempted_subjectId = exemptionsMap.containsKey(user.getString("id")) ? exemptionsMap.getJsonObject(user.getString("id")).getString("subject_id", "") : "0";
            Integer exemption_recursive_id = exemptionsMap.containsKey(user.getString("id")) ? exemptionsMap.getJsonObject(user.getString("id")).getInteger("recursive_id") : null;
            boolean exemption_attendance = exemptionsMap.containsKey(user.getString("id")) ? exemptionsMap.getJsonObject(user.getString("id")).getBoolean("attendance") : false;
            formattedUsers.add(formatStudent(id, user, historyMap.getJsonArray(user.getString("id"), new JsonArray()),
                    lastAbsentUsers.contains(user.getString("id")), groupsNameMap.getString(user.getString("groupId")),
                    exempted, exemption_recursive_id, exemption_attendance, exempted_subjectId, forgottenNotebooks));
        }
        register.put("students", formattedUsers);
        register.put("groups", groups);
        register.put("teachers", teachersFuture.result());
    }

    @Override
    public void exists(String courseId, String startDate, String endDate, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT id " +
                "FROM " + Presences.dbSchema + ".register " +
                "WHERE course_id = ? AND start_date = ? AND end_date = ?";

        JsonArray params = new JsonArray()
                .add(courseId)
                .add(startDate)
                .add(endDate);

        Sql.getInstance().prepared(query, params, message -> {
            Either<String, JsonObject> either = SqlResult.validUniqueResult(message);
            if (either.isLeft()) {
                LOGGER.error("[Presences@DefaultRegisterService] An error occurred when recovering register identifier", either.left().getValue());
                handler.handle(new Either.Left<>("[Presences@DefaultRegisterService] Failed to recover register identifier"));
                return;
            }
            Long count = SqlResult.countResult(message);
            JsonObject response = new JsonObject()
                    .put("exists", count != null && count > 0)
                    .put("id", either.right().getValue().getLong("id"));
            handler.handle(new Either.Right<>(response));
        });
    }

    @Override
    public void setNotified(Long registerId, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Presences.dbSchema + ".register SET notified = true " +
                "WHERE id = ?";

        JsonArray params = new JsonArray()
                .add(registerId);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    /**
     * Retrieve course teachers. Based on given course identifier, it returns user identifier, user name and user function
     *
     * @param courseId Course identifier
     * @param handler  Function handler returning data
     */
    private void getCourseTeachers(String courseId, Handler<Either<String, JsonArray>> handler) {
        JsonObject courseQuery = new JsonObject()
                .put("_id", courseId);

        MongoDb.getInstance().findOne("courses", courseQuery, message -> {
            Either<String, JsonObject> either = MongoDbResult.validResult(message);
            if (either.isLeft()) {
                LOGGER.error("[Presences@DefaultRegisterService] Failed to retrieve course");
                handler.handle(new Either.Left<>(either.left().getValue()));
                return;
            }

            JsonObject course = either.right().getValue();
            JsonArray teacherIds = course.getJsonArray("teacherIds", new JsonArray());

            String teacherQuery = "MATCH (u:User) WHERE u.id IN {teacherIds} RETURN u.id as id, (u.lastName + ' ' + u.firstName) as displayName, " +
                    "CASE WHEN u.functions IS NULL THEN [] ELSE EXTRACT(function IN u.functions | last(split(function, \"$\"))) END as functions";
            Neo4j.getInstance().execute(teacherQuery, new JsonObject().put("teacherIds", teacherIds), Neo4jResult.validResultHandler(handler));
        });
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

    private JsonObject mapExemptions(JsonArray exemptions) {
        JsonObject map = new JsonObject();
        for (int i = 0; i < exemptions.size(); i++) {
            map.put(exemptions.getJsonObject(i).getString("student_id"),
                    new JsonObject()
                            .put("attendance", exemptions.getJsonObject(i).getBoolean("attendance", false))
                            .put("recursive_id", exemptions.getJsonObject(i).getInteger("recursive_id", null))
                            .put("subject_id", exemptions.getJsonObject(i).getString("subject_id")));
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
     * Format user. It creates an object containing its identifier, its name, its group, its events and its event day history
     *
     * @param registerId           Register identifier. The function needs the register identifier to extract events that concern the current register.
     * @param student              Student
     * @param events               Student events list
     * @param lastCourseAbsent     Define if user was absent during last teacher course$
     * @param groupName            User group name
     * @param exempted             User exemption status
     * @param exemption_attendance User exemption attendance status. Sometime, the exemption needs attendance
     * @return Formatted student
     */
    private JsonObject formatStudent(Integer registerId, JsonObject student, JsonArray events, boolean lastCourseAbsent,
                                     String groupName, Boolean exempted, Integer exemption_recursive_id, Boolean exemption_attendance,
                                     String exempted_subjectId, JsonArray forgottenNotebooks) {
        JsonArray registerEvents = new JsonArray();
        for (int i = 0; i < events.size(); i++) {
            JsonObject event = events.getJsonObject(i);
            if (registerId.equals(event.getInteger("register_id"))) {
                registerEvents.add(event);
            }
        }
        JsonObject user = new JsonObject()
                .put("id", student.getString("id"))
                .put("name", student.getString("lastName") + " " + student.getString("firstName"))
                .put("birth_date", student.getString("birthDate"))
                .put("group", student.getString("groupId"))
                .put("group_name", groupName)
                .put("events", registerEvents)
                .put("last_course_absent", lastCourseAbsent)
                .put("forgotten_notebook", hasForgottenNotebook(student, forgottenNotebooks))
                .put("day_history", events)
                .put("exempted", exempted);


        if (exempted) {
            user.put("exemption_attendance", exemption_attendance);
            user.put("exempted_subjectId", exempted_subjectId);
            user.put("exemption_recursive_id", exemption_recursive_id);
        }

        return user;
    }

    private boolean hasForgottenNotebook(JsonObject student, JsonArray forgottenNotebooks) {
        boolean hasForgottenNotebook = false;
        for (int i = 0; i < forgottenNotebooks.size(); i++) {
            if (forgottenNotebooks.getJsonObject(i).getString("student_id").equals(student.getString("id"))) {
                hasForgottenNotebook = true;
            }
        }
        return hasForgottenNotebook;
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
        List<Future> groupsFutures = new ArrayList<>();
        List<Future> classesFutures = new ArrayList<>();
        List<String> groupIdentifiers = new ArrayList<>();
        List<String> classIdentifiers = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            JsonObject group = groups.getJsonObject(i);
            if ("CLASS".equals(group.getString("type"))) classIdentifiers.add(group.getString("id"));
            else groupIdentifiers.add(group.getString("id"));
        }

        if (!groupIdentifiers.isEmpty()) {
            Future future = Future.future();
            futures.add(future);
            groupsFutures.add(future);
            groupService.getFunctionalAndManualGroupsStudents(groupIdentifiers, FutureHelper.handlerJsonArray(future));
        }

        if (!classIdentifiers.isEmpty()) {
            Future future = Future.future();
            futures.add(future);
            classesFutures.add(future);
            groupService.getClassesStudents(classIdentifiers, FutureHelper.handlerJsonArray(future));
        }

        if (futures.isEmpty()) {
            handler.handle(new Either.Right<>(new JsonArray()));
            return;
        }

        CompositeFuture.all(futures).setHandler(event -> {
            if (event.failed()) {
                LOGGER.error(event.cause());
                handler.handle(new Either.Left<>(event.cause().toString()));
            } else {
                JsonArray res = new JsonArray();
                HashMap<String, Boolean> map = new HashMap<>();

                Consumer<Future> mapFunction = future -> {
                    JsonArray users = (JsonArray) future.result();
                    JsonObject user;
                    for (int j = 0; j < users.size(); j++) {
                        user = users.getJsonObject(j);
                        if (!map.containsKey(user.getString("id"))) {
                            map.put(user.getString("id"), true);
                            res.add(user);
                        }
                    }
                };

                if (!groupsFutures.isEmpty()) groupsFutures.forEach(mapFunction);
                if (!classesFutures.isEmpty()) classesFutures.forEach(mapFunction);

                handler.handle(new Either.Right<>(res));
            }
        });
    }

    private void getLastAbsentsStudent(String subject_id, String start_date, Integer registerIdentifier,
                                       Handler<Either<String, JsonArray>> handler) {

        String query = "WITH previous_register as (SELECT register.id as id " +
                "FROM presences.register " +
                "INNER JOIN presences.rel_group_register ON (register.id = rel_group_register.register_id) " +
                "WHERE register.subject_id = ? " +
                "AND rel_group_register.group_id IN ( " +
                "SELECT group_id " +
                "FROM presences.register " +
                "INNER JOIN presences.rel_group_register ON (register.id = rel_group_register.register_id) " +
                "WHERE register.id = ?) " +
                "AND register.id != ? AND register.start_date <= ? ORDER BY start_date DESC LIMIT 1) " +
                "SELECT student_id " +
                "FROM presences.event " +
                "INNER JOIN previous_register ON (previous_register.id = event.register_id) " +
                "AND type_id = ? ";

        JsonArray params = new JsonArray()
                .add(subject_id)
                .add(registerIdentifier)
                .add(registerIdentifier)
                .add(start_date)
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

        Viescolaire.getInstance().getSlotProfiles(structureId, event -> {
            JsonArray slots = new JsonArray();
            if (event.isLeft()) {
                LOGGER.error("[Presences@DefaultRegistrerService::matchSlots] Failed to retrieve slot profile");
            } else if (event.right().getValue().containsKey("slots") && !event.right().getValue().getJsonArray("slots").isEmpty()) {
                slots = event.right().getValue().getJsonArray("slots");
            }
            JsonArray students = register.getJsonArray("students");

            try {
                JsonArray clone = registerHelper.cloneSlots(slots, register.getString("start_date"));
                for (int i = 0; i < students.size(); i++) {
                    JsonObject student = students.getJsonObject(i);
                    JsonArray history = student.getJsonArray("day_history");

                    JsonArray userSlots = clone.copy();
                    if (history.size() == 0) {
                        student.put("day_history", userSlots);
                    } else {
                        student.put("day_history", registerHelper.mergeEventsSlots(student.getJsonArray("day_history"), userSlots));
                    }
                }
                handler.handle(new Either.Right<>(register));
            } catch (Exception e) {
                String message = "[Presences@DefaultRegisterService::matchSlots] Failed to parse slots";
                LOGGER.error(message, e);
                handler.handle(new Either.Left<>(message));
                return;
            }
        });
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
        String query = "INSERT INTO " + Presences.dbSchema +
                ".register (id, structure_id, personnel_id, course_id, state_id, owner, start_date, end_date, subject_id, split_slot) " +
                "VALUES (?, ?, ?, ?, 1, ?, ?, ?, ?, ?) " +
                "RETURNING id, structure_id, course_id, subject_id, start_date, end_date, counsellor_input, state_id;";

        JsonArray params = new JsonArray()
                .add(id)
                .add(register.getString("structure_id"))
                .add(user.getUserId())
                .add(register.getString("course_id"))
                .add(user.getUserId())
                .add(register.getString("start_date"))
                .add(register.getString("end_date"))
                .add(register.getString("subject_id"))
                .add(register.getBoolean("split_slot") != null ? register.getBoolean("split_slot") : true);

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

    @Override
    public void getLastForgottenRegistersCourses(String structureId, List<String> teacherIds, List<String> groupNames,
                                                 String startDate, String endDate, boolean multipleSlot,
                                                 Handler<AsyncResult<JsonArray>> handler) {

        getLastForgottenRegisters(structureId, startDate, endDate)
                .compose(registers -> getCoursesFromRegisters(structureId, registers, teacherIds, groupNames, multipleSlot))
                .setHandler(ar -> {
                    if (ar.failed()) {
                        String message = "[Presences@DefaultCourseService::getLastForgottenRegistersCourses] " +
                                "Error fetching courses with last forgotten registers: " + ar.cause().getMessage();
                        LOGGER.error(message);
                        handler.handle(Future.failedFuture(message));
                    } else {
                        handler.handle(Future.succeededFuture(ar.result()));
                    }
                });
    }

    private Future<JsonArray> getLastForgottenRegisters(String structureId, String startDate, String endDate) {
        Future<JsonArray> future = Future.future();
        String query = "SELECT id, start_date, end_date, course_id, state_id, notified, split_slot FROM "
                + Presences.dbSchema + ".register WHERE structure_id = ? AND state_id != 3 " +
                "AND start_date > ? AND start_date < ? ORDER BY start_date DESC";

        JsonArray params = new JsonArray();
        params.add(structureId)
                .add(startDate)
                .add(endDate);
        sql.prepared(query, params, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Presences@DefaultCourseService::getLastForgottenRegisters] Failed to get " +
                        "last forgotten registers.";
                LOGGER.error(message, result.left().getValue());
                future.fail(result.left().getValue());

            } else {
                future.complete(result.right().getValue());
            }
        }));

        return future;
    }

    @SuppressWarnings("unchecked")
    private Future<JsonArray> getCoursesFromRegisters(String structureId, JsonArray registers,
                                                      List<String> teacherIds, List<String> groupNames, boolean multipleSlot) {
        Promise<JsonArray> promise = Promise.promise();

        JsonArray courseIds = new JsonArray();

        for (int i = 0; i < registers.size(); i++) {
            courseIds.add(registers.getJsonObject(i).getString(Field.COURSE_ID));
        }

        courseHelper.getCoursesByIds(courseIds, res -> {
            if (res.isLeft()) {
                promise.fail(res.left().getValue());
            } else {
                JsonArray courses = res.right().getValue();
                Future<JsonArray> teachersFuture = courseHelper.formatCourseTeachersAndSubjects(courses);
                Promise<JsonArray> slotsFuture = Promise.promise();

                CompositeFuture.all(teachersFuture, slotsFuture.future())
                        .onFailure(fail -> promise.fail(fail.getCause().getMessage()))
                        .onSuccess(ar -> {
                            List<Slot> slots = SlotHelper.getSlotListFromJsonArray(slotsFuture.future().result(),
                                    Slot.MANDATORY_ATTRIBUTE);
                            List<Course> coursesEvent = CourseHelper.getCourseListFromJsonArray(courses,
                                    Course.MANDATORY_ATTRIBUTE);
                            List<Course> splitCoursesEvent = CourseHelper.splitCoursesFromSlot(coursesEvent, slots);

                            List<Course> squashCourses = SquashHelper.squash(coursesEvent, splitCoursesEvent,
                                    registers, multipleSlot);

                            promise.complete(filterLastForgottenRegisterCourses(squashCourses, teacherIds, groupNames,
                                    multipleSlot));
                        });

                Viescolaire.getInstance().getSlotsFromProfile(structureId, FutureHelper.handlerJsonArray(slotsFuture));
            }
        });

        return promise.future();
    }

    private JsonArray filterLastForgottenRegisterCourses(List<Course> courses, List<String> teacherIds,
                                                         List<String> groupNames, boolean multipleSlot) {
        final int numberRegisters = 16;
        courses = courses.stream().filter(course -> (teacherIds.isEmpty() || courseHasTeacherOfId(course.toJSON(), teacherIds))
                        && (groupNames.isEmpty() || courseHasClassOrGroupName(course.toJSON(), groupNames))
                        && !course.getTeachers().isEmpty()
                        && course.getRegisterId() != null
                        && course.isSplitSlot() == multipleSlot)
                .sorted((o1, o2) -> o2.getStartDate().compareToIgnoreCase(o1.getStartDate()))
                .limit(numberRegisters)
                .sorted((o1, o2) -> o1.getStartDate().compareToIgnoreCase(o2.getStartDate()))
                .collect(Collectors.toList());

        return new JsonArray(courses);
    }


    /**
     * Checks if course object contains a teacher w/ an id in teacherIds array.
     * @param course        the course object
     * @param teacherIds    lit of teacher identifiers
     */
    @SuppressWarnings("unchecked")
    private boolean courseHasTeacherOfId(JsonObject course, List<String> teacherIds) {
        JsonArray teachers = course.getJsonArray("teachers", new JsonArray());
        return ((List<JsonObject>) teachers.getList())
                .stream()
                .map(teacher -> teacher.getString("id"))
                .anyMatch(teacherIds::contains);
    }

    @SuppressWarnings("unchecked")
    private boolean courseHasClassOrGroupName(JsonObject course, List<String> groupNames) {
        JsonArray classes = course.getJsonArray("classes", new JsonArray());
        JsonArray groups = course.getJsonArray("groups", new JsonArray());
        return  ((List<String>) classes.getList()).stream().anyMatch(groupNames::contains) ||
                ((List<String>) groups.getList()).stream().anyMatch(groupNames::contains);
    }
}
