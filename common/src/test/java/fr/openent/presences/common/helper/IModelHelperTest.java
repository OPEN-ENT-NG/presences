package fr.openent.presences.common.helper;

import fr.openent.presences.model.IModel;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.reflections.scanners.Scanners.SubTypes;

@RunWith(VertxUnitRunner.class)
public class IModelHelperTest {
    private static final Logger log = LoggerFactory.getLogger(IModelHelperTest.class);

    @Test
    public void testSubClassIModel(TestContext ctx) {
        Reflections reflections = new Reflections("fr.openent.presences");

        Set<Class<?>> subTypes =
                reflections.get(SubTypes.of(IModel.class).asClass());
        List<Class<?>> invalidModel = subTypes.stream().filter(modelClass -> {
            Constructor<?> emptyConstructor = Arrays.stream(modelClass.getConstructors())
                    .filter(constructor -> constructor.getParameterTypes().length == 1
                            && constructor.getParameterTypes()[0].equals(JsonObject.class))
                    .findFirst()
                    .orElse(null);
            return emptyConstructor == null;
        }).collect(Collectors.toList());

        invalidModel.forEach(modelClass -> {
            String message = String.format("[PresencesCommon@%s::testSubClassIModel]: The class %s must have public constructor with JsonObject parameter declared",
                    this.getClass().getSimpleName(), modelClass.getSimpleName());
            log.fatal(message);
        });

        ctx.assertTrue(invalidModel.isEmpty(), "One or more IModel don't have public constructor with JsonObject parameter declared. Check log above.");
    }
}
