package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.db.DBService;
import fr.openent.presences.enums.EventTypeEnum;
import fr.openent.presences.helper.AbsenceHelper;
import fr.openent.presences.model.Absence;
import fr.openent.presences.model.Audience;
import fr.openent.presences.model.CollectiveAbsence;
import fr.openent.presences.model.Person.Student;
import fr.openent.presences.service.AbsenceService;
import fr.openent.presences.service.CollectiveAbsenceService;
import fr.openent.presences.service.ReasonService;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultCollectiveAbsenceService extends DBService implements CollectiveAbsenceService {
    private static final Logger log = LoggerFactory.getLogger(DefaultCollectiveAbsenceService.class);

    private final GroupService groupService;
    private final UserService userService;
    private final AbsenceService absenceService;
    private final ReasonService reasonService;

    public DefaultCollectiveAbsenceService(EventBus eb) {
        this.groupService = new DefaultGroupService(eb);
        this.userService = new DefaultUserService();
        this.absenceService = new DefaultAbsenceService(eb);
        this.reasonService = new DefaultReasonService();
    }

    @Override
    public void getCollectives(String structureId, String startDate, String endDate, Long reasonId, Boolean regularized,
                               List<String> audienceNames, Integer page, Handler<AsyncResult<JsonObject>> handler) {

        getAudiencesIdsFromNames(structureId, audienceNames)
                .compose(audienceIds -> getPaginatedCollectives(structureId, startDate, endDate, reasonId, regularized,
                        audienceIds, page))
                .setHandler(result -> {
                    if (result.failed()) {
                        log.error(result.cause());
                        handler.handle(Future.failedFuture(result.cause().getMessage()));
                        return;
                    }
                    handler.handle(Future.succeededFuture(result.result()));
                });
    }

    @SuppressWarnings("unchecked")
    private Future<List<String>> getAudiencesIdsFromNames(String structureId, List<String> audienceNames) {
        Future<List<String>> future = Future.future();
        groupService.getAudiencesFromNames(structureId, audienceNames, audiencesResult -> {
            if (audiencesResult.failed()) {
                String message = "[Presences@DefaultCollectiveAbsenceService::getCollectives] Failed to get audiences.";
                log.error(message, audiencesResult.cause());
                future.fail(message);
                return;
            }

            List<String> audienceIds = ((List<JsonObject>) audiencesResult.result().getList()).stream()
                    .map(audience -> audience.getString("id"))
                    .collect(Collectors.toList());
            future.complete(audienceIds);
        });
        return future;
    }

    private Future<JsonObject> getPaginatedCollectives(String structureId, String startDate, String endDate, Long reasonId, Boolean regularized,
                                                       List<String> audienceIds, Integer page) {
        Future<JsonObject> future = Future.future();

        Future<JsonObject> allFutures = Future.future();
        Future<JsonObject> countFutures = Future.future();

        getAll(structureId, startDate, endDate, reasonId, regularized, audienceIds, page, allFutures);
        countTotalPages(structureId, startDate, endDate, reasonId, regularized, audienceIds, countFutures);

        FutureHelper.all(Arrays.asList(allFutures, countFutures)).setHandler(resultRequests -> {
            if (resultRequests.failed()) {
                future.fail(resultRequests.cause().getMessage());
                return;
            }

            JsonObject result = allFutures.result();
            result.put("page", page);
            result = countFutures.result().mergeIn(result);

            future.complete(result);
        });
        return future;
    }

    private void getAll(String structureId, String startDate, String endDate, Long reasonId, Boolean regularized,
                        List<String> audienceIdsFilter, Integer page, Handler<AsyncResult<JsonObject>> handler) {
        getCollectives(structureId, startDate, endDate, reasonId, regularized, audienceIdsFilter, page)
                .compose(collectives -> setCollectivesRelatives(structureId, collectives))
                .setHandler(result -> {
                    if (result.failed()) {
                        String message = "[Presences@DefaultCollectiveAbsenceService::getAll] An error has occured while getting collectives.";
                        log.error(message, result.cause());
                        handler.handle(Future.failedFuture(message));
                        return;
                    }
                    handler.handle(Future.succeededFuture(new JsonObject().put("all", result.result())));
                });
    }

    @SuppressWarnings("unchecked")
    private Future<List<JsonObject>> getCollectives(String structureId, String startDate, String endDate, Long reasonId, Boolean regularized,
                                                    List<String> audienceIdsFilter, Integer page) {
        Future<List<JsonObject>> future = Future.future();

        JsonArray params = new JsonArray();

        String query = "WITH count_student AS " +
                "         ( " +
                "             SELECT collective_id, count(DISTINCT student_id) as countStudent " +
                "             FROM " + Presences.dbSchema + ".absence " +
                "             GROUP BY collective_id " +
                "         ) " +
                "   SELECT ca.*, " +
                "          to_json(r)                                as reason, " +
                "          array_to_json(array_agg(rac.audience_id)) as audienceids, " +
                "          cs.countStudent                           as count_student " +
                "   FROM " + Presences.dbSchema + ".collective_absence ca " +
                "            LEFT JOIN " + Presences.dbSchema + ".reason r ON ca.reason_id = r.id " +
                "            LEFT JOIN count_student cs ON ca.id = cs.collective_id " +
                "            LEFT JOIN " + Presences.dbSchema + ".rel_audience_collective rac ON ca.id = rac.collective_id " +
                getWhereFilter(params, structureId, startDate, endDate, reasonId, regularized, audienceIdsFilter) +
                "   GROUP BY ca.id, ca.start_date, r, cs.countStudent " +
                "   ORDER BY ca.start_date DESC ";

        if (page != null) {
            params.add(Presences.PAGE_SIZE);
            params.add(page * Presences.PAGE_SIZE);
            query += " LIMIT ? OFFSET ?";
        }

        sql.prepared(query, params, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Presences@DefaultCollectiveAbsenceService::getCollectives] Failed to get collectives.";
                log.error(message, result.left().getValue());
                future.fail(message);
                return;
            }

            List<JsonObject> collectives = result.right().getValue().getList();
            future.complete(collectives);
        }));

        return future;
    }

    @SuppressWarnings("unchecked")
    private Future<JsonArray> setCollectivesRelatives(String structureId, List<JsonObject> collectives) {
        Future<JsonArray> future = Future.future();

        List<String> audienceIds = collectives.stream()
                .flatMap(collective -> ((List<String>) new JsonArray(collective.getString("audienceids")).getList()).stream())
                .collect(Collectors.toList());
        List<String> ownerIds = collectives.stream().map(collective -> collective.getString("owner_id")).collect(Collectors.toList());

        Future<JsonArray> ownersFuture = Future.future();
        Future<JsonArray> audiencesFuture = Future.future();

        userService.getUsers(ownerIds, FutureHelper.handlerJsonArray(ownersFuture));
        groupService.getAudiences(structureId, audienceIds, audiencesFuture);

        FutureHelper.all(Arrays.asList(ownersFuture, audiencesFuture)).setHandler(relativesResult -> {
            if (relativesResult.failed()) {
                future.fail(relativesResult.cause().getMessage());
                return;
            }
            future.complete(new JsonArray(formatCollectives(collectives, ownersFuture.result().getList(), audiencesFuture.result().getList())));
        });

        return future;
    }

    private Map<String, JsonObject> getOwnersById(List<JsonObject> owners) {
        Map<String, JsonObject> ownersMap = new HashMap<>();
        for (JsonObject owner : owners) ownersMap.put(owner.getString("id"), owner);
        return ownersMap;
    }

    @SuppressWarnings("unchecked")
    private List<JsonObject> formatCollectives(List<JsonObject> collectives, List<JsonObject> ownersList, List<JsonObject> audiencesList) {
        Map<String, JsonObject> owners = getOwnersById(ownersList);

        return collectives.stream().map(collectiveJson -> {
            CollectiveAbsence collective = new CollectiveAbsence(collectiveJson);
            List<String> audienceIds = new JsonArray(collectiveJson.getString("audienceids")).getList();
            List<JsonObject> audiences = audiencesList.stream().filter(audience ->
                    audienceIds.contains(audience.getString("id")))
                    .collect(Collectors.toList());

            return collective.toCamelJSON()
                    .put("owner", owners.get(collective.getOwnerId()))
                    .put("audiences", audiences)
                    .put("reason", collectiveJson.getString("reason") != null ? new JsonObject(collectiveJson.getString("reason")) : null)
                    .put("countStudent", collectiveJson.getLong("count_student", (long) 0));
        }).collect(Collectors.toList());
    }

    private void countTotalPages(String structureId, String startDate, String endDate, Long reasonId, Boolean regularized,
                                 List<String> audienceIds, Handler<AsyncResult<JsonObject>> handler) {

        JsonArray params = new JsonArray();
        String query = " SELECT count(ca.id) FROM " + Presences.dbSchema + ".collective_absence ca ";

        if (audienceIds != null && !audienceIds.isEmpty()) {
            query += "LEFT JOIN " + Presences.dbSchema + ".rel_audience_collective rac ON ca.id = rac.collective_id";
        }
        query += getWhereFilter(params, structureId, startDate, endDate, reasonId, regularized, audienceIds);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Presences@DefaultCollectiveAbsenceService::countTotalPages] Failed to count collective absences.";
                log.error(message, result.left().getValue());
                handler.handle(Future.failedFuture(message));
                return;
            }

            Long count = result.right().getValue().getLong("count");
            Long countPageNumber = count / Presences.PAGE_SIZE;
            if (count % Presences.PAGE_SIZE == 0) countPageNumber--;

            handler.handle(Future.succeededFuture(new JsonObject().put("page_count", countPageNumber)));
        }));
    }

    private String getWhereFilter(JsonArray params, String structureId, String startDate, String endDate, Long reasonId,
                                  Boolean regularized, List<String> audienceIds) {
        params.add(structureId);
        String where = " WHERE ca.structure_id = ? ";

        if (endDate != null) {
            params.add(endDate);
            where += " AND ca.start_date < ? ";
        }

        if (startDate != null) {
            params.add(startDate);
            where += " AND ca.end_date > ? ";
        }

        if (reasonId != null) {
            params.add(reasonId);
            where += " AND ca.reason_id = ? ";
        }

        if (regularized != null) {
            params.add(regularized);
            where += " AND ca.counsellor_regularisation = ? ";
        }

        if (audienceIds != null && !audienceIds.isEmpty()) {
            params.addAll(new JsonArray(audienceIds));
            where += " AND ca.id IN ( " +
                    "    SELECT collective_id " +
                    "    FROM presences.rel_audience_collective " +
                    "    WHERE audience_id IN " + Sql.listPrepared(audienceIds) +
                    " ) ";
        }

        return where;
    }

    @Override
    public void get(String structureId, Long collectiveId, Handler<AsyncResult<JsonObject>> handler) {
        getCollective(structureId, collectiveId)
                .compose(collective -> setCollectiveRelatives(structureId, collective))
                .setHandler(result -> {
                    if (result.failed()) {
                        log.error(result.cause());
                        handler.handle(Future.failedFuture(result.cause().getMessage()));
                        return;
                    }
                    handler.handle(Future.succeededFuture(result.result()));
                });
    }

    private Future<JsonObject> getCollective(String structureId, Long collectiveId) {
        Future<JsonObject> future = Future.future();
        String query = " SELECT ca.*, " +
                "        array_to_json(array_agg(a.student_id)) as studentids, " +
                "        array_to_json(array_agg(rac.audience_id)) as audienceids " +
                " FROM presences.collective_absence ca " +
                "          LEFT JOIN " + Presences.dbSchema + ".absence a ON ca.id = a.collective_id " +
                "          LEFT JOIN " + Presences.dbSchema + ".rel_audience_collective rac ON ca.id = rac.collective_id " +
                " WHERE ca.structure_id = ? AND ca.id = ? " +
                " GROUP BY ca.id";

        JsonArray params = new JsonArray()
                .add(structureId)
                .add(collectiveId);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(future)));

        return future;
    }

    @SuppressWarnings("unchecked")
    private Future<JsonObject> setCollectiveRelatives(String structureId, JsonObject collective) {
        Future<JsonObject> future = Future.future();

        List<String> audienceIds = new JsonArray(collective.getString("audienceids")).getList();
        List<String> studentIds = new JsonArray(collective.getString("studentids")).getList();

        Future<JsonArray> ownerFuture = Future.future();
        Future<JsonArray> studentsFuture = Future.future();
        Future<JsonArray> audiencesFuture = Future.future();

        userService.getUsers(Collections.singletonList(collective.getString("owner_id")), FutureHelper.handlerJsonArray(ownerFuture));
        userService.getStudentsWithAudiences(structureId, studentIds, studentsFuture);
        groupService.getAudiences(structureId, audienceIds, audiencesFuture);

        FutureHelper.all(Arrays.asList(studentsFuture, audiencesFuture, ownerFuture)).setHandler(relatedResult -> {
            if (relatedResult.failed()) {
                future.fail(relatedResult.cause().getMessage());
                return;
            }

            List<Audience> audiences = Audience.audiences(audiencesFuture.result());
            List<Student> students = Student.students(studentsFuture.result());

            addStudentsInAudiences(audiences, students);

            JsonObject collectiveResult = new CollectiveAbsence(collective).toCamelJSON()
                    .put("owner", ownerFuture.result().getJsonObject(0))
                    .put("audiences", Audience.toJSON(audiences));

            future.complete(collectiveResult);
        });

        return future;
    }

    private void addStudentsInAudiences(List<Audience> audiences, List<Student> students) {
        for (Student student : students) {
            Map<String, Audience> studentAudiencesByIds = new HashMap<>();
            for (Audience studentAudience : student.getAudiences()) {
                studentAudiencesByIds.put(studentAudience.getId(), studentAudience);
            }

            audiences.stream()
                    .filter(audience -> studentAudiencesByIds.containsKey(audience.getId()))
                    .findFirst().ifPresent(audienceConcerned -> audienceConcerned.addStudent(student));

        }

        for (Audience audience : audiences) {
            audience.getStudents().sort(Comparator.comparing(Student::getDisplayName));
        }
    }


    public void getCollectiveFromAbsence(Long absenceId, Handler<AsyncResult<JsonObject>> handler) {
        if (absenceId == null) {
            handler.handle(Future.succeededFuture(new JsonObject()));
            return;
        }

        String query = " SELECT * FROM " + Presences.dbSchema +  ".collective_absence " +
                " WHERE id = ( " +
                "     SELECT collective_id " +
                "     FROM " + Presences.dbSchema + ".absence " +
                "     where id = ? " +
                " ) ";

        JsonArray params = new JsonArray()
                .add(absenceId);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(handler)));
    }


    @Override
    public void getAbsencesStatus(String structureId, List<String> studentIds, String startDate, String endDate,
                                  Long collectiveId, Handler<AsyncResult<JsonObject>> handler) {

        Future<JsonObject> absencesFuture = Future.future();
        Future<JsonObject> collectiveFuture = Future.future();
        Future<JsonObject> absencesCollectiveFuture = Future.future();

        getAbsencesStatusFutures(structureId, collectiveId, studentIds, startDate, endDate,
                absencesFuture, absencesCollectiveFuture, collectiveFuture);

        FutureHelper.all(Arrays.asList(absencesFuture, collectiveFuture, absencesCollectiveFuture)).setHandler(result -> {
            if (result.failed()) {
                handler.handle(Future.failedFuture(result.cause().getMessage()));
                return;
            }

            List<Absence> absences = AbsenceHelper.getAbsenceListFromJsonArray(absencesFuture.result().getJsonArray("all", new JsonArray()), Collections.emptyList());
            List<Absence> absencesCollective = AbsenceHelper.getAbsenceListFromJsonArray(absencesCollectiveFuture.result().getJsonArray("all", new JsonArray()), Collections.emptyList());
            CollectiveAbsence collective = new CollectiveAbsence(collectiveFuture.result());

            List<JsonObject> status = studentIds.stream().map(studentId -> setStatusStudent(studentId, absencesCollective, absences, collective)).collect(Collectors.toList());

            handler.handle(Future.succeededFuture(new JsonObject().put("all", status)));
        });
    }

    private JsonObject setStatusStudent(String studentId, List<Absence> absencesCollective, List<Absence> absences, CollectiveAbsence collective) {
        boolean isAbsent = false;
        boolean isUpdated = false;

        Absence absence = absencesCollective.stream().filter(audience -> audience.getStudentId().equals(studentId)).findFirst().orElse(null);

        if (absence != null) isUpdated = isAbsenceUpdated(absence, collective);
        else {
            absence = absences.stream().filter(audience -> audience.getStudentId().equals(studentId)).findFirst().orElse(null);
            isAbsent = absence != null; // check if we found an absence corresponding to the student in the given period, the student is absent
        }

        return new JsonObject()
                .put("studentId", studentId)
                .put("isUpdated", isUpdated)
                .put("isAbsent", isAbsent);
    }

    private boolean isAbsenceUpdated(Absence absence, CollectiveAbsence collective) {
        boolean startDateCorrespond = absence.getStartDate() != null ?
                absence.getStartDate().equals(collective.getStartDate()) :
                collective.getStartDate() == null;

        boolean endDateCorrespond = absence.getEndDate() != null ?
                absence.getEndDate().equals(collective.getEndDate()) :
                collective.getEndDate() == null;

        boolean reasonCorrespond = absence.getReasonId() != null ?
                Long.valueOf(absence.getReasonId()).equals(collective.getReasonId()) :
                collective.getReasonId() == null;

        // check if absence does not correspond to collective, so that he is updated
        return !(startDateCorrespond && endDateCorrespond && reasonCorrespond &&
                absence.isCounsellorRegularisation().equals(collective.isCounsellorRegularisation()));
    }

    private void getAbsencesStatusFutures(String structureId, Long collectiveId, List<String> studentIds,
                                          String startDate, String endDate, Future<JsonObject> absencesFuture,
                                          Future<JsonObject> absencesCollectiveFuture, Future<JsonObject> collectiveFuture) {

        absenceService.getAbsencesBetweenDates(startDate, endDate, studentIds, absencesResult -> {
            if (absencesResult.isLeft()) {
                absencesFuture.fail(absencesResult.left().getValue());
                return;
            }

            absencesFuture.complete(new JsonObject().put("all", absencesResult.right().getValue()));
        });

        getCollectiveAbsences(structureId, collectiveId, absencesResult -> {
            if (absencesResult.failed()) {
                absencesCollectiveFuture.fail(absencesResult.cause().getMessage());
                return;
            }

            absencesCollectiveFuture.complete(new JsonObject().put("all", absencesResult.result()));
        });
        getCollective(structureId, collectiveId, collectiveFuture);
    }

    private void getCollectiveAbsences(String structureId, Long collectiveId, Handler<AsyncResult<JsonArray>> handler) {
        if (collectiveId == null) {
            handler.handle(Future.succeededFuture(new JsonArray()));
            return;
        }
        absenceService.getAbsencesFromCollective(structureId, collectiveId, FutureHelper.handlerJsonArray(handler));
    }

    private void getCollective(String structureId, Long collectiveId, Handler<AsyncResult<JsonObject>> handler) {
        if (collectiveId == null) {
            handler.handle(Future.succeededFuture(new JsonObject()));
            return;
        }

        String query = "SELECT * FROM " + Presences.dbSchema + ".collective_absence " +
                " WHERE id = ? AND structure_id = ?";

        JsonArray params = new JsonArray()
                .add(collectiveId)
                .add(structureId);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(handler)));
    }

    @Override
    public void create(JsonObject collectiveBody, UserInfos user, String structureId, Handler<AsyncResult<JsonObject>> handler) {
        CollectiveAbsence collective = new CollectiveAbsence(collectiveBody);
        collective.setStructureId(structureId);
        isRegularizedByProvingReason(collective.getReasonId(), collectiveBody.getBoolean("counsellorRegularisation"), regularizedResult -> {
            if (regularizedResult.failed()) {
                String message = "[Presences@DefaultCollectiveAbsenceService::create] Failed to create collective.";
                log.error(message, regularizedResult.cause().getMessage());
                handler.handle(Future.failedFuture(message));
                return;
            }
            collective.setCounsellorRegularisation(regularizedResult.result().getBoolean("regularized"));
            create(collective, user, structureId, result -> {
                if (result.failed()) {
                    handler.handle(Future.failedFuture(result.cause().getMessage()));
                    return;
                }

                JsonArray audiences = collectiveBody.getJsonArray("audiences", new JsonArray());
                createRelatives(audiences, collective, user, structureId, result.result().getLong("id"), handler);
            });
        });
    }

    private void create(CollectiveAbsence collective, UserInfos user, String structureId, Handler<AsyncResult<JsonObject>> handler) {
        String query = "INSERT INTO " + Presences.dbSchema
                + ".collective_absence(structure_id, start_date, end_date, owner_id, comment, reason_id, counsellor_regularisation) " +
                " VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING *";

        JsonArray params = new JsonArray()
                .add(structureId)
                .add(collective.getStartDate())
                .add(collective.getEndDate())
                .add(user.getUserId());

        if (collective.getComment() != null) params.add(collective.getComment());
        else params.addNull();

        addReasonParams(collective, params);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(handler)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void update(JsonObject collectiveBody, UserInfos user, String structureId, Long collectiveId, Handler<AsyncResult<JsonObject>> handler) {
        CollectiveAbsence collective = new CollectiveAbsence(collectiveBody);

        isRegularizedByProvingReason(collective.getReasonId(), collectiveBody.getBoolean("counsellorRegularisation"), regularizedResult -> {
            if (regularizedResult.failed()) {
                handler.handle(Future.failedFuture(regularizedResult.cause().getMessage()));
                return;
            }
            collective.setCounsellorRegularisation(regularizedResult.result().getBoolean("regularized"));

            Future<JsonObject> updateCollectiveFuture = Future.future();
            Future<JsonObject> updateAbsencesFuture = Future.future();

            update(collective, structureId, collectiveId, updateCollectiveFuture);

            List<String> studentIds = (List<String>) collectiveBody.getJsonArray("audiences")
                    .stream()
                    .flatMap(audience -> ((JsonObject) audience).getJsonArray("studentIds").getList().stream())
                    .collect(Collectors.toList());

            JsonObject absenceBody = collective.toJSON()
                    .put("structure_id", structureId)
                    .put("student_id", studentIds);
            absenceService.updateFromCollective(absenceBody, user, collectiveId, true, updateAbsencesFuture);


            FutureHelper.all(Arrays.asList(updateCollectiveFuture, updateAbsencesFuture)).setHandler(updateResult -> {
                if (updateResult.failed()) {
                    handler.handle(Future.failedFuture(updateResult.cause().getMessage()));
                    return;
                }

                JsonArray audiences = collectiveBody.getJsonArray("audiences", new JsonArray());
                createRelatives(audiences, collective, user, structureId, collectiveId, handler);
            });
        });
    }

    private void update(CollectiveAbsence collective, String structureId, Long collectiveId, Handler<AsyncResult<JsonObject>> handler) {
        String query = "UPDATE " + Presences.dbSchema + ".collective_absence " +
                " SET start_date = ?, end_date = ?, comment = ?, reason_id = ?,  counsellor_regularisation = ? " +
                " WHERE id = ? AND structure_id = ?";

        JsonArray params = new JsonArray()
                .add(collective.getStartDate())
                .add(collective.getEndDate());

        if (collective.getComment() != null) params.add(collective.getComment());
        else params.addNull();

        addReasonParams(collective, params);
        params.add(collectiveId)
                .add(structureId);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(handler)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removeAbsenceFromCollectiveAbsence(JsonObject students, String structureId, Long collectiveId, Handler<AsyncResult<JsonObject>> handler) {

        if (students.getJsonArray("studentIds") == null || students.getJsonArray("studentIds").getList().isEmpty()) {
            handler.handle(Future.succeededFuture(new JsonObject().put("success", "ok")));
            return;
        }
        List<String> studentIds = students.getJsonArray("studentIds").getList();

        Future<JsonObject> collectiveFuture = Future.future();
        Future<JsonObject> absencesFuture = Future.future();

        get(structureId, collectiveId, collectiveFuture);
        getAbsencesFromStudentIds(studentIds, structureId, collectiveId, absencesFuture);

        FutureHelper.all(Arrays.asList(collectiveFuture, absencesFuture)).setHandler(result -> {
            if (result.failed()) {
                String message = "[Presences@DefaultCollectiveAbsenceService::removeAbsenceFromCollectiveAbsence] Failed to get data relative to collective";
                log.error(message, result.cause());
                handler.handle(Future.failedFuture(result.cause().getMessage()));
            } else {
                List<Absence> absences = AbsenceHelper.getAbsenceListFromJsonArray(absencesFuture.result().getJsonArray("all", new JsonArray()), Collections.emptyList());
                CollectiveAbsence collective = new CollectiveAbsence(collectiveFuture.result());
                deleteAbsences(absences, collective, deleteRes -> {
                    if (deleteRes.failed()) {
                        String message = "[Presences@DefaultCollectiveAbsenceService::removeAbsenceFromCollectiveAbsence] Failed to delete absence from collective absence.";
                        log.error(message, deleteRes.cause());
                        handler.handle(Future.failedFuture(deleteRes.cause().getMessage()));
                    } else removeAudiencesRelation(collective, handler);
                });

            }
        });
    }

    @Override
    public void removeAudiencesRelation(String structureId, Long collectiveId, Handler<AsyncResult<JsonObject>> handler) {
        get(structureId, collectiveId, collectiveResult -> {
            if (collectiveResult.failed()) {
                log.error(collectiveResult.cause());
                handler.handle(Future.failedFuture(collectiveResult.cause().getMessage()));
            }
            removeAudiencesRelation(new CollectiveAbsence(collectiveResult.result()), handler);
        });
    }

    private void removeAudiencesRelation(CollectiveAbsence collective, Handler<AsyncResult<JsonObject>> handler) {
        List<String> audienceIdsToDelete = collective.getAudiences().stream()
                .filter(audience -> audience.getStudents().isEmpty())
                .map(Audience::getId)
                .collect(Collectors.toList());

        if (!audienceIdsToDelete.isEmpty()) removeCollectiveAudiences(audienceIdsToDelete, collective.getId(), handler);
        else handler.handle(Future.succeededFuture(new JsonObject().put("success", "ok")));
    }

    private void removeCollectiveAudiences(List<String> audienceIds, Long collectiveId, Handler<AsyncResult<JsonObject>> handler) {
        String query = "DELETE FROM " + Presences.dbSchema + ".rel_audience_collective " +
                " WHERE audience_id IN " + Sql.listPrepared(audienceIds) + " AND collective_id = ? ";

        JsonArray params = new JsonArray()
                .addAll(new JsonArray(audienceIds))
                .add(collectiveId);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(handler)));
    }

    private void getAbsencesFromStudentIds(List<String> studentIds, String structureId, Long collectiveId, Handler<AsyncResult<JsonObject>> handler) {

        String query = "SELECT * FROM " + Presences.dbSchema + ".absence WHERE structure_id = ? AND collective_id = ? AND student_id IN " + Sql.listPrepared(studentIds);

        JsonArray params = new JsonArray();
        params.add(structureId);
        params.add(collectiveId);
        params.addAll(new JsonArray(studentIds));

        sql.prepared(query, params, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                log.error(result.left().getValue(), result.left());
                handler.handle(Future.failedFuture(result.left().getValue()));
                return;
            }
            handler.handle(Future.succeededFuture(new JsonObject().put("all", result.right().getValue())));
        }));
    }


    @SuppressWarnings("unchecked")
    private void createRelatives(JsonArray audiences, CollectiveAbsence collective, UserInfos user, String structureId, Long collectiveId, Handler<AsyncResult<JsonObject>> handler) {
        List<String> audienceIds = audiences.stream().map(audience -> ((JsonObject) audience).getString("id")).collect(Collectors.toList());
        List<String> studentIds = (List<String>) audiences
                .stream()
                .flatMap(audience -> ((JsonObject) audience).getJsonArray("studentIds").getList().stream())
                .collect(Collectors.toList());

        Future<JsonObject> collectiveAudiencesFuture = Future.future();
        Future<JsonObject> collectiveAbsencesFuture = Future.future();

        createRelativeAudiences(collectiveId, audienceIds, collectiveAudiencesFuture);
        createRelativeAbsences(collectiveId, user, collective, structureId, studentIds, collectiveAbsencesFuture);

        FutureHelper.all(Arrays.asList(collectiveAudiencesFuture, collectiveAbsencesFuture)).setHandler(relativesResult -> {
            if (relativesResult.failed()) {
                handler.handle(Future.failedFuture(relativesResult.cause().getMessage()));
                return;
            }

            handler.handle(Future.succeededFuture(new JsonObject().put("success", "ok")));

        });
    }

    private void createRelativeAbsences(Long collectiveId, UserInfos user, CollectiveAbsence collective, String structureId,
                                        List<String> studentIds, Handler<AsyncResult<JsonObject>> handler) {
        collective.setStructureId(structureId);
        JsonObject absenceBody = collective.toJSON()
                .put("owner", user.getUserId())
                .put("student_id", studentIds);

        absenceService.create(absenceBody, studentIds, user, collectiveId, result -> {
            if (result.failed()) {
                String message = "[Presences@DefaultCollectiveAbsenceService::create] Failed to create absences from collective.";
                log.error(message, result.cause());
                handler.handle(Future.failedFuture(message));
                return;
            }

            absenceService.afterPersistCollective(collectiveId, structureId, user.getUserId(), true, handler);
        });
    }

    private void createRelativeAudiences(Long collectiveId, List<String> audienceIds, Handler<AsyncResult<JsonObject>> handler) {
        if (audienceIds.isEmpty()) {
            handler.handle(Future.succeededFuture(new JsonObject()));
            return;
        }

        JsonArray statements = new JsonArray();
        for (String audienceId : audienceIds) {
            statements.add(getCollectiveAudienceStatement(collectiveId, audienceId));
        }

        sql.transaction(statements, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(handler)));
    }

    private JsonObject getCollectiveAudienceStatement(Long collectiveId, String audienceId) {
        String query = "INSERT INTO " + Presences.dbSchema + ".rel_audience_collective (collective_id, audience_id) VALUES (?, ?) ON CONFLICT DO NOTHING;";
        JsonArray params = new JsonArray()
                .add(collectiveId)
                .add(audienceId);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private void isRegularizedByProvingReason(Long reasonId, Boolean consellorRegularized, Handler<AsyncResult<JsonObject>> handler) {
        if (Boolean.TRUE.equals(consellorRegularized) || reasonId == null) {
            handler.handle(Future.succeededFuture(new JsonObject().put("regularized", consellorRegularized)));
            return;
        }

        reasonService.getReasons(Collections.singletonList(reasonId.intValue()), reasonResult -> {
            if (reasonResult.isLeft() || reasonResult.right().getValue().size() != 1) {
                String message = "[Presences@DefaultCollectiveAbsenceService::addReasonParams] Failed to retrieve reason.";
                log.error(message, reasonResult.isLeft() ? reasonResult.left().getValue() : "");
                handler.handle(Future.failedFuture(message));
                return;
            }

            JsonObject reason = new JsonObject(reasonResult.right().getValue().getJsonObject(0).getString("reason"));
            handler.handle(Future.succeededFuture(new JsonObject().put("regularized", Boolean.TRUE.equals(reason.getBoolean("proving")))));
        });
    }

    private void addReasonParams(CollectiveAbsence collective, JsonArray params) {
        if (collective.getReasonId() != null) {
            params.add(collective.getReasonId());
            params.add(collective.isCounsellorRegularisation() != null && collective.isCounsellorRegularisation());
        } else {
            params.addNull();
            params.add(false);
        }
    }


    @Override
    public void delete(Long id, String structureId, Handler<AsyncResult<JsonObject>> handler) {
        Future<JsonObject> collectiveFuture = Future.future();
        Future<JsonObject> absencesFuture = Future.future();

        getCollective(structureId, id, collectiveFuture);
        absenceService.getAbsencesFromCollective(structureId, id, FutureHelper.handlerJsonArray(result -> {
            if (result.failed()) {
                absencesFuture.fail(result.cause().getMessage());
                return;
            }

            absencesFuture.complete(new JsonObject().put("all", result.result() != null ? result.result() : new JsonArray()));
        }));

        FutureHelper.all(Arrays.asList(collectiveFuture, absencesFuture)).setHandler(result -> {
            if (result.failed()) {
                String message = "[Presences@DefaultCollectiveAbsenceService::delete] Failed to get data relative to collective.";
                log.error(message, result.cause());
                handler.handle(Future.failedFuture(result.cause().getMessage()));
                return;
            }

            List<Absence> absences = AbsenceHelper.getAbsenceListFromJsonArray(absencesFuture.result().getJsonArray("all", new JsonArray()), Collections.emptyList());
            CollectiveAbsence collective = new CollectiveAbsence(collectiveFuture.result());

            deleteAbsences(absences, collective, deleteRes -> {
                if (deleteRes.failed()) {
                    String message = "[Presences@DefaultCollectiveAbsenceService::delete] Failed to delete absences.";
                    log.error(message, deleteRes.cause());
                    handler.handle(Future.failedFuture(deleteRes.cause().getMessage()));

                } else {
                    deleteCollectiveAbsence(id, deleteCollRes -> {
                        if (deleteCollRes.isLeft()) {
                            String message = "[Presences@DefaultCollectiveAbsenceService::delete] Failed to delete collective absence.";
                            log.error(message, deleteCollRes.left().getValue());
                            handler.handle(Future.failedFuture(deleteCollRes.left().getValue()));
                        } else {
                            handler.handle(Future.succeededFuture(new JsonObject().put("success", "ok")));
                        }
                    });
                }
            });
        });
    }

    private void deleteAbsences(List<Absence> absences, CollectiveAbsence collective, Handler<AsyncResult<JsonObject>> handler) {
        if (absences.isEmpty()) {
            handler.handle(Future.succeededFuture(new JsonObject().put("success", "ok")));
            return;
        }

        JsonArray statements = new JsonArray();
        List<Integer> absenceIdsToDelete = new ArrayList<>();
        List<Integer> absenceIdsToUnlink = new ArrayList<>();

        for (Absence absence : absences) {
            if (isAbsenceUpdated(absence, collective)) absenceIdsToUnlink.add(absence.getId());
            else {
                absenceIdsToDelete.add(absence.getId());
                statements.add(getDeleteEventsOnDeleteStatement(absence));
                statements.add(getResetEventsOnDeleteStatement(absence));
            }
            collective.getAudiences().stream()
                    .map(Audience::getStudents)
                    .forEach(students -> students.removeIf(student -> student.getId().equals(absence.getStudentId())));
        }
        if (!absenceIdsToDelete.isEmpty()) statements.add(getDeleteAbsenceStatement(absenceIdsToDelete));
        if (!absenceIdsToUnlink.isEmpty()) statements.add(unlinkAbsencesFromCollective(absenceIdsToUnlink));
        sql.transaction(statements, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(handler)));
    }

    private JsonObject getDeleteEventsOnDeleteStatement(Absence absence) {
        String query = "SELECT " + Presences.dbSchema + ".function_delete_events_synchronously(?,?,?)";

        JsonArray params = new JsonArray()
                .add(absence.getStudentId())
                .add(absence.getStartDate())
                .add(absence.getEndDate());

        return new JsonObject()
                .put("action", "prepared")
                .put("statement", query)
                .put("values", params);
    }

    private JsonObject getResetEventsOnDeleteStatement(Absence absence) {
        String query = "UPDATE " + Presences.dbSchema + ".event SET reason_id = null " +
                "WHERE student_id = ? AND start_date < ? AND end_date > ? AND counsellor_input = false AND type_id = "
                + EventTypeEnum.ABSENCE.getType();

        JsonArray params = new JsonArray()
                .add(absence.getStudentId())
                .add(absence.getEndDate())
                .add(absence.getStartDate());

        return new JsonObject()
                .put("action", "prepared")
                .put("statement", query)
                .put("values", params);
    }

    private JsonObject getDeleteAbsenceStatement(List<Integer> absenceIds) {
        String query = "DELETE FROM " + Presences.dbSchema + ".absence WHERE id IN " + Sql.listPrepared(absenceIds);

        JsonArray params = new JsonArray();
        params.addAll(new JsonArray(absenceIds));

        return new JsonObject()
                .put("action", "prepared")
                .put("statement", query)
                .put("values", params);
    }

    private JsonObject unlinkAbsencesFromCollective(List<Integer> absenceIds) {
        String query = "UPDATE FROM " + Presences.dbSchema + ".absence SET collective_id = null WHERE id IN " + Sql.listPrepared(absenceIds);

        JsonArray params = new JsonArray();
        params.addAll(new JsonArray(absenceIds));

        return new JsonObject()
                .put("action", "prepared")
                .put("statement", query)
                .put("values", params);
    }

    private void deleteCollectiveAbsence(Long id, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Presences.dbSchema + ".collective_absence WHERE id = ?";

        JsonArray params = new JsonArray();
        params.add(id);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }


    @Override
    public void getCSV(String structureId, String startDate, String endDate, Handler<AsyncResult<JsonArray>> handler) {

        getCollectives(structureId, startDate, endDate, null, null, null, null, result -> {

            JsonArray collectivesRes = result.result().getJsonArray("all", new JsonArray());
            JsonArray collectives = new JsonArray();

            List<Future<Void>> futures = new ArrayList<>();

            for (int collectiveIndex = 0; collectiveIndex < collectivesRes.size(); collectiveIndex++) {

                Future<Void> future = Future.future();
                futures.add(future);

                JsonObject collectiveRes = collectivesRes.getJsonObject(collectiveIndex);
                JsonObject collective = new JsonObject();
                JsonObject reason = collectiveRes.getJsonObject("reason") != null ? collectiveRes.getJsonObject("reason") : new JsonObject();
                collective.put("reason", reason.getString("label", ""));
                collective.put("comment", collectiveRes.getString("comment", ""));
                collective.put("startDate", collectiveRes.getString("startDate", ""));
                collective.put("endDate", collectiveRes.getString("endDate", ""));
                collective.put("countStudent", collectiveRes.getLong("countStudent"));
                collective.put("id", collectiveRes.getLong("id"));


                get(structureId, collectiveRes.getLong("id"), collectiveFromId -> {

                    JsonArray audiencesRes = collectiveFromId.result().getJsonArray("audiences");
                    JsonArray students = new JsonArray();

                    for (int audienceIndex = 0; audienceIndex < audiencesRes.size(); audienceIndex++) {
                        JsonObject audienceRes = audiencesRes.getJsonObject(audienceIndex);
                        JsonArray studentsRes = audienceRes.getJsonArray("students", new JsonArray());
                        for (int studentIndex = 0; studentIndex < studentsRes.size(); studentIndex++) {
                            JsonObject studentRes = studentsRes.getJsonObject(studentIndex);
                            JsonObject student = new JsonObject();

                            student.put("firstName", studentRes.getString("firstName", ""));
                            student.put("lastName", studentRes.getString("lastName", ""));
                            student.put("audienceName", audienceRes.getString("name", ""));

                            students.add(student);
                        }
                    }

                    collective.put("students", students);
                    collectives.add(collective);

                    future.complete();
                });
            }

            FutureHelper.all(futures).setHandler(res -> {
                if (res.failed()) {
                    String message = "[Presences@DefaultCollectiveAbsenceService::getCSV] Failed to generate CSV file.";
                    handler.handle(Future.failedFuture(message + " " + result.cause()));
                }

                handler.handle(Future.succeededFuture(collectives));
            });
        });
    }
}
