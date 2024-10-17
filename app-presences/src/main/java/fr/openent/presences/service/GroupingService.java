package fr.openent.presences.service;

import fr.openent.presences.model.grouping.Grouping;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;

import java.util.List;

public interface GroupingService {
    /**
     * List groupings
     * @param structureId   Identifier of the structure to which the groupings belong
     * @return              Promise with the status of the grouping creation.
     */
    Future<List<Grouping>> getGroupingStructure(String structureId);

    /**
     * List groupings
     * @param structureId   Identifier of the structure to which the groupings belong
     * @param searchValue   Student division must start by this value
     * @return              Promise with the status of the grouping creation.
     */
    Future<List<Grouping>> searchGrouping(String structureId, String searchValue);
}
