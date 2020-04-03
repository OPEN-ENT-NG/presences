package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.ModelHelper;
import fr.openent.presences.helper.ExemptionHelper;
import fr.openent.presences.model.Exemption.ExemptionBody;
import fr.openent.presences.model.Exemption.ExemptionView;
import fr.openent.presences.service.ExemptionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.Arrays;
import java.util.List;

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
    public void get(String structure_id, String start_date, String end_date, List<String> student_ids, String page,
                    String order, boolean reverse, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * " + this.getterFROMBuilder(DATABASE_TABLE_VIEW, structure_id, start_date, end_date, student_ids);
        JsonArray values = new JsonArray();

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
                if (exemptions.size() == 0) {
                    handler.handle(new Either.Right<>(ModelHelper.convertToJsonArray(exemptions)));
                } else {
                    Future<JsonObject> userInfoFuture = Future.future();
                    Future<JsonObject> subjectInfoFuture = Future.future();

                    exemptionHelper.addUsersInfo(structure_id, exemptions, userInfoFuture);
                    exemptionHelper.addSubjectsInfo(exemptions, subjectInfoFuture);

                    CompositeFuture.all(userInfoFuture, subjectInfoFuture).setHandler(eventFutured -> {
                        if (eventFutured.succeeded()) {
                            handler.handle(new Either.Right<>(ModelHelper.convertToJsonArray(exemptions)));
                        } else {
                            handler.handle(new Either.Left<>(eventFutured.cause().getMessage()));
                        }
                    });
                }
            }
        }));
    }

    @Override
    public void get(String structure_id, String start_date, String end_date, String userId, String page,
                    Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * " + this.getterFROMBuilder(DATABASE_TABLE, structure_id, start_date, end_date, Arrays.asList(userId));
        JsonArray values = new JsonArray();

        if (page != null) {
            query += " ORDER BY start_date";
            query += " OFFSET ? LIMIT ?";
            values.add(Presences.PAGE_SIZE * Integer.parseInt(page));
            values.add(Presences.PAGE_SIZE);
        }

        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getPageNumber(String structure_id, String start_date, String end_date, List<String> student_ids,
                              String order, boolean reverse, Handler<Either<String, JsonObject>> handler) {
        String query =
                "SELECT count(" + Presences.dbSchema + "." + DATABASE_TABLE_VIEW + ".exemption_id)" +
                        this.getterFROMBuilder(DATABASE_TABLE_VIEW, structure_id, start_date, end_date, student_ids);
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

    private String getterFROMBuilder(String databaseTable, String structure_id, String start_date, String end_date, List<String> student_ids) {
        StringBuilder query = new StringBuilder(" FROM " + Presences.dbSchema + "." + databaseTable +
                " WHERE structure_id = '" + structure_id + "'");
        if (databaseTable.equals(DATABASE_TABLE_VIEW)) {
            query.append(" AND recursive_id IS NULL");
        }
        query.append(" AND (" + " (start_date >= '")
                .append(start_date)
                .append("' AND start_date <= '")
                .append(end_date)
                .append("')")
                .append(" OR (end_date >= '")
                .append(start_date)
                .append("' AND end_date <= '")
                .append(end_date)
                .append("')")
                .append(" OR (start_date >= '")
                .append(start_date)
                .append("' AND end_date <= '")
                .append(end_date)
                .append("')")
                .append(")");

        if (student_ids != null && !student_ids.isEmpty()) {
            query.append(" AND student_id IN (");
            for (String student_id : student_ids) {
                query.append("'").append(student_id).append("',");
            }
            query = new StringBuilder(query.substring(0, query.length() - 1) + ")");

        }
        return query.toString();
    }

    @Override
    public void getRegisterExemptions(List<String> studentList, String structure_id, String start_date, String end_date,
                                      Handler<Either<String, JsonArray>> handler) {
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
