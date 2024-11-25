package fr.openent.presences.model;

import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.core.constants.Field;
import io.vertx.core.json.JsonObject;


public class Alert implements IModel<Alert> {
    String student_id;
    String type;
    Integer count;
    String name;
    String lastName;
    String firstName;
    String audience;

    public Alert(JsonObject jsonObject) {
        this.student_id = jsonObject.getString(Field.STUDENT_ID);
        this.type = jsonObject.getString(Field.TYPE);
        this.count = jsonObject.getInteger(Field.COUNT);
        this.name = jsonObject.getString(Field.NAME);
        this.lastName = jsonObject.getString(Field.LASTNAME);
        this.firstName = jsonObject.getString(Field.FIRSTNAME);
        this.audience = jsonObject.getString(Field.AUDIENCE);
    }


    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(true, this);
    }

    @Override
    public boolean validate() {
        return this.student_id != null && !this.student_id.isEmpty() &&
                this.type != null && !this.type.isEmpty() &&
                this.count != null &&
                this.name != null && !this.name.isEmpty() &&
                this.lastName != null && !this.lastName.isEmpty() &&
                this.firstName != null && !this.firstName.isEmpty() &&
                this.audience != null && !this.audience.isEmpty();
    }
}
