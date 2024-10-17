package fr.openent.presences.helper;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.ModelHelper;
import fr.openent.presences.model.Exemption.Exemption;
import fr.openent.presences.model.Exemption.ExemptionBody;
import fr.openent.presences.model.Exemption.ExemptionView;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ExemptionHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExemptionHelper.class);
    private EventBus eb;
    private String DATABASE_TABLE;
    private String DATABASE_TABLE_RECURSIVE;

    public ExemptionHelper(EventBus eb, String DATABASE_TABLE, String DATABASE_TABLE_RECURSIVE) {
        this.eb = eb;
        this.DATABASE_TABLE = DATABASE_TABLE;
        this.DATABASE_TABLE_RECURSIVE = DATABASE_TABLE_RECURSIVE;
    }

    /**
     * Convert JsonArray into ExemptionView list
     *
     * @param array JsonArray response
     * @return new list of events
     */
    public static List<ExemptionView> getExemptionListFromJsonArray(JsonArray array) {
        List<ExemptionView> eventList = new ArrayList<>();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            ExemptionView event = new ExemptionView((JsonObject) o);
            eventList.add(event);
        }
        return eventList;
    }

    public String getSqlOrderValue(String field) {
        String typeField;
        switch (field) {
            case "date":
                typeField = "exemption_view.start_date";
                break;
            case "attendance":
                typeField = "exemption_view.attendance";
                break;
            default:
                typeField = "exemption_view.start_date";
        }
        return typeField;
    }

    public String getSqlReverseString(Boolean reverse) {
        return reverse ? "ASC" : "DESC";
    }

    /**
     * add User object info (student) to exemption
     *
     * @param structure_id structure identifier
     * @param exemptions   list of exemption
     */
    public void addUsersInfo(String structure_id, List<ExemptionView> exemptions, Future<JsonObject> future) {
        JsonArray student_ids = new JsonArray();

        for (ExemptionView exemption : exemptions) {
            student_ids.add(exemption.getStudentId());
        }

        JsonObject action = new JsonObject()
                .put("action", "eleve.getInfoEleve")
                .put("idEleves", student_ids)
                .put("idEtablissement", structure_id);

        eb.send(Presences.ebViescoAddress, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if ("ok".equals(body.getString("status"))) {
                JsonArray student_ids_fromClasses = body.getJsonArray("results");
                for (ExemptionView exemption : exemptions) {
                    int j = student_ids_fromClasses.size();
                    boolean found = false;
                    while (j > 0 && !found) {
                        j--;
                        JsonObject studentInfo = student_ids_fromClasses.getJsonObject(j);
                        if (exemption.getStudentId().equals(studentInfo.getString("idEleve"))) {
                            found = true;
                            exemption.setStudent(studentInfo);
                        }
                    }
                }
                future.complete();
            } else {
                String error = "[Presences@ExemptionHelper] Exemption: Can't get student info";
                LOGGER.error(error);
                future.fail(error);
            }
        }));
    }

    /**
     * add Subject object info (subject) to exemption
     *
     * @param exemptions list of exemption
     * @param future     future used for a composite future
     */
    public void addSubjectsInfo(List<ExemptionView> exemptions, Future<JsonObject> future) {
        JsonArray subject_ids = new JsonArray();

        for (ExemptionView exemption : exemptions) {
            if (exemption.getSubjectId() != null && !exemption.getSubjectId().isEmpty()) {
                subject_ids.add(exemption.getSubjectId());
            }
        }

        JsonObject action = new JsonObject()
                .put("action", "matiere.getMatieres")
                .put("idMatieres", subject_ids);

        eb.send(Presences.ebViescoAddress, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if ("ok".equals(body.getString("status"))) {
                JsonArray matieresInfo = body.getJsonArray("results");
                for (ExemptionView exemption : exemptions) {
                    int j = matieresInfo.size();
                    boolean found = false;
                    while (j > 0 && !found) {
                        j--;
                        JsonObject subjectInfo = matieresInfo.getJsonObject(j);
                        if (exemption.getSubjectId() != null && !exemption.getSubjectId().isEmpty()) {
                            if (exemption.getSubjectId().equals(subjectInfo.getString("id"))) {
                                found = true;
                                exemption.setSubject(subjectInfo);
                            }
                        }
                    }
                }
                future.complete();
            } else {
                String error = "[Presences@ExemptionHelper] Exemption: Can't get subject info";
                LOGGER.error(error);
                future.fail(error);
            }
        }));
    }

    /**
     * create punctual exemption
     *
     * @param exemptionBody exemption body fetched from client side
     * @param handler       function handler returning data
     */
    public void createPunctualExemption(ExemptionBody exemptionBody, Handler<Either<String, JsonArray>> handler) {
        if (exemptionBody.getListStudentId().isEmpty()) {
            handler.handle(new Either.Left<>("Exemption.create fail : No student ids"));
            return;
        }
        StringBuilder query = new StringBuilder("INSERT INTO " + Presences.dbSchema + "." + DATABASE_TABLE +
                " (structure_id, student_id, subject_id, start_date, end_date, attendance, comment) " +
                " VALUES ");
        JsonArray values = new JsonArray();

        for (String student_id : exemptionBody.getListStudentId()) {
            query.append("( ?, ?, ?, ?, ?, ?, ?),");
            values.add(exemptionBody.getStructureId())
                    .add(student_id)
                    .add(exemptionBody.getSubjectId())
                    .add(exemptionBody.getStartDate())
                    .add(exemptionBody.getEndDate())
                    .add(exemptionBody.getAttendance())
                    .add(exemptionBody.getComment());
        }
        query = new StringBuilder(query.substring(0, query.length() - 1) + " RETURNING * ;");

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    /**
     * create recursive exemption
     *
     * @param exemptionBody exemption body fetched from client side
     * @param handler       function handler returning data
     */
    public void createRecursiveExemption(ExemptionBody exemptionBody, Handler<Either<String, JsonArray>> handler) {
        if (exemptionBody.getListStudentId().isEmpty() && exemptionBody.getDayOfWeeks().isEmpty()) {
            handler.handle(new Either.Left<>("Exemption.create fail : No student ids"));
            return;
        }
        StringBuilder query = new StringBuilder("INSERT INTO " + Presences.dbSchema + "." + DATABASE_TABLE_RECURSIVE +
                " (structure_id, student_id , start_date, end_date, day_of_week, comment, is_every_two_weeks, attendance) " +
                " VALUES ");
        JsonArray values = new JsonArray();
        for (String student_id : exemptionBody.getListStudentId()) {
            query.append("( ?, ?, ?, ?, ")
                    .append(Sql.arrayPrepared(exemptionBody.getDayOfWeeks()))
                    .append(", ?, ?, ?),");
            values.add(exemptionBody.getStructureId())
                    .add(student_id)
                    .add(exemptionBody.getStartDateRecursive())
                    .add(exemptionBody.getEndDateRecursive())
                    .addAll(new JsonArray(exemptionBody.getDayOfWeeks()))
                    .add(exemptionBody.getComment())
                    .add(exemptionBody.isEveryTwoWeeks())
                    .add(exemptionBody.getAttendance());
        }
        query = new StringBuilder(query.substring(0, query.length() - 1) + " RETURNING * ;");
        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(asyncResult -> {
            if (asyncResult.isLeft()) {
                String error = "[Presences@ExemptionHelper] Exemption: Failed to create recursive exemption ";
                LOGGER.error(error);
                handler.handle(new Either.Left<>(error + " " + asyncResult.left().getValue()));
            } else {
                List<ExemptionView> exemptions = ExemptionHelper.getExemptionListFromJsonArray(asyncResult.right().getValue());
                JsonArray statements = new JsonArray();
                for (ExemptionView exemptionView : exemptions) {
                    if (!exemptionView.getDayOfWeek().isEmpty()) {
                        for (int i = 0; i < exemptionView.getDayOfWeek().size(); i++) {
                            statementForEachDay(statements, exemptionView, exemptionView.getDayOfWeek().getString(i), null);
                        }
                    }
                }
                Sql.getInstance().transaction(statements, async -> {
                    Either<String, JsonObject> result = SqlResult.validUniqueResult(0, async);
                    if (result.isLeft()) {
                        String message = "[Presences@ExemptionHelper] Failed to execute exemption recursive creation statements";
                        LOGGER.error(message);
                        handler.handle(new Either.Left<>(message));
                    } else {
                        handler.handle(new Either.Right<>(ModelHelper.convertToJsonArray(exemptions)));
                    }
                });
            }
        }));
    }

    /**
     * method that will add to a statement {JsonArray} each potential exemption based on looped day of week
     *
     * @param statements             list of statement json array for psql
     * @param exemption              exemptionView from returned PSQL creation exemption's data or exemptionBody while
     *                               updating
     * @param exemption_recursive_id exemption recursive identifier fetched from exemptionBody if we UPDATE
     */
    private void statementForEachDay(JsonArray statements, Exemption exemption, String day, Integer exemption_recursive_id) {
        List<LocalDate> daysFetched = DateHelper.getListOfDateBasedOnDates(
                exemption instanceof ExemptionView ? exemption.getStartDate() : ((ExemptionBody) exemption).getStartDateRecursive(),
                exemption instanceof ExemptionView ? exemption.getEndDate() : ((ExemptionBody) exemption).getEndDateRecursive(),
                DateHelper.YEAR_MONTH_DAY,
                day,
                exemption.isEveryTwoWeeks()
        );
        for (LocalDate dayFetched : daysFetched) {
            String start_date = dayFetched.toString() + " " + getStartTimeForEachStatement(exemption);
            String end_date = dayFetched.toString() + " " + getEndTimeForEachStatement(exemption);
            statements.add(addExemptionStatementFromRecursive(exemption, start_date, end_date, exemption_recursive_id));

        }
    }

    /**
     * method that will specify which exemption (VIEW | BODY)
     * we will use for our method to define our start time
     *
     * @param exemption exemptionView from returned PSQL creation exemption's data or exemptionBody while updating
     */
    private String getStartTimeForEachStatement(Exemption exemption) {
        String startTime = "";
        try {
            if (exemption instanceof ExemptionView) {
                startTime = DateHelper.getTimeString(exemption.getStartDate(), DateHelper.SQL_FORMAT);
            } else if (exemption instanceof ExemptionBody) {
                startTime = DateHelper.getTimeString(((ExemptionBody) exemption).getStartDateRecursive(),
                        DateHelper.YEAR_MONTH_DAY + " " + DateHelper.HOUR_MINUTES);
            }
        } catch (ParseException e) {
            LOGGER.error("[Presences@ExemptionHelper::getStartTimeForEachStatement] Failed to parse date", e);
        }
        return startTime;
    }

    /**
     * method that will specify which exemption (VIEW | BODY)
     * we will use for our method to define our end time
     *
     * @param exemption exemptionView from returned PSQL creation exemption's data or exemptionBody while updating
     */
    private String getEndTimeForEachStatement(Exemption exemption) {
        String endTime = "";
        try {
            if (exemption instanceof ExemptionView) {
                endTime = DateHelper.getTimeString(exemption.getEndDate(), DateHelper.SQL_FORMAT);
            } else if (exemption instanceof ExemptionBody) {
                endTime = DateHelper.getTimeString(((ExemptionBody) exemption).getEndDateRecursive(),
                        DateHelper.YEAR_MONTH_DAY + " " + DateHelper.HOUR_MINUTES);
            }
        } catch (ParseException e) {
            LOGGER.error("[Presences@ExemptionHelper::getEndTimeForEachStatement] Failed to parse date", e);
        }
        return endTime;
    }

    /**
     * statement that create exemption from its recursive
     *
     * @param exemption eachExemptionStatement identifier
     * @return Statement
     */
    private JsonObject addExemptionStatementFromRecursive(Exemption exemption, String start_date, String end_date,
                                                          Integer exemption_recursive_id) {
        String query = "INSERT INTO " + Presences.dbSchema + "." + DATABASE_TABLE +
                " (structure_id, student_id, subject_id, start_date, end_date, attendance, comment, recursive_id)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        JsonArray values = new JsonArray().add(exemption.getStructureId());
        if (exemption instanceof ExemptionView) {
            values.add(((ExemptionView) exemption).getStudentId());
        } else if (exemption instanceof ExemptionBody) {
            values.add(((ExemptionBody) exemption).getListStudentId().get(0));
        }
        values.add("")
                .add(start_date)
                .add(end_date)
                .add(exemption.getAttendance())
                .add(exemption.getComment());
        if (exemption instanceof ExemptionView) {
            values.add(((ExemptionView) exemption).getExemptionRecursiveId());
        } else if (exemption instanceof ExemptionBody) {
            values.add(exemption_recursive_id);
        }

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    /**
     * statement that update exemption recursive
     *
     * @param exemption_id  exemption identifier
     * @param exemptionBody exemption body fetched from client side
     * @param handler       function handler returning data
     */
    public void updatePunctualExemption(Integer exemption_id, ExemptionBody exemptionBody,
                                        Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Presences.dbSchema + "." + DATABASE_TABLE +
                " SET structure_id = ?, student_id = ?, subject_id = ?, start_date = ?, end_date = ?, attendance = ?, comment = ? " +
                " WHERE id = ?";
        JsonArray values = new JsonArray()
                .add(exemptionBody.getStructureId())
                .add(exemptionBody.getListStudentId().get(0))
                .add(exemptionBody.getSubjectId())
                .add(exemptionBody.getStartDate())
                .add(exemptionBody.getEndDate())
                .add(exemptionBody.getAttendance())
                .add(exemptionBody.getComment())
                .add(exemption_id);

        query += " RETURNING * ;";
        Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(handler));
    }

    /**
     * statement that update exemption recursive
     *
     * @param exemption_recursive_id exemption identifier
     * @param exemptionBody          exemption body fetched from client side
     * @param handler                function handler returning data
     */
    public void updateRecursiveExemption(Integer exemption_recursive_id, ExemptionBody exemptionBody,
                                         Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray();

        statements.add(updateRecursiveExemptionStatement(exemption_recursive_id, exemptionBody));
        statements.add(eraseExemptionFromRecursive(exemption_recursive_id));

        if (!exemptionBody.getDayOfWeeks().isEmpty()) {
            for (String day : exemptionBody.getDayOfWeeks()) {
                statementForEachDay(statements, exemptionBody, day, exemption_recursive_id);
            }
        }

        Sql.getInstance().transaction(statements, updateAsync -> {
            Either<String, JsonObject> result = SqlResult.validUniqueResult(0, updateAsync);
            if (result.isLeft()) {
                String message = "[Presences@ExemptionHelper::updateRecursiveExemption] Failed to execute recursive exemption update statements";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(result.left().getValue() + " " + message));
            } else {
                handler.handle(new Either.Right<>(result.right().getValue()));
            }
        });
    }

    /**
     * statement that update exemption recursive
     *
     * @param exemption_recursive_id exemption identifier
     * @param exemptionBody          exemption body fetched from client side
     * @return {JsonObject} prepared PSQL request plus its value
     */
    private JsonObject updateRecursiveExemptionStatement(Integer exemption_recursive_id, ExemptionBody exemptionBody) {
        String query = "UPDATE " + Presences.dbSchema + "." + DATABASE_TABLE_RECURSIVE +
                " SET structure_id = ?, student_id = ?, start_date = ?, end_date = ?, attendance = ?, comment = ?, " +
                " is_every_two_weeks = ?, day_of_week = " + Sql.arrayPrepared(exemptionBody.getDayOfWeeks()) +
                " WHERE id = ?";
        JsonArray values = new JsonArray()
                .add(exemptionBody.getStructureId())
                .add(exemptionBody.getListStudentId().get(0))
                .add(exemptionBody.getStartDateRecursive())
                .add(exemptionBody.getEndDateRecursive())
                .add(exemptionBody.getAttendance())
                .add(exemptionBody.getComment())
                .add(exemptionBody.isEveryTwoWeeks())
                .addAll(new JsonArray(exemptionBody.getDayOfWeeks()))
                .add(exemption_recursive_id);

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }

    /**
     * statement that delete exemption recursive
     *
     * @param exemption_recursive_id exemption identifier
     * @return {JsonObject} prepared PSQL request plus its value
     */
    private JsonObject eraseExemptionFromRecursive(Integer exemption_recursive_id) {
        String query = "DELETE FROM " + Presences.dbSchema + "." + DATABASE_TABLE +
                " WHERE recursive_id = ?";
        JsonArray values = new JsonArray().add(exemption_recursive_id);

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }
}
