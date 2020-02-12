package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.helper.DisciplineHelper;
import fr.openent.presences.helper.PersonHelper;
import fr.openent.presences.helper.PresenceHelper;
import fr.openent.presences.model.Discipline;
import fr.openent.presences.model.Person.Student;
import fr.openent.presences.model.Person.User;
import fr.openent.presences.model.Presence.MarkedStudent;
import fr.openent.presences.model.Presence.Presence;
import fr.openent.presences.service.DisciplineService;
import fr.openent.presences.service.PresenceService;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.List;

public class DefaultPresenceService implements PresenceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPresenceService.class);
    private static final String defaultStartTime = "00:00:00";
    private static final String defaultEndTime = "23:59:59";

    private DisciplineService disciplineService;
    private PersonHelper personHelper;
    private PresenceHelper presenceHelper;
    private UserService userService;

    public DefaultPresenceService() {
        this.disciplineService = new DefaultDisciplineService();
        this.personHelper = new PersonHelper();
        this.presenceHelper = new PresenceHelper();
        this.userService = new DefaultUserService();
    }

    @Override
    public void get(String structureId, String startDate, String endDate, List<String> userId,
                    List<String> ownerIds, Handler<Either<String, JsonArray>> handler) {

        Future<JsonArray> presencesFuture = Future.future();
        Future<JsonArray> disciplinesFuture = Future.future();

        CompositeFuture.all(presencesFuture, disciplinesFuture).setHandler(presenceAsync -> {
            if (presenceAsync.failed()) {
                String message = "[Presences@DefaultPresenceService] Failed to fetch presences or disciplines";
                LOGGER.error(message + presenceAsync.cause());
                handler.handle(new Either.Left<>(message + presenceAsync.cause()));
            } else {
                List<Presence> presences = PresenceHelper.getPresenceListFromJsonArray(presencesFuture.result());
                List<Discipline> disciplines = DisciplineHelper.getDisciplineListFromJsonArray(disciplinesFuture.result());

                List<String> usersId = new ArrayList<>();
                List<Integer> presenceIds = new ArrayList<>();

                for (Presence presence : presences) {
                    usersId.add(presence.getOwner().getId());
                    presenceIds.add(presence.getId());
                }

                Future<JsonObject> userInfoFuture = Future.future();
                Future<JsonObject> disciplineActionFuture = Future.future();

                CompositeFuture.all(userInfoFuture, disciplineActionFuture).setHandler(dataAsync -> {
                    if (dataAsync.failed()) {
                        String message = "[Presences@DefaultPresenceService] Failed to manipulate data while " +
                                "using discipline and user info";
                        LOGGER.error(message + dataAsync.cause());
                        handler.handle(new Either.Left<>(message + dataAsync.cause()));
                    } else {
                        handler.handle(new Either.Right<>(PresenceHelper.toPresencesJsonArray(presences)));
                    }
                });
                interactUserInfo(structureId, presences, usersId, presenceIds, userInfoFuture);
                presenceHelper.addDisciplineToPresence(presences, disciplines, disciplineActionFuture);
            }
        });
        fetchPresence(structureId, startDate, endDate, userId, ownerIds, FutureHelper.handlerJsonArray(presencesFuture));
        disciplineService.get(structureId, FutureHelper.handlerJsonArray(disciplinesFuture));
    }

    private void interactUserInfo(String structureId, List<Presence> presences, List<String> usersId,
                                  List<Integer> presenceIds, Future<JsonObject> future) {

        if (presenceIds.isEmpty()) {
            future.complete();
            return;
        }
        Future<List<MarkedStudent>> markedStudentsFuture = Future.future();
        Future<JsonArray> ownerFuture = Future.future();

        CompositeFuture.all(markedStudentsFuture, ownerFuture).setHandler(userAsync -> {
            if (userAsync.failed()) {
                String message = "[Presences@DefaultPresenceService] Failed to fetch user or students info";
                LOGGER.error(message + userAsync.cause());
                future.fail(message + userAsync.cause());
            } else {
                Future<JsonObject> actionMarkedStudentsFuture = Future.future();
                Future<JsonObject> actionOwnerFuture = Future.future();

                List<MarkedStudent> markedStudents = markedStudentsFuture.result();
                List<User> users = personHelper.getUserListFromJsonArray(ownerFuture.result());

                CompositeFuture.all(actionMarkedStudentsFuture, actionOwnerFuture).setHandler(finalResultAsync -> {
                    if (finalResultAsync.failed()) {
                        String message = "[Presences@DefaultPresenceService] Failed to add user or students to presences";
                        LOGGER.error(message + finalResultAsync.cause());
                        future.fail(message + finalResultAsync.cause());
                    } else {
                        future.complete();
                    }
                });
                presenceHelper.addMarkedStudentsToPresence(presences, markedStudents, actionMarkedStudentsFuture);
                presenceHelper.addOwnerToPresence(presences, users, actionOwnerFuture);
            }
        });

        fetchPresenceStudents(structureId, presenceIds, markedStudentsFuture);
        userService.getUsers(usersId, FutureHelper.handlerJsonArray(ownerFuture));
    }

    private void fetchPresence(String structureId, String startDate, String endDate, List<String> userId,
                               List<String> ownerIds, Handler<Either<String, JsonArray>> handler) {

        JsonArray params = new JsonArray();
        String query = "SELECT * FROM " + Presences.dbSchema + ".presence WHERE structure_id = ? " +
                "AND (start_date > ? AND end_date < ?) ";
        params.add(structureId).add(startDate + " " + defaultStartTime).add(endDate + " " + defaultEndTime);

        /* filtering owner ids fetched */
        if (!ownerIds.isEmpty()) {
            query += " AND owner IN " + Sql.listPrepared(ownerIds) + " ";
            params.addAll(new JsonArray(ownerIds));
        }

        /* filtering user ids fetched */
        if (!userId.isEmpty()) {
            query += " AND EXISTS (" +
                    " SELECT * FROM presences.presence_student ps" +
                    " WHERE ps.presence_id = presence.id" +
                    " AND ps.student_id IN " + Sql.listPrepared(userId) + " )";
            params.addAll(new JsonArray(userId));
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    private void fetchPresenceStudents(String structureId, List<Integer> presenceIds,
                                       Handler<AsyncResult<List<MarkedStudent>>> handler) {

        String query = "SELECT * FROM " + Presences.dbSchema + ".presence_student " +
                "WHERE presence_id IN " + Sql.listPrepared(presenceIds);
        JsonArray params = new JsonArray().addAll(new JsonArray(presenceIds));

        /* Query presence_students */
        Sql.getInstance().prepared(query, params, presenceStudentsAsync -> {
            Either<String, JsonArray> result = SqlResult.validResult(presenceStudentsAsync);
            if (result.isLeft()) {
                String message = "[Presences@DefaultPresenceService] Failed to fetch presences students";
                LOGGER.error(message + result.left().getValue());
                handler.handle(Future.failedFuture(message + result.left().getValue()));
            }
            List<MarkedStudent> markedStudents = PresenceHelper.getMarkedStudentListFromJsonArray(
                    result.right().getValue()
            );
            List<String> studentIds = new ArrayList<>();
            for (MarkedStudent markedStudent : markedStudents) {
                if (!studentIds.contains(markedStudent.getStudent().getId())) {
                    studentIds.add(markedStudent.getStudent().getId());
                }
            }
            /* Query students info neo4j with studentIds */
            personHelper.getStudentsInfo(structureId, studentIds, studentAsync -> {
                if (studentAsync.isLeft()) {
                    String message = "[Presences@DefaultPresenceService] Failed to fetch students";
                    LOGGER.error(message + studentAsync.left().getValue());
                    handler.handle(Future.failedFuture(message + studentAsync.left().getValue()));
                }
                List<Student> students = personHelper.getStudentListFromJsonArray(
                        studentAsync.right().getValue(), Student.MANDATORY_ATTRIBUTE
                );
                for (MarkedStudent markedStudent : markedStudents) {
                    for (Student student : students) {
                        if (markedStudent.getStudent().getId().equals(student.getId())) {
                            markedStudent.setStudent(student);
                        }
                    }
                }
                handler.handle(Future.succeededFuture(markedStudents));
            });
        });
    }

    @Override
    public void create(UserInfos user, JsonObject presenceBody, Handler<Either<String, JsonObject>> handler) {
        String queryId = "SELECT nextval('" + Presences.dbSchema + ".presence_id_seq') as id";

        Sql.getInstance().raw(queryId, SqlResult.validUniqueResultHandler(presenceId -> {
            if (presenceId.isLeft()) {
                String message = "[Presences@DefaultPresenceService] Failed to fetch presence id seq";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            }
            Integer id = presenceId.right().getValue().getInteger("id");

            JsonArray statements = new JsonArray();
            statements.add(createPresenceStatement(user, id, presenceBody));
            for (int i = 0; i < presenceBody.getJsonArray("markedStudents").size(); i++) {
                JsonObject student = presenceBody.getJsonArray("markedStudents").getJsonObject(i);
                statements.add(addStudentsStatement(id, student.getString("studentId"), student.getString("comment")));
            }
            Sql.getInstance().transaction(statements, async -> {
                Either<String, JsonObject> result = SqlResult.validUniqueResult(0, async);
                if (result.isLeft()) {
                    String message = "[Presences@DefaultPresenceService] Failed to execute presence creation statements";
                    LOGGER.error(message);
                    handler.handle(new Either.Left<>(message));
                }
                handler.handle(new Either.Right<>(result.right().getValue()));
            });
        }));
    }

    /**
     * statement that create Presence
     *
     * @param id       presence identifier
     * @param presence presence object
     * @return Statement
     */
    private JsonObject createPresenceStatement(UserInfos user, Number id, JsonObject presence) {
        String query = "INSERT INTO " + Presences.dbSchema + ".presence" +
                " (id, start_date, end_date, discipline_id, owner, structure_id)" +
                " VALUES (?, ?, ?, ?, ?, ?)";
        JsonArray values = new JsonArray()
                .add(id)
                .add(presence.getString("startDate"))
                .add(presence.getString("endDate"))
                .add(presence.getInteger("disciplineId"))
                .add(user.getUserId())
                .add(presence.getString("structureId"));

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    /**
     * statement that add marked student in presence
     *
     * @param presenceId presence identifier
     * @param studentId  student identifier
     * @param comment    student's comment
     * @return Statement of a marked student added
     */
    private JsonObject addStudentsStatement(Number presenceId, String studentId, String comment) {
        String query = "INSERT INTO " + Presences.dbSchema + ".presence_student" +
                " (student_id, comment, presence_id) VALUES (?, ?, ?)";
        JsonArray values = new JsonArray()
                .add(studentId)
                .add(comment)
                .add(presenceId);

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    @Override
    public void update(JsonObject presenceBody, Handler<Either<String, JsonObject>> handler) {
        Integer presenceId = presenceBody.getInteger("id");
        JsonArray markedStudents = presenceBody.getJsonArray("markedStudents");

        JsonArray statements = new JsonArray();
        statements.add(updatePresenceStatement(presenceBody));
        statements.add(eraseStudentsFromPresenceStatement(presenceId));
        for (int i = 0; i < markedStudents.size(); i++) {
            JsonObject student = markedStudents.getJsonObject(i);
            statements.add(addStudentsStatement(presenceId, student.getString("studentId"), student.getString("comment")));
        }
        Sql.getInstance().transaction(statements, updateAsync -> {
            Either<String, JsonObject> result = SqlResult.validUniqueResult(0, updateAsync);
            if (result.isLeft()) {
                String message = "[Presences@DefaultPresenceService] Failed to execute presence update statements";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            }
            handler.handle(new Either.Right<>(result.right().getValue()));
        });
    }

    /**
     * statement that update Presence
     *
     * @param presenceBody presence body object
     */
    private JsonObject updatePresenceStatement(JsonObject presenceBody) {
        String query = "UPDATE " + Presences.dbSchema + ".presence SET " +
                "start_date = ?, end_date = ?, structure_id = ?, discipline_id = ? WHERE id = ?";
        JsonArray values = new JsonArray()
                .add(presenceBody.getString("startDate"))
                .add(presenceBody.getString("endDate"))
                .add(presenceBody.getString("structureId"))
                .add(presenceBody.getInteger("disciplineId"))
                .add(presenceBody.getInteger("id"));

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    /**
     * statement that update Presence
     *
     * @param presenceId Presence Identifier
     */
    private JsonObject eraseStudentsFromPresenceStatement(Integer presenceId) {
        String query = "DELETE FROM " + Presences.dbSchema + ".presence_student where presence_id = ?";
        JsonArray values = new JsonArray().add(presenceId);
        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    @Override
    public void delete(String presenceId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Presences.dbSchema + ".presence where id = ? RETURNING id as id_deleted";
        JsonArray values = new JsonArray().add(Integer.parseInt(presenceId));
        Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(handler));
    }
}
