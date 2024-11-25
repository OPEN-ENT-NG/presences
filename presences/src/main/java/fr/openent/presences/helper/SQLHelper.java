package fr.openent.presences.helper;

import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;

import java.util.List;

public class SQLHelper {

    private SQLHelper() {
    }


    public static String addLimitOffset(Integer limit, Integer offset, JsonArray params) {
        String query = "";

        if (offset != null) {
            query += " OFFSET ?";
            params.add(offset);
        }

        if (limit != null) {
            query += " LIMIT ?";
            params.add(limit);
        }

        return query;
    }

    public static String structureId(String structureId, JsonArray params) {

        if (structureId != null) {
            params.add(structureId);
            return " structure_id = ?";
        }
        return "";
    }
    public static String filterList(String field, List<?> list, JsonArray params){
        if (!list.isEmpty()) {
            params.addAll(new JsonArray(list));
            return String.format(" %s IN %s", field, Sql.listPrepared(list));
        }
        return "";
    }

    public static String startDateStartTime(String startDate, String startTime, JsonArray params) {
        if (startDate != null && startTime != null) {
            params.add(startDate);
            params.add(startTime);
            return " date >= ?::date + ?::time";
        }
        return "";
    }

    public static String endDateEndTime(String endDate, String endTime, JsonArray params) {
        if (endDate != null && endTime != null) {
            params.add(endDate);
            params.add(endTime);
            return " date <= ?::date + ?::time";
        }
        return "";
    }
}
