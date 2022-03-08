package fr.openent.statistics_presences.service;

import fr.openent.statistics_presences.bean.Register;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface StatisticsWeeklyAudiencesService {
    /**
     * Create statistics weekly audiences
     *
     * @param structureId structure identifier
     * @param registerIds register identifiers to convert
     * @return Future containing creation the result
     */
    Future<JsonObject> create(String structureId, List<Integer> registerIds);

    /**
     * Create statistics weekly audiences from registers
     *
     * @param structureId structure identifier
     * @param registers registers to convert
     * @return Future containing creation the result
     */
    Future<JsonObject> createFromRegisters(String structureId, List<Register> registers);

    /**
     * Fire worker that convert registers to statisticsWeeklyAudiences
     *
     * @param structureIds list structure identifier
     * @param startAt start date to retrieve register to convert
     * @param endAt end date to retrieve register to convert
     * @return Future JsonObject completing process
     */
    Future<JsonObject> processWeeklyAudiencesPrefetch(List<String> structureIds, String startAt, String endAt);
}
