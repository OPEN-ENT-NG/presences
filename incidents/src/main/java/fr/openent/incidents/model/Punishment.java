package fr.openent.incidents.model;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.enums.WorkflowActions;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.Model;
import fr.openent.presences.model.Validator;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.*;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

public class Punishment extends Model {

    private String id;
    private String description;
    private boolean processed;
    private Long incident_id;
    private Long type_id;
    private String owner_id;
    private String structure_id;
    private String student_id;
    private JsonObject fields;
    private JsonObject type;
    private String grouped_punishment_id;
    private String created_at;
    private String updated_at;


    public Punishment() {
        table = "presences.punishments";

        fillables.put("id", Arrays.asList(Field.UPDATE, Field.MANDATORY));
        fillables.put("description", Arrays.asList(Field.CREATE, Field.UPDATE));
        fillables.put("processed", Collections.singletonList(Field.UPDATE));
        fillables.put("incident_id", Collections.singletonList(Field.CREATE));
        fillables.put("type_id", Arrays.asList(Field.CREATE, Field.UPDATE, Field.MANDATORY));
        fillables.put("owner_id", Arrays.asList(Field.CREATE, Field.UPDATE, Field.MANDATORY));
        fillables.put("structure_id", Arrays.asList(Field.CREATE, Field.MANDATORY));
        fillables.put("student_id", Arrays.asList(Field.CREATE, Field.MANDATORY));
        fillables.put("fields", Arrays.asList(Field.CREATE, Field.UPDATE));
        fillables.put("type", Collections.emptyList());
        fillables.put("grouped_punishment_id", Arrays.asList(Field.CREATE, Field.UPDATE));
        fillables.put("created_at", Collections.emptyList());
        fillables.put("updated_at", Collections.emptyList());
    }

    public String getId() {
        return id;
    }

    public Long getTypeId() {
        return type != null ? type.getLong("id") : type_id;
    }

    public String getStructureId() {
        return structure_id;
    }

    public JsonObject getFields() {
        return fields;
    }

    public String getStudentId() {
        return student_id;
    }

    public String getGroupedPunishmentId() {
        return grouped_punishment_id;
    }

    public String getOwnerId() {
        return owner_id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setStudentId(String student_id) {
        this.student_id = student_id;
    }

    public void setOwnerId(String owner_id) {
        this.owner_id = owner_id;
    }

    public void setFields(JsonObject fields) {
        this.fields = fields;
    }

    public void setTypeId(Long typeId) {
        this.type_id = typeId;
    }

    public void setStructureId(String structureId) {
        this.structure_id = structureId;
    }

    public void setGroupedPunishmentId(String groupedPunishmentId) {
        this.grouped_punishment_id = groupedPunishmentId;
    }

    public void setGroupedPunishmentId() {
        setGroupedPunishmentId(null);
    }


    public JsonObject toJsonObject(String method) {
        JsonObject result = new JsonObject();
        fillables.forEach((key, value) -> {
            if (value.contains(method)) {
                result.put(key, get(key));
            }
        });
        return result;
    }


    public Future<JsonObject> persistMongo(UserInfos user) {
        Promise<JsonObject> promise = Promise.promise();
        persistMongo(user, promise);
        return promise.future();
    }

    public void persistMongo(UserInfos user, Handler<AsyncResult<JsonObject>> handler) {
        checkUserRight(user, resRight -> {
            if (resRight.succeeded()) {
                if (getId() != null) {
                    if (getOwnerId() == null) this.setOwnerId(user.getUserId());
                    updateMongo(getId(), handler);
                } else {
                    this.setOwnerId(user.getUserId());
                    createMongo(handler);
                }
            } else {
                handler.handle(Future.failedFuture(resRight.cause().getMessage()));
            }
        });
    }




    private void updateMongo(String id, Handler<AsyncResult<JsonObject>> handler) {
        SimpleDateFormat formatter = new SimpleDateFormat(DateHelper.MONGO_FORMAT);
        String currentDate = formatter.format(new Date());
        String method = "UPDATE";
        JsonObject data = toJsonObject(method);
        data.put("updated_at", currentDate);
        data.remove("id");

        if (!Validator.validate(this, method)) {
            handler.handle(Future.failedFuture("[Incidents@Punishment::updateMongo] Validation failed."));
            return;
        }
        JsonObject criteria = new JsonObject()
                .put("_id", id);
        JsonObject returnFields = new JsonObject().put("_id", id);

        JsonObject set = new JsonObject().put("$set", data);

        MongoDb.getInstance().findAndModify(table, criteria, set, new JsonObject(), returnFields, false, true, false, event -> {
            if (event.body().isEmpty() || event.body().getString("status").equals("error")) {
                handler.handle(Future.failedFuture("[Incidents@Punishment::updateMongo] Failed to persist in mongoDb."));
            } else {
                handler.handle(Future.succeededFuture(event.body().getJsonObject("result")));
            }
        });
    }

    private void createMongo(Handler<AsyncResult<JsonObject>> handler) {

        SimpleDateFormat formatter = new SimpleDateFormat(DateHelper.MONGO_FORMAT);
        String currentDate = formatter.format(new Date());
        JsonObject data = toJsonObject();
        data.put("created_at", currentDate);
        data.remove("id");

        if (!Validator.validate(this, "CREATE")) {
            handler.handle(Future.failedFuture("[Common@Model::create] Validation failed."));
            return;
        }

        MongoDb.getInstance().save(table, data, event -> {
            if (event.body().isEmpty() || event.body().getString("status").equals("error")) {
                handler.handle(Future.failedFuture("[Incidents@Punishment::persistMongo] Failed to persist in mongoDb."));
            } else {
                handler.handle(Future.succeededFuture(event.body()));
            }
        });
    }

    private void checkUserRight(UserInfos user, Handler<AsyncResult<JsonObject>> handler) {
        JsonArray params = new JsonArray();

        String query = "SELECT type FROM " + Incidents.dbSchema + ".punishment_type where id = ?" +
                " AND structure_id = ?";

        params.add(getTypeId())
                .add(getStructureId());
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Incidents@Punishment::checkUserRight] Failed to get type";
                handler.handle(Future.failedFuture(message + " " + result.left().getValue()));
                return;
            }

            String resType = result.right().getValue().getString(Field.TYPE);

            if (resType.equals("PUNITION") && !WorkflowHelper.hasRight(user, WorkflowActions.PUNISHMENT_CREATE.toString())) {
                handler.handle(Future.failedFuture("[Incidents@Punishment::checkUserRight] This user have not the right to create Punishment."));
                return;
            } else if (resType.equals("SANCTION") && !WorkflowHelper.hasRight(user, WorkflowActions.SANCTION_CREATE.toString())) {
                handler.handle(Future.failedFuture("[Incidents@Punishment::checkUserRight] This user have not the right to create Sanction."));
                return;
            }
            handler.handle(Future.succeededFuture());
        }));
    }
}