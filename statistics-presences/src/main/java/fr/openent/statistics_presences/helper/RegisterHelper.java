package fr.openent.statistics_presences.helper;

import fr.openent.statistics_presences.bean.Register;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

public class RegisterHelper {

    private RegisterHelper() {
        throw new IllegalStateException("Utility class");
    }

    @SuppressWarnings("unchecked")
    public static List<Register> getRegistersFromArray(JsonArray registers) {
        return ((List<JsonObject>) registers.getList()).stream()
                .map(Register::new)
                .collect(Collectors.toList());
    }
}

