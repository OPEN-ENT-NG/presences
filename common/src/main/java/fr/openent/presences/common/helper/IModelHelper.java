package fr.openent.presences.common.helper;

import fr.openent.presences.model.IModel;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ⚠ If you use this helper you must have the tests that go with it.
 * ⚠ These tests will make it possible to verify the correct implementation of all the classes that implement IModel.
 * ⚠ This will guarantee the correct execution of the line modelClass.getConstructor(JsonObject.class).newInstance(iModel).
 */
public class IModelHelper {
    private IModelHelper() {
        throw new IllegalStateException("Utility class");
    }

    @SuppressWarnings("unchecked")
    public static <T extends IModel<T>> List<T> toList(JsonArray results, Class<T> modelClass) {
        return ((List<JsonObject>) results.getList()).stream()
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
}