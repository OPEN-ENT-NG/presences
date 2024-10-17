package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.ModelHelper;
import fr.openent.presences.helper.ExemptionHelper;
import fr.openent.presences.model.Exemption.ExemptionBody;
import fr.openent.presences.model.Exemption.ExemptionView;
import fr.openent.presences.service.ExemptionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.*;

public class DefaultExemptionService extends SqlCrudService implements ExemptionService {
    private final static String DATABASE_TABLE = "exemption";
    private final static String DATABASE_TABLE_RECURSIVE = "exemption_recursive";
    private final static String DATABASE_TABLE_VIEW = "exemption_view";
    private ExemptionHelper exemptionHelper;

    public DefaultExemptionService(EventBus eb) {
        super(Presences.dbSchema, DATABASE_TABLE);
        this.exemptionHelper = new ExemptionHelper(eb, DATABASE_TABLE, DATABASE_TABLE_RECURSIVE);
    }

    @Override
    public void get(String structureId, String startDate, String endDate, List<String> studentIds, String page,
                    String order, boolean reverse, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * " + this.getterFROMBuilder(DATABASE_TABLE_VIEW, studentIds);
        JsonArray values = new JsonArray()
                .add(structureId)
                .add(endDate)
                .add(startDate);

        if (studentIds != null && !studentIds.isEmpty()) {
            values.addAll(new JsonArray(studentIds));
        }

        if (page != null) {
            query += " ORDER BY " + exemptionHelper.getSqlOrderValue(order) + " " + exemptionHelper.getSqlReverseString(reverse);
            query += " OFFSET ? LIMIT ?";
            values.add(Presences.PAGE_SIZE * Integer.parseInt(page));
            values.add(Presences.PAGE_SIZE);
        }

        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>(event.left().getValue()));
            } else {
                List<ExemptionView> exemptions = ExemptionHelper.getExemptionListFromJsonArray(event.right().getValue());
                if (exemptions.isEmpty()) {
                    handler.handle(new Either.Right<>(ModelHelper.convertToJsonArray(exemptions)));
                } else if (studentIds == null) {
                    handler.handle(new Either.Right<>(new JsonArray()));
                } else {
                    Promise<JsonObject> userInfoPromise = Promise.promise();
                    Promise<JsonObject> subjectInfoPromise = Promise.promise();

                    exemptionHelper.addUsersInfo(structureId, exemptions, userInfoPromise.future());
                    exemptionHelper.addSubjectsInfo(exemptions, subjectInfoPromise.future());

                    CompositeFuture.all(userInfoPromise.future(), subjectInfoPromise.future())
                            .onFailure(fail -> handler.handle(new Either.Left<>(fail.getMessage())))
                            .onSuccess(evt -> handler.handle(new Either.Right<>(ModelHelper.convertToJsonArray(exemptions))));
                }
            }
        }));
    }

    @Override
    public void get(String structureId, String startDate, String endDate, String userId, String page,
                    Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * " + this.getterFROMBuilder(DATABASE_TABLE, Collections.singletonList(userId));
        JsonArray values = new JsonArray()
                .add(structureId)
                .add(endDate)
                .add(startDate);

        if (userId != null) {
            values.addAll(new JsonArray(Collections.singletonList(userId)));
        }

        if (page != null) {
            query += " ORDER BY start_date";
            query += " OFFSET ? LIMIT ?";
            values.add(Presences.PAGE_SIZE * Integer.parseInt(page));
            values.add(Presences.PAGE_SIZE);
        }

        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getPageNumber(String structureId, String startDate, String endDate, List<String> studentIds,
                              String order, boolean reverse, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray()
                .add(structureId)
                .add(endDate)
                .add(startDate);

        if (studentIds != null && !studentIds.isEmpty()) {
             params.addAll(new JsonArray(studentIds));
        }

        String query =
                "SELECT count(" + Presences.dbSchema + "." + DATABASE_TABLE_VIEW + ".exemption_id)" +
                        this.getterFROMBuilder(DATABASE_TABLE_VIEW, studentIds);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private String getterFROMBuilder(String databaseTable, List<String> studentIds) {
        StringBuilder query = new StringBuilder(" FROM " + Presences.dbSchema + "." + databaseTable +
                " WHERE structure_id = ?");
        if (databaseTable.equals(DATABASE_TABLE_VIEW)) {
            query.append(" AND recursive_id IS NULL");
        }
        query.append(" AND ((start_date <= ?) AND (end_date >= ?))");

        if (studentIds != null && !studentIds.isEmpty()) {
            query.append(" AND student_id IN ");
            query.append(Sql.listPrepared(studentIds));
        }
        return query.toString();
    }

    @Override
    public void getRegisterExemptions(List<String> studentList, String structure_id, String start_date, String end_date,
                                      Handler<Either<String, JsonArray>> handler) {
        if(studentList.isEmpty()) {
            handler.handle(new Either.Right<>(new JsonArray()));
            return;
        }

        String query = "SELECT student_id, subject_id, recursive_id, attendance FROM " + Presences.dbSchema + "." +
                DATABASE_TABLE + " WHERE start_date <= ? AND end_date >= ? AND student_id IN " + Sql.listPrepared(studentList);
        JsonArray params = new JsonArray()
                .add(start_date)
                .add(end_date)
                .addAll(new JsonArray(studentList));
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(ExemptionBody exemptionBody, Handler<Either<String, JsonArray>> handler) {
        if (!exemptionBody.isRecursive()) {
            exemptionHelper.createPunctualExemption(exemptionBody, handler);
        } else {
            exemptionHelper.createRecursiveExemption(exemptionBody, handler);
        }
    }

    @Override
    public void update(Integer id, ExemptionBody exemptionBody, Handler<Either<String, JsonObject>> handler) {
        if (!exemptionBody.isRecursive()) {
            exemptionHelper.updatePunctualExemption(id, exemptionBody, handler);
        } else {
            exemptionHelper.updateRecursiveExemption(id, exemptionBody, handler);
        }
    }

    @Override
    public void delete(List<String> exemption_ids, Handler<Either<String, JsonArray>> handler) {
        delete(DATABASE_TABLE, exemption_ids, handler);
    }

    @Override
    public void deleteRecursive(List<String> recursive_exemption_ids, Handler<Either<String, JsonArray>> handler) {
        delete(DATABASE_TABLE_RECURSIVE, recursive_exemption_ids, handler);
    }

    private void delete(String databaseTable, List<String> recursive_exemption_ids, Handler<Either<String, JsonArray>> handler) {
        if (recursive_exemption_ids.isEmpty()) {
            handler.handle(new Either.Left<>("Exemption.delete fail : No exemption id to delete"));
            return;
        }
        String query = "DELETE" +
                " FROM " + Presences.dbSchema + "." + databaseTable +
                " WHERE ";
        query += "id IN " + Sql.listPrepared(recursive_exemption_ids.toArray());
        JsonArray values = new JsonArray().addAll(new JsonArray(recursive_exemption_ids));

        query += " RETURNING * ;";
        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));
    }
}
