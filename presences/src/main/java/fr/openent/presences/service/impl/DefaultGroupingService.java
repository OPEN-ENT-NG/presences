package fr.openent.presences.service.impl;

import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.model.grouping.Grouping;
import fr.openent.presences.service.GroupingService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class DefaultGroupingService implements GroupingService {
    private static final Logger log = LoggerFactory.getLogger(DefaultGroupingService.class);

    @Override
    public Future<List<Grouping>> getGroupingStructure(String structureId) {
        return searchGrouping(structureId, null);
    }

    @Override
    public Future<List<Grouping>> searchGrouping(String structureId, String searchValue) {
        Promise<List<Grouping>> promise = Promise.promise();
        Viescolaire.getInstance().getGroupingStructure(structureId, searchValue)
                .onSuccess(groupingList -> promise.complete(this.filterGrouping(groupingList, searchValue)))
                .onFailure(error -> {
                    String messageError = String.format("[Presences@%s::listGroupings] Error when get grouping list %s",
                            this.getClass().getSimpleName(), error.getMessage());
                    log.error(messageError);
                    promise.fail(messageError);
                });

        return promise.future();
    }

    private List<Grouping> filterGrouping(JsonArray groupingArray, String searchValue) {
        List<Grouping> groupingList = groupingArray.stream()
                .map(JsonObject.class::cast)
                .map(Grouping::new)
                .filter(grouping -> grouping.getName().startsWith(searchValue))
                .collect(Collectors.toList());

        return groupingList;
    }
}
