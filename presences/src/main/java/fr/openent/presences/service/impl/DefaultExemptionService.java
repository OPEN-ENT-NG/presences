package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.service.ExemptionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultExemptionService extends SqlCrudService implements ExemptionService {
    private final static String DATABASE_TABLE = "exemption";
    private EventBus eb;

    public DefaultExemptionService(EventBus eb) {

        super(Presences.dbSchema, DATABASE_TABLE);
        this.eb = eb;
    }

    @Override
    public void get(String structure_id, String start_date, String end_date, List<String> student_ids, String page, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * " + this.getterFROMBuilder(structure_id, start_date, end_date, student_ids);
        JsonArray values = new JsonArray();

        if (page != null) {
            query += " ORDER BY start_date";
            query += " OFFSET ? LIMIT ?";
            values.add(Presences.PAGE_SIZE * Integer.parseInt(page));
            values.add(Presences.PAGE_SIZE);
        }


        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isLeft()) {
                    handler.handle(new Either.Left<>(event.left().getValue()));
                } else {
                    JsonArray exemptions = event.right().getValue();
                    if (exemptions.size() == 0) {
                        handler.handle(new Either.Right<>(exemptions));
                    } else {
                        Future<JsonObject> userInfoFuture = Future.future();
                        Future<JsonObject> subjectInfoFuture = Future.future();

                        addUsersInfo(structure_id, exemptions, userInfoFuture);
                        addSubjectsInfo(structure_id, exemptions, subjectInfoFuture);


                        CompositeFuture.all(userInfoFuture, subjectInfoFuture).setHandler(eventFutured -> {
                            if (eventFutured.succeeded()) {
                                handler.handle(new Either.Right<>(exemptions));
                            } else {
                                handler.handle(new Either.Left<>(eventFutured.cause().getMessage()));
                            }
                        });
                    }
                }
            }
        }));
    }

    @Override
    public void getPageNumber(String structure_id, String start_date, String end_date, List<String> student_ids, Handler<Either<String, JsonObject>> handler) {
        String query =
                "SELECT count(" + Presences.dbSchema + "." + this.DATABASE_TABLE + ".id)" +
                        this.getterFROMBuilder(structure_id, start_date, end_date, student_ids);
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

    private String getterFROMBuilder(String structure_id, String start_date, String end_date, List<String> student_ids) {
        String query = " FROM " + Presences.dbSchema + "." + this.DATABASE_TABLE +
                " WHERE structure_id = '" + structure_id + "'" +
                " AND (" +
                " (start_date >= '" + start_date + "' AND start_date <= '" + end_date + "')" +
                " OR (end_date >= '" + start_date + "' AND end_date <= '" + end_date + "')" +
                " OR (start_date >= '" + start_date + "' AND end_date <= '" + end_date + "')" +
                ")";

        if (student_ids != null && !student_ids.isEmpty() && student_ids.size() > 0) {
            query += " AND student_id IN (";
            for (int i = 0; i < student_ids.size(); ++i) {
                query += "'" + student_ids.get(i) + "',";
            }
            query = query.substring(0, query.length() - 1) + ")";

        }
        return query;
    }

    ;

    @Override
    public void create(String structure_id, JsonArray student_ids, String subject_id, String start_date, String end_date, Boolean attendance, String comment, Handler<Either<String, JsonArray>> handler) {

        if (student_ids.isEmpty()) {
            handler.handle(new Either.Left<>("Exemption.create fail : No student ids"));
            return;
        }
        String query = "INSERT INTO " + Presences.dbSchema + "." + this.DATABASE_TABLE +
                " (structure_id, student_id, subject_id, start_date, end_date, attendance, comment) " +
                " VALUES ";
        JsonArray values = new JsonArray();

        for (Integer i = 0; i < student_ids.size(); i++) {
            String student_id = student_ids.getString(i);
            query += "( ?, ?, ?, ?, ?, ?, ?),";
            values.add(structure_id)
                    .add(student_id)
                    .add(subject_id)
                    .add(start_date)
                    .add(end_date)
                    .add(attendance)
                    .add(comment);
        }
        query = query.substring(0, query.length() - 1) + " RETURNING * ;";

        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void update(String exemption_id, String structure_id, String student_id, String subject_id, String start_date, String end_date, Boolean attendance, String comment, Handler<Either<String, JsonObject>> handler) {

        String query = "UPDATE " + Presences.dbSchema + "." + this.DATABASE_TABLE +
                " SET structure_id = ?, student_id = ?, subject_id = ?, start_date = ?, end_date = ?, attendance = ?, comment = ? " +
                " WHERE id = ?";
        JsonArray values = new JsonArray()
                .add(structure_id)
                .add(student_id)
                .add(subject_id)
                .add(start_date)
                .add(end_date)
                .add(attendance)
                .add(comment)
                .add(exemption_id);

        query += " RETURNING * ;";
        Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(List<String> exemption_ids, Handler<Either<String, JsonArray>> handler) {
        if (exemption_ids.isEmpty()) {
            handler.handle(new Either.Left<>("Exemption.delete fail : No exemption id to delete"));
            return;
        }
        String query = "DELETE" +
                " FROM " + Presences.dbSchema + "." + this.DATABASE_TABLE +
                " WHERE ";
        query += "id IN " + Sql.listPrepared(exemption_ids.toArray());
        JsonArray values = new JsonArray().addAll(new JsonArray(exemption_ids));

        query += " RETURNING * ;";
        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));
    }


    private void addUsersInfo(String structure_id, JsonArray exemptions, Future future) {
        JsonArray student_ids = new JsonArray();

        for (int i = 0; i < exemptions.size(); i++) {
            student_ids.add(exemptions.getJsonObject(i).getString("student_id"));
        }

        JsonObject action = new JsonObject()
                .put("action", "eleve.getInfoEleve")
                .put("idEleves", student_ids)
                .put("idEtablissement", structure_id);

        eb.send(Presences.ebViescoAddress, action, handlerToAsyncHandler((Handler<Message<JsonObject>>) message -> {
            JsonObject body = message.body();
            if ("ok".equals(body.getString("status"))) {
                JsonArray student_ids_fromClasses = body.getJsonArray("results");
                for (int i = 0; i < exemptions.size(); i++) {
                    JsonObject exemption_tmp = exemptions.getJsonObject(i);
                    int j = student_ids_fromClasses.size();
                    boolean found = false;
                    while (j > 0 && !found) {
                        j--;
                        JsonObject studentInfo = student_ids_fromClasses.getJsonObject(j);
                        if (exemption_tmp.getString("student_id").equals(studentInfo.getString("idEleve"))) {
                            found = true;
                            exemption_tmp.put("student", studentInfo);
                        }
                    }
                }

                future.complete();
            } else {
                future.fail("Exemption: Can't get student info");
            }
        }));
    }

    private void addSubjectsInfo(String structure_id, JsonArray exemptions, Future future) {
        JsonArray subject_ids = new JsonArray();

        for (int i = 0; i < exemptions.size(); i++) {
            subject_ids.add(exemptions.getJsonObject(i).getString("subject_id"));
        }

        JsonObject action = new JsonObject()
                .put("action", "matiere.getMatieres")
                .put("idMatieres", subject_ids);

        eb.send(Presences.ebViescoAddress, action, handlerToAsyncHandler((Handler<Message<JsonObject>>) message -> {
            JsonObject body = message.body();
            if ("ok".equals(body.getString("status"))) {
                JsonArray matieresInfo = body.getJsonArray("results");
                for (int i = 0; i < exemptions.size(); i++) {
                    JsonObject exemption_tmp = exemptions.getJsonObject(i);
                    int j = matieresInfo.size();
                    boolean found = false;
                    while (j > 0 && !found) {
                        j--;
                        JsonObject subjectInfo = matieresInfo.getJsonObject(j);
                        if (exemption_tmp.getString("subject_id").equals(subjectInfo.getString("id"))) {
                            found = true;
                            exemption_tmp.put("subject", subjectInfo);
                        }
                    }
                }
                future.complete();
            } else {
                future.fail("Exemption: Can't get subject info");
            }
        }));
    }
}
