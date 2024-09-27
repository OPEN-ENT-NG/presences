package fr.openent.statistics_presences.service.impl;


import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DBService;
import fr.openent.presences.model.StatisticsUser;
import fr.openent.presences.model.StructureStatisticsUser;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.indicator.ComputeStatistics;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import fr.openent.statistics_presences.service.StatisticsPresencesService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultStatisticsPresencesService extends DBService implements StatisticsPresencesService {
    private final CommonServiceFactory commonServiceFactory;
    Logger log = LoggerFactory.getLogger(DefaultStatisticsPresencesService.class);

    public DefaultStatisticsPresencesService(CommonServiceFactory commonServiceFactory) {
        this.commonServiceFactory = commonServiceFactory;
    }

    @Override
    public void create(String structureId, List<String> studentIds, Handler<AsyncResult<JsonObject>> handler) {
        Viescolaire.getInstance().getSchoolYear(structureId)
                .onSuccess(schoolYear -> {
                    String startDate = schoolYear.getString(Field.START_DATE, "now()");
                    JsonArray statements = new JsonArray(studentIds.stream().map(studentId -> createStatisticsUserStatement(structureId, studentId, startDate)).collect(Collectors.toList()));
                    sql.transaction(statements, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(handler)));
                })
                .onFailure(error -> {
                    log.error(String.format("[StatisticsPresences@%s::create] Failed to get school year %s",
                            this.getClass().getSimpleName(), error.getMessage()));
                    handler.handle(Future.failedFuture(error));
                });
    }

    @Override
    public void createWithModifiedDate(String structureId, List<StatisticsUser> studentIdModifiedDateMap, Handler<AsyncResult<JsonObject>> handler) {
        JsonArray statements = new JsonArray();
        studentIdModifiedDateMap.forEach(statisticsUser -> statements.add(createStatisticsUserStatement(structureId, statisticsUser.getId(), statisticsUser.getModified())));
        sql.transaction(statements, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(handler)));
    }

    @Override
    public Future<JsonObject> processStatisticsPrefetch(List<String> structures, List<String> studentIds, Boolean isWaitingEndProcess) {
        Promise<JsonObject> promise = Promise.promise();
        if (structures.isEmpty()) {
            promise.fail("No structure(s) identifier given");
        } else {
            JsonObject params = new JsonObject()
                    .put(Field.STRUCTURE, structures)
                    .put(Field.STUDENTIDS, studentIds)
                    .put(Field.ISWAITINGENDPROCESS, isWaitingEndProcess);
            if (Boolean.TRUE.equals(isWaitingEndProcess)) {
                ComputeStatistics computeStatistics = new ComputeStatistics(this.commonServiceFactory);
                computeStatistics.start(structures, studentIds)
                        .onSuccess(res -> promise.complete(new JsonObject().put(Field.MESSAGE, Field.OK)))
                        .onFailure(promise::fail);
            } else {
                StatisticsPresences.launchProcessingStatistics(commonServiceFactory.eventBus(), params)
                        .onSuccess(promise::complete)
                        .onFailure(error -> {
                            log.error(String.format("[StatisticsPresences@%s::processStatisticsPrefetch] Failed to launch processing statistics %s",
                                    this.getClass().getSimpleName(), error.getMessage()));
                            promise.fail(error);
                        });
            }
        }
        return promise.future();
    }

    @Override
    public Future<JsonObject> processStatisticsPrefetch(List<StructureStatisticsUser> structureStatisticsUserList, Boolean isWaitingEndProcess) {
        Promise<JsonObject> promise = Promise.promise();
        if (structureStatisticsUserList.isEmpty()) {
            return Future.succeededFuture(new JsonObject());
        } else {
            List<Future<JsonObject>> futureList = new ArrayList<>();

            for (StructureStatisticsUser structureStatisticsUser : structureStatisticsUserList) {
                futureList.add(Viescolaire.getInstance().getSchoolYear(structureStatisticsUser.getStructureId())
                        .onSuccess(schoolYear -> {
                            String startDate = schoolYear.getString(Field.START_DATE, "2000-01-01T00:00:00");
                            structureStatisticsUser.getStatisticsUsers().forEach(statisticsUser -> statisticsUser.setModified(startDate));
                        })
                        .onFailure(error -> log.error(String.format("[StatisticsPresences@%s::processStatisticsPrefetch] Failed to get school year for id %s %s",
                                this.getClass().getSimpleName(), structureStatisticsUser.getStructureId(), error.getMessage())))
                );
            }

            Future.all(futureList)
                    .onSuccess(result -> {
                        JsonObject params = new JsonObject()
                                .put(Field.STRUCTURE_STATISTICS_USERS, IModelHelper.toJsonArray(structureStatisticsUserList))
                                .put(Field.ISWAITINGENDPROCESS, isWaitingEndProcess);
                        if (Boolean.TRUE.equals(isWaitingEndProcess)) {
                            ComputeStatistics computeStatistics = new ComputeStatistics(this.commonServiceFactory);
                            computeStatistics.start(structureStatisticsUserList)
                                    .onSuccess(res -> promise.complete(new JsonObject().put(Field.MESSAGE, Field.OK)))
                                    .onFailure(promise::fail);
                        } else {
                            StatisticsPresences.launchProcessingStatistics(commonServiceFactory.eventBus(), params)
                                    .onSuccess(promise::complete)
                                    .onFailure(error -> {
                                        log.error(String.format("[StatisticsPresences@%s::processStatisticsPrefetch] Failed to launch processing statistics %s",
                                                this.getClass().getSimpleName(), error.getMessage()));
                                        promise.fail(error);
                                    });
                        }
                    })
                    .onFailure(promise::fail);
        }
        return promise.future();
    }


    /**
     * Get statement that create statistic user
     *
     * @param structureId structure identifier
     * @param studentId   user student identifier
     * @return Statement
     */
    private JsonObject createStatisticsUserStatement(String structureId, String studentId, String modified) {

        String query = " INSERT INTO " + StatisticsPresences.DB_SCHEMA + ".user(id, structure, modified) " +
                " VALUES (?, ?, ?) " +
                " ON CONFLICT (id, structure) DO UPDATE SET modified = ?;";

        JsonArray values = new JsonArray()
                .add(studentId)
                .add(structureId)
                .add(modified)
                .add(modified);

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    @Override
    public Future<List<StructureStatisticsUser>> fetchUsers(List<String> structureIdList, String startDate) {
        Promise<List<StructureStatisticsUser>> promise = Promise.promise();

        List<StructureStatisticsUser> structureStatisticsUserList = new ArrayList<>();
        commonServiceFactory.userService().fetchAllStudentsFromStructure(structureIdList)
                .compose(structuresResult -> {
                    List<Future<JsonObject>> futureList = new ArrayList<>();

                    structuresResult.stream()
                            .map(JsonObject.class::cast)
                            .forEach(struct -> {
                                JsonArray users = struct.getJsonArray(Field.USERS, new JsonArray());
                                if (!users.isEmpty()) {
                                    StructureStatisticsUser structureStatisticsUser = new StructureStatisticsUser()
                                            .setStructureId(struct.getString(Field.STRUCTURE))
                                            .setStatisticsUsers(
                                                    struct.getJsonArray(Field.USERS).stream()
                                                            .map(String.class::cast)
                                                            .map(studentId -> new StatisticsUser().setId(studentId)
                                                                            .setStructureId(struct.getString(Field.STRUCTURE)))
                                                            .collect(Collectors.toList())
                                            );
                                    structureStatisticsUserList.add(structureStatisticsUser);
                                    Future<JsonObject> schoolDateFuture = startDate == null ?
                                            Viescolaire.getInstance().getSchoolYear(structureStatisticsUser.getStructureId()) :
                                            Future.succeededFuture(new JsonObject().put(Field.START_DATE, startDate));
                                    futureList.add(schoolDateFuture);
                                    schoolDateFuture
                                            .onSuccess(schoolDate -> {
                                                String startingDate = schoolDate.getString(Field.START_DATE, "2000-01-01 00:00:00");
                                                structureStatisticsUser.getStatisticsUsers().forEach(statisticsUser -> statisticsUser.setModified(startingDate));
                                            })
                                            .onFailure(error -> {
                                                log.error(String.format("[StatisticsPresences@%s::fetchUsers] Fail to fetch users %s",
                                                        this.getClass().getSimpleName(), error.getMessage()));
                                                promise.fail(error);
                                            });
                                }
                            });
                    return Future.all(futureList);
                })
                .onSuccess(result -> promise.complete(structureStatisticsUserList))
                .onFailure(error -> {
                    log.error(String.format("[Statistics@DefaultStatisticsPresencesService::fetchUsers] " +
                            "Failed to fetch user. %s", error.getMessage()));
                    promise.fail(error);
                });
        return promise.future();
    }

    @Override
    public Future<List<StructureStatisticsUser>> fetchUsers(List<String> structureIdList) {
        Promise<List<StructureStatisticsUser>> promise = Promise.promise();

        this.fetchUsers(structureIdList, null)
                .onSuccess(promise::complete)
                .onFailure(promise::fail);

        return promise.future();
    }

    @Override
    public Future<StructureStatisticsUser> fetchUsers(String structureId, List<String> studentIdList) {
        Promise<StructureStatisticsUser> promise = Promise.promise();

        Viescolaire.getInstance().getSchoolYear(structureId)
                .onSuccess(schoolDate -> {
                    String startDate = schoolDate.getString(Field.START_DATE, "2000-01-01 00:00:00");
                    StructureStatisticsUser structureStatisticsUser = new StructureStatisticsUser()
                            .setStructureId(structureId)
                            .setStatisticsUsers(
                                    studentIdList.stream()
                                            .map(studentId -> new StatisticsUser().setId(studentId).setModified(startDate))
                                            .collect(Collectors.toList())
                            );
                    promise.complete(structureStatisticsUser);
                })
                .onFailure(error -> {
                    log.error(String.format("[StatisticsPresences@%s::fetchUsers] Failed to get school year for id %s %s",
                            this.getClass().getSimpleName(), structureId, error.getMessage()));
                    promise.fail(error);
                });

        return promise.future();
    }

    @Override
    public Future<List<StructureStatisticsUser>> fetchUsers() {
        Promise<List<StructureStatisticsUser>> promise = Promise.promise();
        String query = String.format("SELECT structure as structure_id, json_object_agg(id, modified) as statistics_users" +
                " FROM %s.user GROUP BY structure_id;", StatisticsPresences.DB_SCHEMA);

        Sql.getInstance().raw(query, SqlResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[Statistics@DefaultStatisticsPresencesService::fetchUsers] " +
                        "Failed to retrieve users to process. %s", either.left().getValue()));
                promise.fail(either.left().getValue());
            } else {
                JsonArray result = either.right().getValue();
                List<StructureStatisticsUser> structureStatisticsUserList = result.stream()
                        .map(JsonObject.class::cast)
                        .map(structureStatisticsUser -> {
                            structureStatisticsUser.getString(Field.STATISTICS_USERS);
                            final List<StatisticsUser> statisticsUserList = new JsonObject(structureStatisticsUser.getString(Field.STATISTICS_USERS)).stream()
                                    .map(statisticsUser -> new StatisticsUser().setId(statisticsUser.getKey())
                                            .setModified(statisticsUser.getValue().toString())
                                            .setStructureId(structureStatisticsUser.getString(Field.STRUCTURE_ID)))
                                    .collect(Collectors.toList());
                            return new StructureStatisticsUser()
                                    .setStructureId(structureStatisticsUser.getString(Field.STRUCTURE_ID))
                                    .setStatisticsUsers(statisticsUserList);
                        })
                        .filter(structureStatisticsUser -> !structureStatisticsUser.getStatisticsUsers().isEmpty())
                        .collect(Collectors.toList());
                promise.complete(structureStatisticsUserList);
            }
        }));

        return promise.future();
    }

    @Override
    public Future<Void> clearWaitingList(List<String> studentIdList) {
        if (studentIdList.isEmpty()) {
            return Future.succeededFuture();
        }
        Promise<Void> promise = Promise.promise();
        String query = String.format("DELETE FROM %s.user WHERE id IN " + Sql.listPrepared(studentIdList) + " ;", StatisticsPresences.DB_SCHEMA);
        JsonArray params = new JsonArray().addAll(new JsonArray(studentIdList));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[Statistics@%s::clearWaitingList] Fail to clear waiting list %s",
                        this.getClass().getSimpleName(), either.left().getValue()));
                promise.fail(either.left().getValue());
            }
            else promise.complete();
        }));

        return promise.future();
    }

    @Override
    public Future<Void> clearWaitingList() {
        Promise<Void> promise = Promise.promise();
        String query = String.format("TRUNCATE TABLE %s.user;", StatisticsPresences.DB_SCHEMA);
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[Statistics@%s::clearWaitingList] Fail to clear waiting list %s",
                        this.getClass().getSimpleName(), either.left().getValue()));
                promise.fail(either.left().getValue());
            }
            else promise.complete();
        }));

        return promise.future();
    }
}
