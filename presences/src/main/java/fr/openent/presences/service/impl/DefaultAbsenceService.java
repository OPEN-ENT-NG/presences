package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.constants.Reasons;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DBService;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.helper.AbsenceHelper;
import fr.openent.presences.model.Absence;
import fr.openent.presences.model.Event.Event;
import fr.openent.presences.service.AbsenceService;
import fr.openent.presences.service.CommonPresencesServiceFactory;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DefaultAbsenceService extends DBService implements AbsenceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAbsenceService.class);
    private static final String defaultStartTime = "00:00:00";
    private static final String defaultEndTime = "23:59:59";

    private final GroupService groupService;
    private final UserService userService;
    private final CommonPresencesServiceFactory commonPresencesServiceFactory;


    public DefaultAbsenceService(EventBus eb) {
        this.groupService = new DefaultGroupService(eb);
        this.userService = new DefaultUserService();

        // todo spread new class CommonPresencesServiceFactory toward other classes that use
        this.commonPresencesServiceFactory = null;
    }

    public DefaultAbsenceService(CommonPresencesServiceFactory commonPresencesServiceFactory) {
        this.groupService = commonPresencesServiceFactory.groupService();
        this.userService = commonPresencesServiceFactory.userService();
        this.commonPresencesServiceFactory = commonPresencesServiceFactory;
    }

    @Override
    public void get(String structureId, String startDate, String endDate,
                    List<String> users, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence";

        query += " WHERE structure_id = ? AND start_date > ? AND end_date < ? OR ? > start_date ";
        params.add(structureId)
                .add(startDate + " " + defaultStartTime)
                .add(endDate + " " + defaultEndTime)
                .add(endDate + " " + defaultEndTime);
        if (!users.isEmpty()) {
            query += " AND student_id IN " + Sql.listPrepared(users.toArray());
            params.addAll(new JsonArray(users));
        }
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public Future<JsonArray> get(String structureId, String startDate, String endDate, List<String> users) {
        Promise<JsonArray> promise = Promise.promise();
        JsonArray params = new JsonArray();
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence" +
                " WHERE structure_id = ? AND ? <= end_date AND start_date <= ?";

        params.add(structureId);
        params.add(startDate);
        params.add(endDate);

        if (!users.isEmpty()) {
            query += " AND student_id IN " + Sql.listPrepared(users.toArray());
            params.addAll(new JsonArray(users));
        }

        sql.prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(promise)));

        return promise.future();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Future<JsonObject> get(String structureId, String teacherId, List<String> audienceIds, List<String> studentIds,
                                  List<Integer> reasonIds, String startAt, String endAt,
                                  Boolean regularized, Boolean noReason, Boolean followed, Boolean halfBoarder, Boolean internal, Integer page) {
        Promise<JsonObject> promise = Promise.promise();

        List<String> filteredAudienceIds = new ArrayList<>();
        groupService.getGroupsAndClassesFromTeacherId(teacherId, structureId)
                .compose(restrictedAudienceIds -> {
                    filteredAudienceIds.addAll(
                            teacherId != null ? audienceIds.stream()
                                    .filter(restrictedAudienceIds::contains).collect(Collectors.toList()) :
                                    audienceIds
                    );
                    return groupService.getGroupStudents(teacherId != null ? restrictedAudienceIds : audienceIds);
                })
                .compose(resStudents -> {
                    if (teacherId != null && resStudents.isEmpty()) return Future.succeededFuture(new JsonArray());

                    List<String> filteredStudentIds = getFilteredStudentIds(teacherId, filteredAudienceIds,
                            resStudents.getList(), studentIds);

                    return userService.getStudents(structureId, filteredStudentIds, halfBoarder, internal);
                })
                .compose(students -> getWithPaginate(structureId, teacherId, students.getList(), startAt, endAt, regularized,
                        noReason, followed, halfBoarder, internal, reasonIds, page))
                .onFailure(err -> {
                    String message = String.format("[Presences@%s::getWithPaginate] Failed to get absences ",
                            this.getClass().getSimpleName());
                    LOGGER.error(message);
                    promise.fail(message);
                })
                .onSuccess(result -> promise.complete((JsonObject) result));

        return promise.future();
    }

    private List<String> getFilteredStudentIds(String teacherId, List<String> filteredAudienceIds, List<JsonObject> students, List<String> studentIds) {
        if (teacherId != null)
            return students
                    .stream()
                    .filter(student -> (studentIds.isEmpty() || studentIds.contains(student.getString(Field.ID)))
                            && (filteredAudienceIds.isEmpty() ||
                            filteredAudienceIds.contains(student.getString(Field.GROUPID))))
                    .map(student -> student.getString(Field.ID))
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

        studentIds.addAll(students.stream()
                .map(student -> student.getString(Field.ID))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));

        return studentIds;
    }

    @SuppressWarnings("unchecked")
    private Future<JsonObject> getWithPaginate(String structureId, String teacherId, List<JsonObject> students,
                                               String startAt, String endAt, Boolean regularized, Boolean noReason,
                                               Boolean followed, Boolean halfBoarder, Boolean internal,
                                               List<Integer> reasonIds, Integer page) {
        Promise<JsonObject> promise = Promise.promise();

        List<String> filterStudentIds = students.stream()
                .map(student -> student.getString(Field.ID))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        boolean canRetrieveAbsences = (teacherId == null && !Boolean.TRUE.equals(halfBoarder) && !Boolean.TRUE.equals(internal))
                || !students.isEmpty();

        Future<JsonArray> absencesFuture = !canRetrieveAbsences ? Future.succeededFuture(new JsonArray()) :
                retrieve(structureId, filterStudentIds, startAt, endAt, regularized,
                        noReason, followed, reasonIds, page);

        Future<JsonObject> countFuture = (page == null || !canRetrieveAbsences) ? Future.succeededFuture(new JsonObject()) :
                count(structureId, filterStudentIds, startAt, endAt, regularized, noReason, followed, reasonIds);

        CompositeFuture.all(absencesFuture, countFuture)
                .compose(futures -> {
                    List<String> ownerIds = ((List<JsonObject>) (absencesFuture.result()).getList())
                            .stream()
                            .map(absence -> absence.getString(Field.OWNER))
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());

                    return userService.getUsers(ownerIds);
                })
                .onFailure(err -> {
                    String message = String.format("[Presences@%s::getWithPaginate] Failed to get absences ",
                            this.getClass().getSimpleName());
                    LOGGER.error(String.format("%s %s", message, err.getMessage()));
                    promise.fail(message);
                })
                .onSuccess(ownersArray -> {
                    List<JsonObject> owners = ownersArray.getList();
                    List<JsonObject> values = absencesFuture.result().getList();
                    values.forEach(absence -> mapAbsence(absence, owners, students));

                    Integer pageCount = countFuture.result().getInteger(Field.COUNT, 0)
                            .equals(Presences.PAGE_SIZE) ? 0
                            : countFuture.result().getInteger(Field.COUNT, 0) / Presences.PAGE_SIZE;

                    promise.complete(new JsonObject()
                            .put(Field.PAGE, page)
                            .put(Field.PAGE_COUNT, pageCount)
                            .put(Field.ALL, values));
                });
        return promise.future();
    }

    private JsonObject mapAbsence(JsonObject absence, List<JsonObject> owners, List<JsonObject> students) {
        JsonObject owner = owners.stream()
                .filter(currentOwner -> absence.getString(Field.OWNER) != null
                        && absence.getString(Field.OWNER).equals(currentOwner.getString(Field.ID)))
                .findFirst()
                .orElse(null);

        JsonObject student = students.stream()
                .filter(currentStudent -> absence.getString(Field.STUDENT_ID) != null
                        && absence.getString(Field.STUDENT_ID).equals(currentStudent.getString(Field.ID)))
                .findFirst()
                .orElse(null);

        return absence
                .put(Field.OWNER, owner)
                .put(Field.STUDENT, student);
    }

    @Override
    public void getAbsenceInEvents(String structureId, String startDate, String endDate,
                                   List<String> users, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence";

        query += " WHERE absence.structure_id = ? " +
                " AND (start_date > ? AND end_date < ? OR ? > start_date)";
        params.add(structureId)
                .add(startDate + " " + defaultStartTime)
                .add(endDate + " " + defaultEndTime)
                .add(endDate + " " + defaultEndTime);
        if (!users.isEmpty()) {
            query += " AND student_id IN " + Sql.listPrepared(users.toArray());
            params.addAll(new JsonArray(users));
        }
        query += " AND absence.student_id IN (" +
                " SELECT distinct event.student_id FROM " + Presences.dbSchema + ".event" +
                " WHERE absence.start_date::date = event.start_date::date" +
                " AND absence.end_date::date = event.end_date::date" +
                " ) ";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getAbsenceId(Integer absenceId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence WHERE id = " + absenceId;
        sql.raw(query, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getAbsencesBetween(String startDate, String endDate, List<String> users, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence" +
                " WHERE student_id IN " + Sql.listPrepared(users.toArray()) +
                " AND (? >= start_date OR ? < end_date)" +
                " AND (end_date <= ? OR ? > start_date)";

        params.addAll(new JsonArray(users));
        params.add(startDate + " " + defaultStartTime);
        params.add(startDate + " " + defaultStartTime);
        params.add(endDate + " " + defaultEndTime);
        params.add(endDate + " " + defaultEndTime);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getAbsencesBetweenDates(String startDate, String endDate, List<String> users, String structureId,
                                        Handler<Either<String, JsonArray>> handler) {

        JsonArray params = new JsonArray();
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence" +
                " WHERE student_id IN " + Sql.listPrepared(users.toArray()) +
                " AND ? < end_date" +
                " AND start_date < ? ";

        params.addAll(new JsonArray(users));
        params.add(startDate);
        params.add(endDate);

        if (structureId != null) {
            query += " AND structure_id = ?";
            params.add(structureId);
        }

        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public Future<JsonArray> getAbsencesBetweenDates(String startDate, String endDate, List<String> users, String structureId) {
        Promise<JsonArray> promise = Promise.promise();
        getAbsencesBetweenDates(startDate, endDate, users, structureId, FutureHelper.handlerJsonArray(promise));
        return promise.future();
    }

    @Override
    public void getAbsencesBetweenDates(String startDate, String endDate, List<String> users,
                                        Handler<Either<String, JsonArray>> handler) {
        getAbsencesBetweenDates(startDate, endDate, users, null, handler);
    }

    @Override
    public void getAbsencesFromCollective(String structureId, Long collectiveId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence " +
                " WHERE structure_id = ? AND collective_id = ? ";

        JsonArray params = new JsonArray().add(structureId);
        params.add(collectiveId);

        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject absenceBody, List<String> studentIds, UserInfos user, Long collectiveId, Handler<AsyncResult<JsonArray>> handler) {
        create(absenceBody, studentIds, user.getUserId(), collectiveId, handler);
    }

    @Override
    public void create(JsonObject absenceBody, List<String> studentIds, String userId, Long collectiveId, Handler<AsyncResult<JsonArray>> handler) {
        if (studentIds.isEmpty()) {
            handler.handle(Future.succeededFuture(new JsonArray()));
            return;
        }

        JsonArray statements = new JsonArray();
        for (String studentId : studentIds) {
            statements.add(insertStatement(absenceBody, studentId, userId, collectiveId));
        }
        sql.transaction(statements, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Presences@DefaultAbsenceService::create] failed to create absences from collective";
                LOGGER.error(message, result.left().getValue());
                handler.handle(Future.failedFuture(message));
                return;
            }

            if (Boolean.TRUE.equals(absenceBody.getBoolean("counsellor_regularisation", false)))
                regularizeAfterCollectivePersist(collectiveId, handler);
            else handler.handle(Future.succeededFuture(result.right().getValue()));
        }));
    }

    private JsonObject insertStatement(JsonObject absenceBody, String studentId, String userId, Long collectiveId) {
        String query = "INSERT INTO " + Presences.dbSchema + ".absence(structure_id, student_id, start_date, " +
                "end_date, owner, reason_id, followed," +
                " collective_id) " +
                " SELECT ?, ?, ?, ?, ?, ?, ?, ? ";

        JsonArray params = new JsonArray()
                .add(absenceBody.getString("structure_id"))
                .add(studentId)
                .add(absenceBody.getString("start_date"))
                .add(absenceBody.getString("end_date"))
                .add(userId);

        if (absenceBody.getLong("reason_id") != null) params.add(absenceBody.getLong("reason_id"));
        else params.addNull();

        params.add(absenceBody.getBoolean("followed", false));

        if (collectiveId != null) {
            params.add(collectiveId);
            query += " WHERE NOT EXISTS " +
                    "        ( " +
                    "            SELECT id " +
                    "            FROM presences.absence " +
                    "                WHERE collective_id = ? " +
                    "                AND student_id = ? " +
                    "        ) ";
            params.add(collectiveId)
                    .add(studentId);
        } else params.addNull();

        return new JsonObject()
                .put("action", "prepared")
                .put("statement", query)
                .put("values", params);
    }

    private void regularizeAfterCollectivePersist(Long collectiveId, Handler<AsyncResult<JsonArray>> handler) {
        String query = "UPDATE " + Presences.dbSchema + ".absence " +
                "SET counsellor_regularisation = ? WHERE collective_id = ?";
        JsonArray params = new JsonArray()
                .add(true)
                .add(collectiveId);
        sql.prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(handler)));
    }

    @Override
    public void create(JsonObject absenceBody, UserInfos user, boolean editEvents, Handler<Either<String, JsonObject>> handler) {

        String startDate = absenceBody.getString(Field.START_DATE);
        String endDate = absenceBody.getString(Field.END_DATE);
        String studentId = absenceBody.getString(Field.STUDENT_ID);
        String structureId = absenceBody.getString(Field.STRUCTURE_ID);
        checkPresenceEvent(startDate, endDate, studentId, structureId, absenceBody)
                .compose(aVoid -> deleteOldStudentAbsences(studentId, structureId, startDate, endDate))
                .onFailure(error -> handler.handle(new Either.Left<>(error.getMessage())))
                .onSuccess(aVoid -> {
                    String query = "INSERT INTO " + Presences.dbSchema + ".absence(structure_id, start_date, end_date, student_id, owner, reason_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?) RETURNING id;";
                    JsonArray params = new JsonArray()
                            .add(structureId)
                            .add(startDate)
                            .add(endDate)
                            .add(studentId)
                            .add(user.getUserId());
                    if (absenceBody.getInteger("reason_id") != null) {
                        params.add(absenceBody.getInteger("reason_id"));
                    } else {
                        params.addNull();
                    }

                    sql.prepared(query, params, SqlResult.validUniqueResultHandler(absenceResult ->
                            afterPersistAbsence(absenceResult.isRight() ? absenceResult.right().getValue().getLong("id") : null,
                                    absenceBody,
                                    null,
                                    editEvents,
                                    user.getUserId(),
                                    handler,
                                    absenceResult
                            )
                    ));
                });
    }

    @SuppressWarnings("unchecked")
    private Future<Void> checkPresenceEvent(String startDate, String endDate, String studentId, String structureId, JsonObject absenceBody) {
        Promise<Void> promise = Promise.promise();
        String formattedStartDate = DateHelper.getDateString(startDate, DateHelper.YEAR_MONTH_DAY);
        String formattedEndDate = DateHelper.getDateString(endDate, DateHelper.YEAR_MONTH_DAY);
        commonPresencesServiceFactory.presenceService()
                .fetchPresence(structureId, formattedStartDate, formattedEndDate, new ArrayList<>(Collections.singletonList(studentId)),
                        new ArrayList<>(), new ArrayList<>())
                .compose(presences -> {
                    Promise<Void> presencePromise = Promise.promise();
                    boolean containPresenceInEvent = ((List<JsonObject>) presences.getList())
                            .stream()
                            .anyMatch(presence -> {
                                try {
                                    return DateHelper.isBetween(
                                            presence.getString(Field.START_DATE),
                                            presence.getString(Field.END_DATE),
                                            startDate,
                                            endDate,
                                            DateHelper.SQL_FORMAT,
                                            DateHelper.MONGO_FORMAT
                                    );
                                } catch (ParseException err) {
                                    String message = String.format("[Presences@%s::checkPresenceEvent::isBetween] Failed to parse: %s",
                                            this.getClass().getSimpleName(), err.getMessage());
                                    LOGGER.error(message, err);
                                    return false;
                                }
                            });
                    if (DateHelper.getDaysBetweenTwoDates(formattedStartDate, formattedEndDate) == 0 && containPresenceInEvent) {
                        absenceBody.put(Field.REASON_ID, Reasons.PRESENT_IN_STRUCTURE);
                    }
                    presencePromise.complete();
                    return presencePromise.future();
                })
                .onSuccess(promise::complete)
                .onFailure(err -> {
                    String message = String.format("[Presences@%s::checkPresenceEvent] Failed to check potential " +
                            "presences in absence creation : %s", this.getClass().getSimpleName(), err.getMessage());
                    LOGGER.error(message, err);
                    promise.fail(err.getMessage());
                });

        return promise.future();
    }

    private Future<Void> deleteOldStudentAbsences(String studentId, String structureId, String startDate, String endDate) {

        Promise<Void> promise = Promise.promise();

        List<String> studentIdList = new ArrayList<>();
        studentIdList.add(studentId);

        getAbsencesBetweenDates(startDate, endDate, studentIdList, structureId, absencesRes -> {

            if (absencesRes.isLeft()) {
                String message = "[Presences@DefaultAbsenceService::deleteOldStudentAbsences] failed to retrieve absences for student";
                LOGGER.error(message, absencesRes.left().getValue());
                promise.fail(message);
            } else {
                List<Absence> absences = AbsenceHelper.getAbsenceListFromJsonArray(absencesRes.right().getValue(), new ArrayList<>());

                List<Future> futures = new ArrayList<>();

                for (Absence absence : absences) {
                    Promise<JsonObject> promiseAbs = Promise.promise();
                    futures.add(promiseAbs.future());
                    delete(absence.getId(), FutureHelper.handlerJsonObject(promiseAbs));
                }

                CompositeFuture.join(futures)
                        .onFailure(fail -> {
                            String message = "[Presences@DefaultAbsenceService::deleteOldStudentAbsences] " +
                                    "failed to delete absences for student";
                            LOGGER.error(message, fail.getMessage());
                            promise.fail(fail.getMessage());
                        })
                        .onSuccess(unused -> promise.complete());
            }

        });
        return promise.future();
    }

    @Override
    public void update(Long absenceId, JsonObject absenceBody, UserInfos user, boolean editEvents, Handler<Either<String, JsonObject>> handler) {
        update(absenceId, absenceBody, user.getUserId(), editEvents, handler);
    }

    @Override
    public void update(Long absenceId, JsonObject absenceBody, String userInfoId, boolean editEvents, Handler<Either<String, JsonObject>> handler) {
        String beforeUpdateAbsenceQuery = "SELECT * FROM " + Presences.dbSchema + ".absence WHERE id = ? AND structure_id = ?";
        Sql.getInstance().prepared(beforeUpdateAbsenceQuery, new JsonArray().add(absenceId).add(absenceBody.getString(Field.STRUCTURE_ID)),
                SqlResult.validUniqueResultHandler(oldAbsenceResult -> {
                    if (oldAbsenceResult.isLeft() || oldAbsenceResult.right().getValue().isEmpty()) {
                        String message = String.format("[Presences@%s::update] failed to retrieve absence", this.getClass().getSimpleName());
                        LOGGER.error(String.format("%s %s", message, oldAbsenceResult.isLeft() ? oldAbsenceResult.isLeft() : ""));
                        handler.handle(new Either.Left<>(message));
                        return;
                    }
                    JsonObject oldAbsence = oldAbsenceResult.right().getValue();

                    String query = "UPDATE " + Presences.dbSchema + ".absence " +
                            " SET start_date = ?, end_date = ?, reason_id = ? " +
                            " WHERE id = ? AND structure_id = ?";


                    JsonArray values = new JsonArray()
                            .add(absenceBody.getString(Field.START_DATE))
                            .add(absenceBody.getString(Field.END_DATE));

                    if (absenceBody.getInteger(Field.REASON_ID) != null) {
                        values.add(absenceBody.getInteger(Field.REASON_ID));
                    } else {
                        values.addNull();
                    }
                    values
                            .add(absenceId)
                            .add(absenceBody.getString(Field.STRUCTURE_ID));

                    Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(absenceResult ->
                            afterPersistAbsence(absenceId, absenceBody, oldAbsence, editEvents, userInfoId, handler, absenceResult)));
                }));
    }

    @Override
    @SuppressWarnings("Unchecked")
    public void updateFromCollective(JsonObject absenceBody, UserInfos user, Long collectiveId, boolean editEvents, Handler<AsyncResult<JsonObject>> handler) {
        String beforeUpdateAbsenceQuery = "SELECT * FROM " + Presences.dbSchema + ".absence WHERE collective_id = ?";
        sql.prepared(beforeUpdateAbsenceQuery, new JsonArray().add(collectiveId), SqlResult.validResultHandler(oldAbsenceResult -> {
            if (oldAbsenceResult.isLeft()) {
                String message = "[Presences@DefaultAbsenceService::update] failed to retrieve absences";
                LOGGER.error(message, oldAbsenceResult.left().getValue());
                handler.handle(Future.failedFuture(message));
                return;
            }
            List<JsonObject> oldAbsences = oldAbsenceResult.right().getValue().getList();

            String query = "UPDATE " + Presences.dbSchema + ".absence " +
                    "SET start_date = ?, end_date = ?";

            JsonArray values = new JsonArray()
                    .add(absenceBody.getString("start_date"))
                    .add(absenceBody.getString("end_date"));

            Integer reasonId = absenceBody.getInteger("reason_id");
            if (reasonId != null) {
                query += ", reason_id = ?";
                values.add(reasonId);
            }

            if (absenceBody.getBoolean("counsellor_regularisation") != null) {
                query += ", counsellor_regularisation = ?";
                values.add(absenceBody.getBoolean("counsellor_regularisation"));
            }

            query += " WHERE collective_id = ? AND structure_id = ?";
            values.add(collectiveId)
                    .add(absenceBody.getString("structure_id"));

            sql.prepared(query, values, SqlResult.validUniqueResultHandler(absenceResult -> {
                if (absenceResult.isLeft()) {
                    String message = "[Presences@DefaultAbsenceService::updateFromCollective] failed to update absence";
                    LOGGER.error(message, absenceResult.left().getValue());
                    handler.handle(Future.failedFuture(message));
                    return;
                }

                regularizedFromCollective(collectiveId, absenceBody.getBoolean("counsellor_regularisation", false), regularizeResult -> {
                    if (regularizeResult.failed()) {
                        String message = "[Presences@DefaultAbsenceService::updateFromCollective] failed to regularize absences";
                        LOGGER.error(message, regularizeResult.cause().getMessage());
                        handler.handle(Future.failedFuture(regularizeResult.cause().getMessage()));
                        return;
                    }
                    List<Future<JsonObject>> futures = new ArrayList<>();

                    for (JsonObject oldAbsence : oldAbsences) {
                        Future<JsonObject> future = Future.future();
                        futures.add(future);

                        absenceBody.put("student_id", oldAbsence.getString("student_id"));

                        afterPersistAbsence(oldAbsence.getLong("id"), absenceBody, oldAbsence, editEvents, user.getUserId(),
                                FutureHelper.handlerJsonObject(future), absenceResult);
                    }

                    FutureHelper.all(futures).setHandler(result -> {
                        if (result.failed()) {
                            handler.handle(Future.failedFuture(result.cause().getMessage()));
                            return;
                        }
                        handler.handle(Future.succeededFuture(new JsonObject().put("success", "ok")));
                    });
                });
            }));
        }));
    }

    private void regularizedFromCollective(Long collectiveId, Boolean regularize, Handler<AsyncResult<JsonArray>> handler) {
        if (Boolean.TRUE.equals(regularize))
            regularizeAfterCollectivePersist(collectiveId, handler);
        else handler.handle(Future.succeededFuture(new JsonArray()));
    }

    private void afterPersistAbsence(Long absenceId, JsonObject absenceBody, JsonObject oldAbsence, boolean editEvents, String userInfoId, Handler<Either<String, JsonObject>> handler, Either<String, JsonObject> absenceResult) {
        if (absenceResult.isLeft()) {
            String message = "[Presences@DefaultAbsenceService] failed to update absence";
            LOGGER.error(message, absenceResult.left().getValue());
            handler.handle(new Either.Left<>(message));
        } else if (editEvents) {
            interactingEvents(absenceBody, oldAbsence, userInfoId, event -> {
                if (event.isLeft()) {
                    String message = "[Presences@DefaultAbsenceService] failed to interact with events while updating absence";
                    LOGGER.error(message, event.left().getValue());
                    handler.handle(new Either.Left<>(message));
                } else {
                    handler.handle(new Either.Right<>(event.right().getValue()
                            .put("id", absenceId)));
                }
            });
        } else {
            handler.handle(new Either.Right<>(absenceResult.right().getValue()));
        }
    }

    private void afterPersistAbsences(List<Long> absenceIds, String userInfoId, boolean editEvents, Handler<Either<String, JsonObject>> handler,
                                      Either<String, JsonObject> absencesResult) {
        if (absencesResult.isLeft()) {
            String message = "[Presences@DefaultAbsenceService] failed to update absence";
            LOGGER.error(message, absencesResult.left().getValue());
            handler.handle(new Either.Left<>(message));
        } else {
            String query = " SELECT * " +
                    " FROM " + Presences.dbSchema + ".absence " +
                    " WHERE id IN " + Sql.listPrepared(absenceIds);

            JsonArray params = new JsonArray();
            params.addAll(new JsonArray(absenceIds));

            afterPersistAbsence(query, params, userInfoId, editEvents, handler);
        }
    }

    @Override
    public void afterPersistCollective(Long collectiveId, String structureId, String userInfoId, boolean editEvents, Handler<AsyncResult<JsonObject>> handler) {
        String query = " SELECT * " +
                " FROM " + Presences.dbSchema + ".absence " +
                " WHERE collective_id = ? AND structure_id = ?";

        JsonArray params = new JsonArray().add(collectiveId).add(structureId);
        afterPersistAbsence(query, params, userInfoId, editEvents, FutureHelper.handlerJsonObject(handler));
    }

    @Override
    public void afterPersist(List<String> studentIds, String structureId, String startDate, String endDate, String userInfoId, boolean editEvents, Handler<AsyncResult<JsonObject>> handler) {
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence " +
                " WHERE student_id IN " + Sql.listPrepared(studentIds.toArray()) +
                " AND start_date = ? " +
                " AND end_date = ? " +
                " AND structure_id = ? ";

        JsonArray params = new JsonArray()
                .addAll(new JsonArray(studentIds))
                .add(startDate)
                .add(endDate)
                .add(structureId);
        afterPersistAbsence(query, params, userInfoId, editEvents, FutureHelper.handlerJsonObject(handler));
    }

    private void afterPersistAbsence(String getAbsencesQuery, JsonArray params, String userInfoId, boolean editEvents, Handler<Either<String, JsonObject>> handler) {
        Sql.getInstance().prepared(getAbsencesQuery, params, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Presences@DefaultAbsenceService::afterPersistAbsences] Failed to retrieve absences";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else if (editEvents) {
                JsonArray absences = result.right().getValue();

                List<Future> futures = new ArrayList<>();

                absences.forEach(oAbsence -> {
                    Future<JsonObject> future = Future.future();
                    futures.add(future);
                    JsonObject absence = (JsonObject) oAbsence;
                    interactingEvents(absence, userInfoId, future);
                });

                CompositeFuture.all(futures).setHandler(event -> {
                    if (event.failed()) {
                        LOGGER.info("[Presences@DefaultAbsenceService::afterPersistAbsences::CompositeFuture]: " +
                                "An error has occured)");
                        handler.handle(new Either.Left<>(event.cause().getMessage()));
                    } else {
                        handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
                    }
                });
            } else {
                handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
            }
        }));
    }

    private void interactingEvents(JsonObject absenceBody, JsonObject oldAbsence, String userInfoId, Handler<Either<String, JsonObject>> handler) {
        List<String> users = new ArrayList<>();
        users.add(absenceBody.getString("student_id"));
        groupService.getUserGroups(users, absenceBody.getString("structure_id"), groupEvent -> {
            if (groupEvent.isLeft()) {
                String message = "[Presences@DefaultAbsenceService::interactingEvents] failed to retrieve user info";
                LOGGER.error(message, groupEvent.left().getValue());
                handler.handle(new Either.Left<>(message));
            } else {
                List<String> groupIds = new ArrayList<>();
                for (int i = 0; i < groupEvent.right().getValue().size(); i++) {
                    groupIds.add(groupEvent.right().getValue().getJsonObject(i).getString("id"));
                }
                matchEventsWithAbsents(absenceBody, oldAbsence, groupIds, userInfoId, handler);
            }
        });
    }

    private void interactingEvents(JsonObject absenceBody, String userInfoId, Future<JsonObject> future) {
        interactingEvents(absenceBody, null, userInfoId, FutureHelper.handlerJsonObject(future));
    }

    private void matchEventsWithAbsents(JsonObject absenceBody, JsonObject oldAbsence, List<String> groupIds, String userInfoId, Handler<Either<String, JsonObject>> handler) {
        Future<JsonArray> createEventsFuture = Future.future();
        Future<JsonArray> updateEventsFuture = Future.future();
        Future<JsonObject> deleteEventsFuture = Future.future();

        Boolean isRegularized = absenceBody.getBoolean("counsellor_regularisation", false);
        createEventsWithAbsents(absenceBody, isRegularized, groupIds, userInfoId, FutureHelper.handlerJsonArray(createEventsFuture));
        updateEventsWithAbsents(absenceBody, isRegularized, groupIds, FutureHelper.handlerJsonArray(updateEventsFuture));
        deleteEventsFromAbsence(absenceBody, oldAbsence, FutureHelper.handlerJsonObject(deleteEventsFuture));


        CompositeFuture.all(createEventsFuture, updateEventsFuture, deleteEventsFuture).setHandler(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultAbsenceService] Failed to create or update events from absent";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                List<Long> updatedRegisterId = new ArrayList<>();
                List<Long> createdRegisterId = new ArrayList<>();
                for (int i = 0; i < updateEventsFuture.result().size(); i++) {
                    updatedRegisterId.add(updateEventsFuture.result().getJsonObject(i).getLong("register_id"));
                }
                for (int j = 0; j < createEventsFuture.result().size(); j++) {
                    createdRegisterId.add(createEventsFuture.result().getJsonObject(j).getLong("register_id"));
                }
                handler.handle(new Either.Right<>(new JsonObject()
                        .put("updatedRegisterId", updatedRegisterId)
                        .put("createdRegisterId", createdRegisterId)));
            }
        });
    }

    @Override
    public void changeReasonAbsences(JsonObject absenceBody, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "UPDATE " + Presences.dbSchema + ".absence SET reason_id = ? ";
        if (absenceBody.getInteger("reasonId") != null) {
            params.add(absenceBody.getInteger("reasonId"));
        } else {
            params.addNull();
        }
        query += " WHERE id IN " + Sql.listPrepared(absenceBody.getJsonArray("ids").getList());
        params.addAll(absenceBody.getJsonArray("ids"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(result -> {
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < absenceBody.getJsonArray("ids").size(); i++) {
                ids.add(Long.parseLong(absenceBody.getJsonArray("ids").getInteger(i).toString()));
            }
            afterPersistAbsences(ids, user.getUserId(), true, handler, result);
        }));
    }

    @Override
    public void changeRegularizedAbsences(JsonObject absenceBody, UserInfos user, boolean editEvents, Handler<Either<String, JsonObject>> handler) {
        Boolean isRegularized = absenceBody.getBoolean("regularized");
        JsonArray ids = absenceBody.getJsonArray("ids");
        JsonArray params = new JsonArray();
        if (isRegularized == null || ids == null || ids.isEmpty()) {
            String message = "[Presences@DefaultAbsenceService::changeRegularizedAbsences] some fields are null or empty.";
            LOGGER.error(message);
            handler.handle(new Either.Left<>(message));
            return;
        }
        String query = "UPDATE " + Presences.dbSchema + ".absence SET counsellor_regularisation = ? ";
        params.add(isRegularized);

        query += " WHERE id IN " + Sql.listPrepared(absenceBody.getJsonArray("ids").getList());
        params.addAll(ids);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(result -> {
            List<Long> resultIds = absenceBody.getJsonArray("ids").stream().map(id -> Long.parseLong(id.toString())).collect(Collectors.toList());
            afterPersistAbsences(resultIds, user.getUserId(), editEvents, handler, result);
        }));
    }

    @Override
    public void changeRegularizedAbsences(JsonObject absenceBody, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        changeRegularizedAbsences(absenceBody, user, true, handler);
    }

    private void createEventsWithAbsents(JsonObject absenceBody, Boolean isRegularized, List<String> groupIds, String ownerId, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH register as " +
                "(" +
                "SELECT register.id, register.start_date, register.end_date FROM " + Presences.dbSchema + ".register " +
                "INNER JOIN presences.rel_group_register as rgr ON (rgr.register_id = register.id) " +
                "WHERE rgr.group_id IN " + Sql.listPrepared(groupIds.toArray()) + " " +
                "AND register.start_date >= ? " +
                "AND register.end_date <= ? " +
                "AND register.id NOT IN (" +
                "  SELECT event.register_id FROM " + Presences.dbSchema + ".event " +
                "  WHERE event.type_id = 1 and event.register_id = register.id and event.student_id = ?" +
                ")" +
                ") " +
                "INSERT INTO " + Presences.dbSchema + ".event (start_date, end_date, comment, counsellor_input, followed, student_id, register_id, type_id, reason_id, owner)" +
                "(SELECT register.start_date, register.end_date, '', true, ?, ?," +
                "register.id, 1, ?, ? FROM register) " +
                "RETURNING *";

        JsonArray values = new JsonArray()
                .addAll(new JsonArray(groupIds))
                .add(absenceBody.getString("start_date"))
                .add(absenceBody.getString("end_date"))
                .add(absenceBody.getString("student_id"))
                .add(absenceBody.getBoolean("followed", false));
        values.add(absenceBody.getString("student_id"));

        if (absenceBody.getInteger("reason_id") != null) {
            values.add(absenceBody.getInteger("reason_id"));
        } else {
            values.addNull();
        }
        values.add((absenceBody.getString("owner") != null) ? absenceBody.getString("owner") : ownerId);
        sql.prepared(query, values, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Presences@DefaultAbsenceService::createEventsWithAbsents] Failed to create events from absent.";
                LOGGER.error(message + " " + result.left().getValue());
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonArray events = result.right().getValue();
            regularizeEventsFromAbsence(events, isRegularized, handler);
        }));
    }

    private void updateEventsWithAbsents(JsonObject absenceBody, Boolean isRegularized, List<String> groupIds, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH register as " +
                "(" +
                "SELECT register.id, register.start_date, register.end_date FROM " + Presences.dbSchema + ".register " +
                "INNER JOIN presences.rel_group_register as rgr ON (rgr.register_id = register.id) " +
                "WHERE rgr.group_id IN " + Sql.listPrepared(groupIds.toArray()) + " " +
                "AND register.start_date >= ? " +
                "AND register.end_date <= ? " +
                "AND register.id IN (" +
                "  SELECT event.register_id FROM " + Presences.dbSchema + ".event" +
                "  WHERE event.type_id = 1 and event.register_id = register.id and event.student_id = ?" +
                ") " +
                ") " +
                "UPDATE " + Presences.dbSchema + ".event SET reason_id = ?, followed = ? WHERE register_id IN (SELECT id FROM register) AND student_id = ? " +
                "RETURNING *";
        JsonArray values = new JsonArray()
                .addAll(new JsonArray(groupIds))
                .add(absenceBody.getString("start_date"))
                .add(absenceBody.getString("end_date"))
                .add(absenceBody.getString("student_id"));

        if (absenceBody.getInteger("reason_id") != null) {
            values.add(absenceBody.getInteger("reason_id"));
        } else {
            values.addNull();
        }

        values.add(absenceBody.getBoolean("followed", false));

        values.add(absenceBody.getString("student_id"));
        sql.prepared(query, values, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Presences@DefaultAbsenceService::updateEventsWithAbsents] Failed to update events from absent.";
                LOGGER.error(message + " " + result.left().getValue());
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonArray events = result.right().getValue();
            regularizeEventsFromAbsence(events, isRegularized, handler);
        }));
    }

    private void regularizeEventsFromAbsence(JsonArray events, Boolean isRegularized, Handler<Either<String, JsonArray>> handler) {
        if (isRegularized != null) {
            changeRegularizedEvents(events, isRegularized, result -> {
                if (result.isLeft()) {
                    String message = "[Presences@DefaultAbsenceService::regularizeEventsFromAbsence] Failed to regularize or follow absence linked events.";
                    LOGGER.error(message + " " + result.left().getValue());
                    handler.handle(new Either.Left<>(message));
                    return;
                }
                handler.handle(new Either.Right<>(events));
            });
        } else {
            handler.handle(new Either.Right<>(events));
        }
    }

    private void deleteEventsFromAbsence(JsonObject newAbsence, JsonObject oldAbsence, Handler<Either<String, JsonObject>> handler) {
        if (oldAbsence == null || oldAbsence.isEmpty()) {
            handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
            return;
        }

        Future<JsonObject> beforeEventsRemovalFuture = Future.future();
        Future<JsonObject> afterEventsRemovalFuture = Future.future();

        // Here if the new period start later than the oldest one, we remove events corresponding to the period it no longer covers
        // so we check if oldAbsence.start_date < (plus tôt que..) newAbsence.start_date
        removeEventsIfDateBefore(
                oldAbsence.getString("start_date"),
                newAbsence.getString("start_date"),
                oldAbsence.getString("student_id"),
                FutureHelper.handlerJsonObject(beforeEventsRemovalFuture)
        );

        // Here if the new period start later than the oldest one, we remove events corresponding to the period it no longer covers
        // so we check if newAbsence.end_date < (plus tôt que..) oldAbsence.end_date
        removeEventsIfDateBefore(
                newAbsence.getString("end_date"),
                oldAbsence.getString("end_date"),
                oldAbsence.getString("student_id"),
                FutureHelper.handlerJsonObject(afterEventsRemovalFuture)
        );

        CompositeFuture.all(beforeEventsRemovalFuture, afterEventsRemovalFuture).setHandler(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultAbsenceService::deleteEventsFromAbsence] Failed to delete events";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
            }
        });
    }

    private void removeEventsIfDateBefore(String date1, String date2, String student_id, Handler<Either<String, JsonObject>> handler) {
        try {
            if (DateHelper.isBefore(date1, date2)) {
                JsonObject deleteData = new JsonObject()
                        .put("start_date", date1)
                        .put("end_date", date2)
                        .put("student_id", student_id);
                deleteEventsOnDelete(deleteData, handler);
            } else {
                handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
            }
        } catch (ParseException e) {
            String message = "[Presences@DefaultAbsenceService::removeEventsIfDateBefore] An error occurred while parsing dates";
            LOGGER.error(message, e.getCause().getMessage());
            handler.handle(new Either.Left<>(message));
        }
    }

    // same function that in DefaultEventService. we so it there, because we cannot cross service instances
    private void changeRegularizedEvents(JsonArray events, Boolean regularized, Handler<Either<String, JsonObject>> handler) {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            Event event = new Event(events.getJsonObject(i), new ArrayList<>());
            ids.add(event.getId());
        }

        if (!ids.isEmpty()) {
            JsonArray params = new JsonArray();
            String query = "UPDATE " + Presences.dbSchema + ".event SET counsellor_regularisation = ? ";
            params.add(regularized);

            query += " WHERE id IN " + Sql.listPrepared(ids) + " AND type_id = 1";
            params.addAll(new JsonArray(ids));
            sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
            return;
        }
        handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
    }

    @Override
    public void followAbsence(JsonArray absenceIds, Boolean followed, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "UPDATE " + Presences.dbSchema + ".absence SET followed = ? WHERE id IN " + Sql.listPrepared(absenceIds);
        params.add(followed)
                .addAll(absenceIds);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(Integer absenceId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence WHERE id = " + absenceId;
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(absenceResult -> {
            if (absenceResult.isLeft()) {
                String message = "[Presences@DefaultAbsenceService] failed to fetch absence id before deletion";
                LOGGER.error(message, absenceResult.left().getValue());
                handler.handle(new Either.Left<>(message));
            } else {
                Future<JsonObject> deleteEventsFuture = Future.future();
                Future<JsonObject> resetEventsFuture = Future.future();
                Future<JsonObject> deleteAbsenceFuture = Future.future();

                deleteEventsOnDelete(absenceResult.right().getValue(), FutureHelper.handlerJsonObject(deleteEventsFuture));
                resetEventsOnDelete(absenceResult.right().getValue(), FutureHelper.handlerJsonObject(resetEventsFuture));
                deleteAbsence(absenceId, FutureHelper.handlerJsonObject(deleteAbsenceFuture));

                CompositeFuture.all(deleteEventsFuture, resetEventsFuture, deleteAbsenceFuture).setHandler(event -> {
                    if (event.failed()) {
                        String message = "[Presences@DefaultAbsenceService] Failed to delete events or absence";
                        LOGGER.error(message);
                        handler.handle(new Either.Left<>(message));
                    } else {
                        handler.handle(new Either.Right<>(absenceResult.right().getValue()));
                    }
                });
            }
        }));
    }

    private void deleteEventsOnDelete(JsonObject absenceResult, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT " + Presences.dbSchema + ".function_delete_events_synchronously(?,?,?)";

        JsonArray params = new JsonArray()
                .add(absenceResult.getString("student_id"))
                .add(absenceResult.getString("start_date"))
                .add(absenceResult.getString("end_date"));
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private void resetEventsOnDelete(JsonObject absenceResult, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Presences.dbSchema + ".event SET reason_id = null, followed = false " +
                "WHERE student_id = ? AND start_date >= ? AND end_date <= ? AND counsellor_input = false AND type_id = "
                + EventType.ABSENCE.getType();

        JsonArray params = new JsonArray()
                .add(absenceResult.getString("student_id"))
                .add(absenceResult.getString("start_date"))
                .add(absenceResult.getString("end_date"));
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private void deleteAbsence(Integer absenceId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Presences.dbSchema + ".absence WHERE id = " + absenceId;
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void absenceRemovalTask(Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Presences.dbSchema + ".absence WHERE start_date <= NOW() - interval '72 hour'";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public Future<JsonObject> count(String structure, List<String> students, String start, String end, Boolean regularized,
                                    Boolean noReason, Boolean followed, List<Integer> reasons) {
        Promise<JsonObject> promise = Promise.promise();

        JsonArray params = new JsonArray();
        String query = "SELECT COUNT(id) " +
                " FROM " + Presences.dbSchema + ".absence " +
                getWhereFilter(params, structure, students, start, end, regularized, noReason, followed, reasons);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(promise)));

        return promise.future();
    }

    @Override
    public Future<JsonArray> retrieve(String structure, List<String> students, String start, String end, Boolean regularized,
                                      Boolean noReason, Boolean followed, List<Integer> reasons, Integer page) {
        Promise<JsonArray> promise = Promise.promise();

        JsonArray params = new JsonArray();
        String query = "SELECT id, start_date, end_date, student_id, reason_id, counsellor_regularisation, followed, " +
                " structure_id, owner" +
                " FROM " + Presences.dbSchema + ".absence " +
                getWhereFilter(params, structure, students, start, end, regularized, noReason, followed, reasons) +
                " ORDER BY start_date DESC";

        if (page != null) {
            params.add(Presences.PAGE_SIZE);
            params.add(page * Presences.PAGE_SIZE);
            query += " LIMIT ? OFFSET ?";
        }

        sql.prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(promise)));

        return promise.future();
    }

    private String getWhereFilter(JsonArray params, String structureId, List<String> studentIds, String startAt, String endAt,
                                  Boolean regularized, Boolean noReason, Boolean followed, List<Integer> reasonIds) {
        String where = "WHERE structure_id = ?";
        params.add(structureId);

        if (!studentIds.isEmpty()) {
            where += " AND student_id IN " + Sql.listPrepared(studentIds);
            params.addAll(new JsonArray(studentIds));
        }

        where += filterReasons(reasonIds, noReason, regularized, params);

        if (followed != null) {
            where += " AND followed = ? ";
            params.add(followed);
        }

        if (startAt != null) {
            where += " AND end_date > ? ";
            params.add(DateHelper.isFormat(startAt, DateHelper.MONGO_FORMAT) ? startAt : String.format("%s %s", startAt, defaultStartTime));
        }

        if (endAt != null) {
            where += " AND start_date < ? ";
            params.add(DateHelper.isFormat(endAt, DateHelper.MONGO_FORMAT) ? endAt : String.format("%s %s", endAt, defaultEndTime));
        }

        return where;
    }

    private String filterReasons(List<Integer> reasonIds, Boolean noReason, Boolean regularized, JsonArray params) {
        String query = "";

        boolean isNoReason = (noReason != null && noReason);

        // this condition occurs when we want to filter no reason and regularized event at the same time
        if (isNoReason && (regularized != null && regularized)) {
            query += " AND (reason_id IS NULL OR (reason_id IS NOT NULL AND counsellor_regularisation = true)";
            if (reasonIds != null && !reasonIds.isEmpty()) {
                query += " AND reason_id IN " + Sql.listPrepared(reasonIds);
                params.addAll(new JsonArray(reasonIds));
            }
            query += ")";

            // else is default condition
        } else {
            // If we want to fetch events WITH reasonId, array reasonIds fetched is not empty
            // (optional if we wish noReason fetched at same time then noReason is TRUE)
            if (reasonIds != null && !reasonIds.isEmpty()) {
                query += " AND ((reason_id IN " + Sql.listPrepared(reasonIds) + ")";
                if (isNoReason) query += " OR reason_id IS NULL";
                query += ")";
                params.addAll(new JsonArray(reasonIds));
            }

            // If we want to fetch events with NO reasonId, array reasonIds fetched is empty
            // AND noReason is TRUE
            if (reasonIds != null && reasonIds.isEmpty() && isNoReason)
                query += " AND (reason_id IS NULL " + (regularized != null ? " OR counsellor_regularisation = " + regularized : "") + ") ";

            if (regularized != null) query += " AND (counsellor_regularisation = " + regularized + ") ";
        }

        return query;
    }

    @Override
    public Future<JsonObject> countAbsentStudents(String structureId, List<String> studentIds, String startAt, String endAt) {
        Promise<JsonObject> promise = Promise.promise();
        JsonArray params = new JsonArray();
        String query = String.format("SELECT COUNT(*)" +
                        " FROM (SELECT DISTINCT student_id" +
                        " FROM presences.absence a %s" +
                        " UNION DISTINCT " +
                        " SELECT DISTINCT student_id " +
                        " FROM presences.event a " +
                        " INNER JOIN presences.register r on a.register_id = r.id %s) as student_ids ",
                countAbsentStudentsWhereFilter(params, structureId, studentIds, startAt, endAt),
                countAbsentStudentsWhereFilter(params, structureId, studentIds, startAt, endAt));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(promise)));

        return promise.future();
    }

    public String countAbsentStudentsWhereFilter(JsonArray params, String structureId, List<String> studentIds,
                                                 String startAt, String endAt) {
        String query = " WHERE structure_id = ?";
        params.add(structureId);
        if (endAt != null) {
            query += " AND a.start_date < ?";
            params.add(endAt);
        }

        if (startAt != null) {
            query += " AND a.end_date > ?";
            params.add(startAt);
        }

        if (studentIds != null && !studentIds.isEmpty()) {
            query += String.format(" AND student_id in %s", Sql.listPrepared(studentIds));
            params.addAll(new JsonArray(studentIds));
        }

        return query;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Future<JsonObject> restoreAbsences(String structureId, String startAt, String endAt) {
        Promise<JsonObject> promise = Promise.promise();
        List<JsonObject> absences = new ArrayList<>();
        retrieve(structureId, Collections.emptyList(), startAt, endAt, null, null, null, null, null)
                .compose(absencesResult -> {
                    absences.addAll(absencesResult.getList());
                    List<String> studentIds = absences.stream()
                            .map(student -> student.getString(Field.ID))
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());

                    return userService.getStudents(studentIds);
                })
                .onFailure(err -> {
                    String message = String.format("[Presences@%s::restoreAbsences] Fail to retrieve absences"
                            , this.getClass().getSimpleName());
                    LOGGER.error(String.format("%s %s", message, err));
                    promise.fail(message);
                })
                .onSuccess(students -> {


                    List<JsonObject> statements = absences.stream()
                            .filter(absence -> {
                                JsonObject student = ((List<JsonObject>) students.getList())
                                        .stream()
                                        .filter(currentStudent -> currentStudent.getString(Field.ID) != null &&
                                                currentStudent.getString(Field.ID)
                                                        .equals(absence.getString(Field.STUDENT_ID)))
                                        .findFirst()
                                        .orElse(null);

                                String absenceStructureId = absence.getString(Field.STRUCTURE_ID);

                                return student != null && absenceStructureId != null &&
                                        !absenceStructureId.equals(student.getString(Field.STRUCTURE_ID));
                            })
                            .map(this::restoreStructureStatement)
                            .collect(Collectors.toList());

                    if (statements.isEmpty()) {
                        promise.complete(new JsonObject());
                        return;
                    }

                    sql.transaction(new JsonArray(statements), SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(promise)));
                });
        return promise.future();
    }

    private JsonObject restoreStructureStatement(JsonObject absence) {
        String structureId = absence.getJsonObject(Field.STUDENT, new JsonObject())
                .getString(Field.STRUCTURE_ID);

        String query = "UPDATE " + Presences.dbSchema + ".absence " +
                "SET structure_id = ? WHERE id = ?";

        JsonArray params = new JsonArray()
                .add(structureId)
                .add(absence.getLong(Field.ID));

        return new JsonObject()
                .put(Field.STATEMENT, query)
                .put(Field.VALUES, params)
                .put(Field.ACTION, Field.PREPARED);
    }
}
