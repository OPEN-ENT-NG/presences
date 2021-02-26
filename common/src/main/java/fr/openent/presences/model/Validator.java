package fr.openent.presences.model;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Validator {
    private static final Logger log = LoggerFactory.getLogger(Validator.class);

    public static Boolean validate(JsonObject body, Map<String, List<String>> validator, String method) {
        return mandatoryCheck(body, validator, method);
    }

    public static Boolean mandatoryCheck(JsonObject body, Map<String, List<String>> validator, String method) {
        Map<String, List<String>> failures = validator.entrySet()
                .stream()
                .filter(item -> item.getValue().contains(method)
                        && item.getValue().contains("mandatory")
                        && (!body.containsKey(item.getKey())) || body.getValue(item.getKey()) == null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return failures.size() <= 0;
    }

    public static Boolean validate(Model model, String method) {
        return mandatoryCheck(model, method);
    }

    public static Boolean mandatoryCheck(Model model, String method) {
        Map<String, List<String>> failures = model.fillables.entrySet()
                .stream()
                .filter(item -> {
                    try {
                        Field f = model.getClass().getDeclaredField(item.getKey());
                        f.setAccessible(true);
                        return item.getValue().contains(method)
                                && item.getValue().contains("mandatory")
                                && (f.get(model) == null || f.get(model) == "");
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return true;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        boolean failed = failures.size() > 0;
        if(failed) {
            log.error("Validation mandatory failed for the following fields: ",
                    failures.entrySet().stream().map(failure -> failure.getKey()).collect(Collectors.toList()).toString());
        }
        return !failed;
    }
}
