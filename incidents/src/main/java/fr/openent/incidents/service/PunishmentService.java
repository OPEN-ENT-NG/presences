package fr.openent.incidents.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.Map;

public interface PunishmentService {

    /**
     * get punishment
     *
     * @param user      current user logged
     * @param body      data to filter
     * @param isStudent if user is a student (and need to be filtered about that).
     * @param handler   Function handler returning data
     */
    void get(UserInfos user, MultiMap body, boolean isStudent, Handler<AsyncResult<JsonObject>> handler);

    void get(UserInfos user, String id, String structureId, String startAt, String endAt, List<String> studentIds, List<String> groupIds,
             List<String> typeIds, List<String> processStates, boolean isStudent, String pageString, String limitString, String offsetString,
             Handler<AsyncResult<JsonObject>> handler);

    Future<JsonObject> get(UserInfos user, String id, String groupedPunishmentId, String structureId, String startAt, String endAt, List<String> studentIds, List<String> groupIds,
                           List<String> typeIds, List<String> processStates, boolean isStudent, String pageString, String limitString, String offsetString);

    /**
     * get Sanctions/Punishments by students
     *
     * @param structure  structure identifier
     * @param startAt    start date
     * @param endAt      end date
     * @param students   List of students
     * @param typeIds    List of punishment type
     * @param eventType  Punishment event type ("SANCTION"/ "PUNISHMENT")
     * @param processed  filter processed
     * @param massmailed filter massmailed
     * @param handler    Function handler returning data
     */
    void getPunishmentByStudents(String structure, String startAt, String endAt, List<String> students, List<Integer> typeIds,
                                 String eventType, Boolean processed, Boolean massmailed, Handler<Either<String, JsonArray>> handler);

    /**
     * get Punishments/Sanctions COUNT By students
     *
     * @param structure  structure identifier
     * @param startAt    start date
     * @param endAt      end date
     * @param students   List of students
     * @param typeIds    List of punishment type
     * @param processed  filter processed
     * @param massmailed filter massmailed
     * @param handler    Function handler returning data
     */
    void getPunishmentCountByStudent(String structure, String startAt, String endAt, List<String> students, List<Integer> typeIds,
                                     Boolean processed, Boolean massmailed, Handler<Either<String, JsonArray>> handler);


    /**
     * update punishment massmailing
     *
     * @param punishmentsIds List of punishment identifier
     * @param isMassmailed   value massmailing to update for each punishment identifier
     * @param handler        Function handler returning data
     */
    void updatePunishmentMassmailing(List<String> punishmentsIds, Boolean isMassmailed, Handler<Either<String, JsonObject>> handler);

    /**
     * get punishment
     *
     * @param user      current user logged
     * @param body      data to filter
     * @param isStudent if user is a student (and need to be filtered about that).
     * @param handler   Function handler returning number of punishments
     */
    void count(UserInfos user, MultiMap body, boolean isStudent, Handler<AsyncResult<Long>> handler);


    void count(UserInfos user, String structureId, String startAt, String endAt, List<String> studentIds,
               List<String> groupIds, List<String> typeIds, List<String> processStates, boolean isStudent, Handler<AsyncResult<Long>> handler);

    /**
     * create punishment
     *
     * @param user    current user logged
     * @param body    data to store
     * @param handler Function handler returning data
     */
    void create(UserInfos user, JsonObject body, Handler<AsyncResult<JsonArray>> handler);

    /**
     * put punishment
     *
     * @param user    current user logged
     * @param body    data to update
     * @param handler Function handler returning data
     */
    void update(UserInfos user, JsonObject body, Handler<AsyncResult<JsonArray>> handler);

    /**
     * delete punishment
     *
     * @param user                User
     * @param structureId         Structure identifier
     * @param punishmentId        Delete one punishment (by id)
     * @param groupedPunishmentId Delete grouped punishments
     * @return returning data
     */
    Future<JsonObject> delete(UserInfos user, String structureId, String punishmentId, String groupedPunishmentId);

    /**
     * get absences by students
     *
     * @param studentIds student list identifiers
     * @param starDate   start period to get
     * @param endDate    end period to get
     */
    void getAbsencesByStudentIds(List<String> studentIds, String starDate, String endDate, Handler<AsyncResult<Map<String, List<JsonObject>>>> handler);
}
