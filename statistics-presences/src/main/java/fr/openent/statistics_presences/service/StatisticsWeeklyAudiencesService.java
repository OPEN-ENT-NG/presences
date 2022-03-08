package fr.openent.statistics_presences.service;

import fr.openent.statistics_presences.bean.Register;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface StatisticsWeeklyAudiencesService {
    /**
     * Create incident
     *
     */
    Future<JsonObject> create(String structureId, List<Integer> registerIds);
    Future<JsonObject> createFromRegisters(String structureId, List<Register> registers);

    /**
     * Add user in deleted table.
     *
     * @param structureIds list structure identifier
     * @return Future JsonObject completing process
     */
    Future<JsonObject> processWeeklyAudiencesPrefetch(List<String> structureIds, String startAt, String endAt);
}
