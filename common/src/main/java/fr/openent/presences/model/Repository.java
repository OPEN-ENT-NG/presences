package fr.openent.presences.model;


import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Repository {

    public static void create(Model model, Handler<AsyncResult<JsonObject>> handler) {
        List<String> createFieldList = getFieldList(model, "CREATE");
        String query = getCreateQuery(model, createFieldList);
        persist(model, query, createFieldList, handler);
    }

    public static void update(Model model, Handler<AsyncResult<JsonObject>> handler) {
        List<String> updateFieldList = getFieldList(model, "UPDATE");

        try {
            String query = getUpdateQuery(model, updateFieldList);
            persist(model, query, updateFieldList, handler);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            handler.handle(Future.failedFuture("[Common@Repository::update] Field access exception"));
        }
    }

    public static String getCreateQuery(Model model, List<String> createFieldList) {
        return " INSERT INTO " + model.table +
                " (" + fieldsToStrList(createFieldList) + ") " +
                " VALUES (" + fieldsToQuestionMarks(createFieldList) + ") RETURNING *";
    }

    public static String getUpdateQuery(Model model, List<String> updateFieldList) throws NoSuchFieldException, IllegalAccessException {
        Field primaryKeyValue = model.getClass().getDeclaredField(model.primaryKey);
        primaryKeyValue.setAccessible(true);

         return " UPDATE " + model.table +
                " SET " + fieldsToAttributeQuestionMarks(updateFieldList) +
                " WHERE " + model.primaryKey + " = " + primaryKeyValue.get(model) + " RETURNING *";
    }

    public static List<String> getFieldList(Model model, String method) {
        Stream<Map.Entry<String, List<String>>> fieldStream = model.fillables.entrySet().stream();
        if (method != null) fieldStream = fieldStream.filter(item -> {
            boolean usePrimaryKey = true;
            if (method.equals("UPDATE")) {
                usePrimaryKey = !item.getKey().equals(model.primaryKey);
            }
            return item.getValue().contains(method) && usePrimaryKey;
        });

        return fieldStream
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public static String fieldsToStrList(List<String> fieldList) {
        return String.join(", ", fieldList);
    }

    public static String fieldsToQuestionMarks(List<String> fieldList) {
        String questionMarks = new String(new char[fieldList.size()]).replace("\0", "?, ");
        return questionMarks.substring(0, questionMarks.length() - 2);
    }

    public static String fieldsToAttributeQuestionMarks(List<String> fieldList) {
        StringBuilder resultBuilder = new StringBuilder();
        for (String field : fieldList) {
            resultBuilder.append(field).append(" = ?, ");
        }
        String result = resultBuilder.toString();
        if (fieldList.size() > 0) return result.substring(0, result.length() - 2);
        return result;
    }

    public static JsonArray getParameters(Model model, List<String> fieldList, Handler<AsyncResult<JsonObject>> handler) {
        JsonArray params = new JsonArray();
        fieldList.forEach(field -> {
            try {
                Field f = model.getClass().getDeclaredField(field);
                f.setAccessible(true);
                if (f.get(model) == null) params.addNull();
                else params.add(f.get(model));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                handler.handle(Future.failedFuture("[Common@Repository::persist] Field access exception"));
            }
        });
        return params;
    }

    public static void getStatement(Model model, String method, Handler<AsyncResult<JsonObject>> handler)  {
        List<String> fieldList = getFieldList(model, method);
        JsonArray params = getParameters(model, fieldList, handler);
        String query;
        try {
            switch (method) {
                case "CREATE": {
                    query = getCreateQuery(model, fieldList);
                    break;
                }
                case "UPDATE": {
                    query = getUpdateQuery(model, fieldList);
                    break;
                }
                default: {
                    handler.handle(Future.failedFuture("[Common@Repository::getStatement] Unrecognized method " + method + "."));
                    return;
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            handler.handle(Future.failedFuture("[Common@Repository::getStatement] Field access exception"));
            return;
        }

        handler.handle(Future.succeededFuture(
                new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared")
        ));

    }

    public static void persist(Model model, String query, List<String> fieldList, Handler<AsyncResult<JsonObject>> handler) {
        JsonArray params = getParameters(model, fieldList, handler);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Common@Repository::persist] Failed to persist in database : ";
                handler.handle(Future.failedFuture(message + " " + result.left().getValue()));
                return;
            }
            handler.handle(Future.succeededFuture(result.right().getValue()));
        }));
    }

    public static void persist(JsonArray statements, Handler<AsyncResult<JsonArray>> handler) {
        Sql.getInstance().transaction(statements, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Common@Repository::persist] Failed to persist statements in database : ";
                handler.handle(Future.failedFuture(message + " " + result.left().getValue()));
                return;
            }

            handler.handle(Future.succeededFuture(result.right().getValue()));
        }));
    }

}
