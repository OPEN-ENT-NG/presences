package fr.openent.presences.common.helper;

import fr.openent.presences.model.IModel;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ⚠ If you use this helper you must have the tests that go with it.
 * ⚠ These tests will make it possible to verify the correct implementation of all the classes that implement IModel.
 * ⚠ This will guarantee the correct execution of the line modelClass.getConstructor(JsonObject.class).newInstance(iModel).
 */
public class IModelHelper {
    private final static List<Class<?>> validJsonClasses = Arrays.asList(String.class, boolean.class, Boolean.class,
            double.class, Double.class, float.class, Float.class, Integer.class, int.class, CharSequence.class,
            JsonObject.class, JsonArray.class, Long.class, long.class, Instant.class);

    private IModelHelper() {
        throw new IllegalStateException("Utility class");
    }

    @SuppressWarnings("unchecked")
    public static <T extends IModel<T>> List<T> toList(JsonArray results, Class<T> modelClass) {
        return results.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .map(iModel -> {
                    try {
                        return modelClass.getConstructor(JsonObject.class).newInstance(iModel);
                    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                             InvocationTargetException e) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static JsonArray toJsonArray(List<? extends IModel<?>> dataList) {
        return new JsonArray(dataList.stream().map(IModel::toJson).collect(Collectors.toList()));
    }

    /**
     * Generic convert an {@link IModel} to {@link JsonObject}.
     * Classes that do not implement any {@link #validJsonClasses} class or iModel implementation will be ignored.
     * Except {@link List} and {@link Enum}
     *
     * @param ignoreNull If true ignore, fields that are null will not be put in the result
     * @param iModel Instance of {@link IModel} to convert to {@link JsonObject}
     * @return {@link JsonObject}
     */
    public static JsonObject toJson(boolean ignoreNull, IModel<?> iModel) {
        JsonObject statisticsData = new JsonObject();
        final Field[] declaredFields = iModel.getClass().getDeclaredFields();
        Arrays.stream(declaredFields).forEach(field -> {
            boolean accessibility = field.isAccessible();
            field.setAccessible(true);
            try {
                Object object = field.get(iModel);
                String fieldName = field.getName();
                if (object == null) {
                    if (!ignoreNull) statisticsData.putNull(StringHelper.camelToSnake(fieldName));
                }
                else if (object instanceof IModel) {
                    statisticsData.put(StringHelper.camelToSnake(fieldName), ((IModel<?>)object).toJson());
                } else if (validJsonClasses.stream().anyMatch(aClass -> aClass.isInstance(object))) {
                    statisticsData.put(StringHelper.camelToSnake(fieldName), object);
                } else if (object instanceof Enum) {
                    statisticsData.put(StringHelper.camelToSnake(fieldName), (Enum) object);
                } else if (object instanceof List) {
                    statisticsData.put(StringHelper.camelToSnake(fieldName), listToJsonArray(((List<?>)object)));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            field.setAccessible(accessibility);
        });
        return statisticsData;
    }

    /**
     * Generic convert a list of {@link Object} to {@link JsonArray}.
     * Classes that do not implement any {@link #validJsonClasses} class or iModel implementation will be ignored.
     * Except {@link List} and {@link Enum}
     *
     * @param objects List of object
     * @return {@link JsonArray}
     */
    private static JsonArray listToJsonArray(List<?> objects) {
        JsonArray res = new JsonArray();
        objects.stream()
                .filter(Objects::nonNull)
                .forEach(object -> {
                    if (object instanceof IModel) {
                        res.add(((IModel<?>) object).toJson());
                    }
                    else if (validJsonClasses.stream().anyMatch(aClass -> aClass.isInstance(object))) {
                        res.add(object);
                    } else if (object instanceof Enum) {
                        res.add((Enum)object);
                    } else if (object instanceof List) {
                        res.add(listToJsonArray(((List<?>) object)));
                    }
                });
        return res;
    }
}