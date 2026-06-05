package fr.openent.statistics_presences.helper;

import fr.openent.statistics_presences.bean.Register;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class RegisterHelper {

    private static final Logger log = LoggerFactory.getLogger(RegisterHelper.class);

    private RegisterHelper() {
        throw new IllegalStateException("Utility class");
    }

    @SuppressWarnings("unchecked")
    public static List<Register> getRegistersFromArray(JsonArray registers) {
        return registers.stream()
                .filter(Objects::nonNull)
                .map(elt -> {
                    if(elt instanceof JsonObject) {
                        return new Register((JsonObject) elt);
                    } else if(elt instanceof Map) {
                        return new Register(new JsonObject((Map)elt));
                    } else {
                        log.error("Cannot use elt of type " + elt.getClass().getCanonicalName() + " to create a Register");
                        throw new IllegalArgumentException("Cannot use elt of type " + elt.getClass().getCanonicalName() + " to create a Register");
                    }
                })
                .collect(Collectors.toList());
    }
}

