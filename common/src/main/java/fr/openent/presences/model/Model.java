package fr.openent.presences.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.openent.presences.common.helper.CSVExport;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class Model {
    public static final Logger log = LoggerFactory.getLogger(Model.class);
    ObjectMapper mapper = new ObjectMapper();
    protected Map<String, List<String>> fillables = new HashMap<>();
    protected String primaryKey = "id";
    protected String table = null;

    public String getTable() {
        return table;
    }

    public Map<String, List<String>> getFillables() {
        return fillables;
    }

    public Model setFromJson(JsonObject body) {
        body.forEach(item -> {
            try {
                if (fillables.containsKey(item.getKey())) {
                    set(item.getKey(), item.getValue());
                }
            } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
                e.printStackTrace();
            }
        });
        return this;
    }

    public void set(String key, Object value) throws NoSuchFieldException, IllegalAccessException, IOException {
        Field f = getClass().getDeclaredField(key);
        f.setAccessible(true);
        f.set(this, mapper(value, f.getType()));
    }

    public Object get(String key) {
        try {
            Field f = getClass().getDeclaredField(key);
            f.setAccessible(true);
            return f.get(this);
        } catch (NoSuchFieldException | IllegalAccessException | SecurityException e) {
            log.error("[Common@Model::get] Error while getting attribute " + key + ".");
            e.printStackTrace();
        }
        return null;
    }

    public JsonObject toJsonObject() {
        JsonObject result = new JsonObject();
        fillables.forEach((key, value) -> {
            result.put(key, get(key));
        });
        return result;
    }

    public void create(Handler<AsyncResult<JsonObject>> handler) {
        if(!Validator.validate(this, "CREATE")) {
            handler.handle(Future.failedFuture("[Common@Model::create] Validation failed."));
            return;
        }

        Repository.create(this, result -> {
            if (result.failed()) {
                handler.handle(Future.failedFuture("[Common@Model::create] Failed to create."));
                return;
            }
            setFromJson(result.result());
            handler.handle(Future.succeededFuture(toJsonObject()));
        });
    }

    public void update(Handler<AsyncResult<JsonObject>> handler) {
        if(!Validator.validate(this, "UPDATE")) {
            handler.handle(Future.failedFuture("[Common@Model::update] Validation failed."));
            return;
        }

        Repository.update(this, result -> {
            if (result.failed()) {
                handler.handle(Future.failedFuture("[Common@Model::create] Failed to update."));
                return;
            }
            setFromJson(result.result());
            handler.handle(Future.succeededFuture(toJsonObject()));
        });
    }


    public void getStatement(String method, Handler<AsyncResult<JsonObject>> handler) {
        Repository.getStatement(this, method, handler);
    }

    public void persistStatements(String method, Handler<AsyncResult<JsonObject>> handler) {
        Repository.getStatement(this, method, handler);
    }

    public Object mapper (Object value, Class<?> type) throws IOException {
        if (JsonObject.class.equals(type)) {
            return value;
        }
        return mapper.readValue(mapper.writeValueAsString(value), type);
    }

}
