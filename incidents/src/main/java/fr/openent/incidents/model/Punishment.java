package fr.openent.incidents.model;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.enums.WorkflowActions;
import fr.openent.incidents.model.punishmentCategory.PunishmentCategory;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.model.Model;
import fr.openent.presences.model.Validator;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

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
    private String created_at;
    private String updated_at;


    public Punishment() {
        table = "presences.punishments";

        fillables.put("id", Arrays.asList("UPDATE", "mandatory"));
        fillables.put("description", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("processed", Collections.singletonList("UPDATE"));
        fillables.put("incident_id", Collections.singletonList("CREATE"));
        fillables.put("type_id", Arrays.asList("CREATE", "UPDATE", "mandatory"));
        fillables.put("owner_id", Arrays.asList("CREATE", "mandatory"));
        fillables.put("structure_id", Arrays.asList("CREATE", "mandatory"));
        fillables.put("student_id", Arrays.asList("CREATE", "mandatory"));
        fillables.put("fields", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("created_at", Collections.emptyList());
        fillables.put("updated_at", Collections.emptyList());
    }

    public String getId() {
        return id;
    }

    public Long getTypeId() {
        return type_id;
    }

    public String getStructureId() {
        return structure_id;
    }

    public JsonObject getFields() {
        return fields;
    }

    public void setStudentId(String student_id) {
        this.student_id = student_id;
    }

    public void setOwnerId(String owner_id) {
        this.owner_id = owner_id;
    }

    public void setFields(JsonObject fields) {this.fields = fields;}


    public JsonObject toJsonObject(String method) {
        JsonObject result = new JsonObject();
        fillables.forEach((key, value) -> {
            if (value.contains(method)) {
                result.put(key, get(key));
            }
        });
        return result;
    }

    public void persistMongo(UserInfos user, Handler<AsyncResult<JsonObject>> handler) {
        PunishmentCategory.getSpecifiedCategoryFromType(getTypeId(), getStructureId(), getFields(), result -> {
                    if (result.failed()) {
                        handler.handle(Future.failedFuture(result.cause().getMessage()));
                    }
                    PunishmentCategory category = result.result();
                    setFields(category.toJsonObject());

                    if (getId() != null) {
                        updateMongo(user, getId(), handler);
                    } else {
                        setOwnerId(user.getUserId());
                        createMongo(user, handler);
                    }
                });
    }

    private void updateMongo(UserInfos user, String id, Handler<AsyncResult<JsonObject>> handler) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String currentDate = formatter.format(new Date());
        String method = "UPDATE";
        JsonObject data = toJsonObject(method);
        data.put("updated_at", currentDate);
        data.remove("id");

        if (!Validator.validate(this, method)) {
            handler.handle(Future.failedFuture("[Common@Model::create] Validation failed."));
            return;
        }

        JsonObject criteria = new JsonObject()
                .put("_id", id)
                .put("owner_id", this.owner_id.isEmpty() ? user.getUserId() : this.owner_id);
        JsonObject returnFields = new JsonObject().put("_id", id);

        JsonObject set = new JsonObject().put("$set", data);

        MongoDb.getInstance().findAndModify(table, criteria, set, new JsonObject(), returnFields, false, true, false, event -> {
            if (event.body().isEmpty() || event.body().getString("status").equals("error")) {
                handler.handle(Future.failedFuture("[Incidents@Punishment::persistMongo] Failed to persist in mongoDb."));
            } else {
                handler.handle(Future.succeededFuture(event.body().getJsonObject("result")));
            }
        });
    }

    private void createMongo(UserInfos user, Handler<AsyncResult<JsonObject>> handler) {
        String query = "SELECT type FROM " + Incidents.dbSchema + ".punishment_type where id = " + getTypeId() +
                " AND structure_id = '" + getStructureId() + "'";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Incidents@Punishment::createMongo] Failed to get type";
                handler.handle(Future.failedFuture(message + " " + result.left().getValue()));
                return;
            }
            String type = result.right().getValue().getString("type");

            if (type.equals("PUNITION") && !WorkflowHelper.hasRight(user, WorkflowActions.PUNISHMENT_CREATE.toString())) {
                handler.handle(Future.failedFuture("[Incidents@Punishment::createMongo] This user have not the right to create Punishment."));
                return;
            } else if(type.equals("SANCTION") && !WorkflowHelper.hasRight(user, WorkflowActions.SANCTION_CREATE.toString())) {
                handler.handle(Future.failedFuture("[Incidents@Punishment::createMongo] This user have not the right to create Sanction."));
                return;
            }

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
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

        }));
    }
}
