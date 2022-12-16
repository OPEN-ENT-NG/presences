package fr.openent.statistics_presences.bean;

import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.IModel;
import io.vertx.core.json.JsonObject;

public class Failure implements IModel<Failure> {
    private String user;
    private String structure;
    private String error;

    public Failure(JsonObject jsonObject) {
        this.user = jsonObject.getString(Field.USER);
        this.structure = jsonObject.getString(Field.STRUCTURE);
        this.error = jsonObject.getString(Field.ERROR);
    }
    public Failure(String user, String structure, Throwable err) {
        this.user = user;
        this.structure = structure;
        this.error = err.getMessage();
    }

    public String toString() {
        return String.format("user: %s structure: %s error: %s", user, structure, error);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(false, this);
    }

    @Override
    public boolean validate() {
        return false;
    }

    public String getUser() {
        return user;
    }

    public Failure setUser(String user) {
        this.user = user;
        return this;
    }

    public String getStructure() {
        return structure;
    }

    public Failure setStructure(String structure) {
        this.structure = structure;
        return this;
    }

    public String getError() {
        return error;
    }

    public Failure setError(String error) {
        this.error = error;
        return this;
    }
}
