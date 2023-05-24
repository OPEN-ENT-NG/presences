package fr.openent.presences.helper;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.Alert;
import fr.openent.presences.model.IModel;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class PaginationHelper {
    private PaginationHelper() {

    }

    public static JsonObject getPaginationResponse(Integer page, Integer elementCount, List<? extends IModel<?>> data) {
        JsonObject res = new JsonObject();

        elementCount = elementCount == null ? 0 : elementCount;
        page = page == null ? 0 : page;


        Integer pageCount = elementCount
                .equals(Presences.ALERT_PAGE_SIZE) ? 0 :
                elementCount / Presences.ALERT_PAGE_SIZE;

        res.put(Field.PAGE, page)
                .put(Field.PAGE_COUNT, pageCount)
                .put(Field.ALL, IModelHelper.toJsonArray(data));

        return res;
    }
}
