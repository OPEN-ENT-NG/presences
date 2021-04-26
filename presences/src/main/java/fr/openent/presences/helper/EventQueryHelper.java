package fr.openent.presences.helper;

import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;

import java.util.List;

public class EventQueryHelper {
    public static final String DEFAULT_START_TIME = "00:00:00";
    public static final String DEFAULT_END_TIME = "23:59:59";

    // This query will set true if at the events he regrouped is all regularized, otherwise will set false
    public static final String MAIN_COUNSELLOR_REGULARIZED_QUERY = " bool_and(e.counsellor_regularisation) AS counsellor_regularisation, ";
    // This query will set true if at the events he regrouped is all massmailed, otherwise will set false
    public static final String MAIN_MASSMAILED_QUERY = " bool_and(e.massmailed) as massmailed ";

    private EventQueryHelper() {
        throw new IllegalStateException("Utility EventQueryHelper class");
    }

    /**
     * This method returns a Query that add register's INNER JOIN
     *
     * @param structureId structure identifier
     * @param params      JsonArray params where you add the specific structure identifier (using filterStructureId)
     * @return {String}
     */
    public static String joinRegister(String structureId, JsonArray params) {
        return " INNER JOIN presences.register AS r " +
                " ON (r.id = e.register_id AND r." + EventQueryHelper.filterStructureId(structureId, params) + ") ";
    }

    /**
     * This method simplifies the setter for structure
     *
     * @param structureId structure identifier
     * @param params      JsonArray params where you add the specific structure identifier
     * @return {String}
     */
    public static String filterStructureId(String structureId, JsonArray params) {
        params.add(structureId);
        return "structure_id = ?";
    }

    /**
     * This method add action column
     *
     * @param isWithNullAction option that allow us to/not to add action column
     * @return {String}
     */
    public static String addActionColumn(boolean isWithNullAction) {
        if (isWithNullAction) return "null as action, ";
        return " (CASE WHEN count(DISTINCT event_actions.action_id ) > 1 " +
                " THEN 'MULTIPLES'                                                                                                                                                                                                            \n" +
                " ELSE (array_agg(actions.abbreviation ORDER BY event_actions.created_date DESC))[1] END) AS action_abbreviation, ";
    }

    /**
     * This method add main reason event
     *
     * @return {String}
     */
    public static String addMainReasonEvent() {
        return " (CASE " +
                " WHEN count(DISTINCT CASE WHEN e.type_id = 1 then coalesce(e.reason_id, 1) end) > 1 " +
                " THEN -1 " +
                " ELSE (array_agg(e.reason_id) FILTER (WHERE e.type_id = 1))[1] END) AS reason_id, ";
    }

    /**
     * This method add join action query
     *
     * @param structureId      structure identifier
     * @param isWithNullAction option that allow us to/not to add action column  (in this case we add its join)
     * @param params           JsonArray params where you add the specific structure identifier (using filterStructureId)
     * @return {String}
     */
    public static String joinActions(String structureId, boolean isWithNullAction, JsonArray params) {
        if (isWithNullAction) return "";
        return " INNER JOIN ( " +
                " SELECT DISTINCT ON (event_id) event_id, action_id, created_date FROM presences.event_actions " +
                " ORDER BY event_id DESC, created_date DESC" +
                " ) AS event_actions ON (event_actions.event_id = e.id)" +
                " INNER JOIN presences.actions as actions ON (actions.id = event_actions.action_id AND " +
                " actions." + filterStructureId(structureId, params) + ") ";
    }

    /**
     * This method add eventType join query
     *
     * @param eventType List of EventType
     * @param params    JsonArray params where you add the specific EventType
     * @return {String}
     */
    public static String joinEventType(List<String> eventType, JsonArray params) {
        if (eventType != null && !eventType.isEmpty()) {
            params.addAll(new JsonArray(eventType));
            return " INNER JOIN presences.event_type AS event_type ON (event_type.id = e.type_id  AND e.type_id IN "
                    + Sql.listPrepared(eventType.toArray()) + ") ";
        } else {
            return "";
        }
    }

    /**
     * This method filter the event's type_id
     *
     * @param typeIds List of typeIds
     * @param params  JsonArray params where you add the specific typeIds JsonArray
     * @return {String}
     */
    public static String filterTypes(List<String> typeIds, JsonArray params) {
        if (typeIds != null && !typeIds.isEmpty()) {
            params.addAll(new JsonArray(typeIds));
            return " AND e.type_id IN " + Sql.listPrepared(typeIds);
        }
        return "";
    }

    /**
     * This method filter the date
     *
     * @param startDate startDate
     * @param endDate   endDate
     * @param params    JsonArray params where you add the specific start and end date
     * @return {String}
     */
    public static String filterDates(String startDate, String endDate, JsonArray params) {
        params.add(startDate + " " + DEFAULT_START_TIME);
        params.add(endDate + " " + DEFAULT_END_TIME);
        return " WHERE e.start_date > ? AND e.end_date < ? ";
    }

    /**
     * This method filter the time
     *
     * @param startTime startTime
     * @param endTime   endTime
     * @param params    JsonArray params where you add the specific start and end time
     * @return {String}
     */
    public static String filterTimes(String startTime, String endTime, JsonArray params) {
        String query = "";

        if (endTime != null) {
            query += " AND e.start_date::time < ? ";
            params.add(endTime);
        }

        if (startTime != null) {
            query += " e.end_date::time > ? ";
            params.add(startTime);
        }

        return query;
    }

    /**
     * This method filter reason (by including several rules between regularized and reasons)
     *
     * @param reasonIds   list of reason ids
     * @param noReason    no reason wish to not display boolean
     * @param regularized regularized wish to display boolean
     * @param params      JsonArray params where you add the reasonIds, no reason and regularized boolean
     * @return {String}
     */
    public static String filterReasons(List<String> reasonIds, Boolean noReason, Boolean regularized, JsonArray params) {
        String query = "";

        // this condition occurs when we want to filter no reason and regularized event at the same time
        if ((noReason != null && noReason) && (regularized != null && regularized)) {
            query += "AND (reason_id IS NULL OR (reason_id IS NOT NULL AND counsellor_regularisation = true)";
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
                query += " AND (reason_id IN " + Sql.listPrepared(reasonIds) + (noReason != null && noReason ? " OR reason_id IS NULL" : "") + ") ";
                params.addAll(new JsonArray(reasonIds));
            }

            // If we want to fetch events with NO reasonId, array reasonIds fetched is empty
            // AND noReason is TRUE
            if (reasonIds != null && reasonIds.isEmpty() && (noReason != null && noReason)) {
                query += " AND (reason_id IS NULL " + (regularized != null ? " OR counsellor_regularisation = " + regularized + "" : "") + ") ";
            }

            if (regularized != null) {
                query += " AND counsellor_regularisation = " + regularized + " ";
            }
        }

        return query;
    }

    /**
     * This method filter students
     *
     * @param studentIds list of students ids
     * @param params     JsonArray params where you add the specific list of student ids
     * @return {String}
     */
    public static String filterStudentIds(List<String> studentIds, JsonArray params) {
        if (studentIds != null && !studentIds.isEmpty()) {
            params.addAll(new JsonArray(studentIds));
            return " AND e.student_id IN " + Sql.listPrepared(studentIds);
        }
        return "";
    }

    /**
     * This method filter followed events
     *
     * @param followed followed boolean
     * @param params   JsonArray params where you add the specific followed boolean
     * @return {String}
     */
    public static String filterFollowed(Boolean followed, JsonArray params) {
        if (followed != null) {
            params.add(followed);
            return " AND followed = ? ";
        }
        return "";
    }

    /**
     * This filter events that have no action for a student and a day
     *
     * @param isWithNullAction option that allow us to/not to add action column  (in this case we add its join)
     * @return {String}
     */
    public static String filterNoActions(boolean isWithNullAction) {
        if (!isWithNullAction) return "";
        return " AND NOT EXISTS(SELECT NULL FROM students_with_actions as swa WHERE swa.student_id = e.student_id AND swa.date = e.start_date::date) ";
    }
}