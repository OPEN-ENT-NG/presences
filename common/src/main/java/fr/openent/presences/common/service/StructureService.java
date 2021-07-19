package fr.openent.presences.common.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import java.util.List;

public interface StructureService {

    /**
     * fetch structures info
     *
     * @param structuresId list of structure identifier
     * @param handler      handler
     */
    void fetchStructuresInfos(List<String> structuresId, Handler<Either<String, JsonArray>> handler);

    /**
     * fetch structures info
     *
     * @param structuresId list of structure identifier
     * @return {@link Future<JsonArray>}
     */
    Future<JsonArray> fetchStructuresInfos(List<String> structuresId);

    /**
     * fetch all structures using presence's module
     * @return {@link Future<JsonArray>}
     */
    Future<JsonArray> fetchActiveStructure();
}
