package fr.openent.presences.helper;

import fr.openent.presences.model.Event.RegisterEvent;
import fr.openent.presences.model.Register;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class RegisterHelper {

    /**
     * Convert JsonArray into register list
     *
     * @param array               JsonArray response
     * @param mandatoryAttributes List of mandatory attributes
     * @return new list of events
     */
    public static List<Register> getRegisterListFromJsonArray(JsonArray array, List<String> mandatoryAttributes) {
        List<Register> registerList = new ArrayList<>();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            Register register = new Register((JsonObject) o, mandatoryAttributes);
            registerList.add(register);
        }
        return registerList;
    }

    /**
     * Convert JsonArray into register events list
     *
     * @param array JsonArray response
     * @return new list of events
     */
    public static List<RegisterEvent> getRegisterEventListFromJsonArray(JsonArray array) {
        List<RegisterEvent> registerEventList = new ArrayList<>();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            RegisterEvent registerEvent = new RegisterEvent((JsonObject) o);
            registerEventList.add(registerEvent);
        }
        return registerEventList;
    }
}
