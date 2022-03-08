package fr.openent.statistics_presences.indicator;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.presences.Presences;
import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.helper.RegisterHelper;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class ProcessingWeeklyAudiencesManual extends AbstractVerticle {
    Logger log = LoggerFactory.getLogger(ProcessingWeeklyAudiencesManual.class);
    private CommonServiceFactory commonServiceFactory;

    @Override
    @SuppressWarnings("unchecked")
    public void start() {
        this.commonServiceFactory = new CommonServiceFactory(vertx);
        List<String> structureIds = config().getJsonArray(Field.STRUCTUREIDS, new JsonArray()).getList();
        String startAt = config().getString(Field.STARTAT);
        String endAt = config().getString(Field.ENDAT);

        process(structureIds, startAt, endAt);
    }

    public void process(List<String> structureIds, String startAt, String endAt) {
        log.info(String.format("[StatisticsPresences@%s::process] receiving from route /process/weekly/audiences/tasks",
                this.getClass().getSimpleName()));
        processStructures(structureIds, startAt, endAt);
    }

    /**
     * Launch process by structure.
     *
     * @param structureIds List of structure identifiers.
     * @param startAt      start period to get registers (optional).
     * @param endAt        end period to get registers (optional).
     * @return Future handling structureIds list that succeeded
     */
    private void processStructures(List<String> structureIds, String startAt, String endAt) {
        List<Future<String>> structuresFutures = new ArrayList<>();

        for (String structureId : structureIds) {
            structuresFutures.add(processStructure(structureId, startAt, endAt));
        }

        FutureHelper.join(structuresFutures)
                .onComplete(structuresRes -> sendSigTerm(structuresRes, structuresFutures));
    }

    /**
     * Launch process structure. The process compute values for each registers of the structure and store it
     * in the database as weeklyAudience.
     *
     * @param structureId structure identifiers.
     * @param startAt     start of period to get registers (optional).
     * @param endAt       end of period to get registers (optional).
     * @return Future handling structureId when it succeeded
     */
    private Future<String> processStructure(String structureId, String startAt, String endAt) {
        Promise<String> promise = Promise.promise();
        Presences.getInstance().getRegistersWithGroups(structureId, null, startAt, endAt)
                .compose(registersResult -> commonServiceFactory.statisticsWeeklyAudiencesService()
                        .createFromRegisters(structureId, RegisterHelper.getRegistersFromArray(registersResult)))
                .onSuccess(result -> promise.complete(structureId))
                .onFailure(error -> {
                            String message = String.format("[StatisticsPresences@%s::processStructures] " +
                                            "Processing weekly audiences failed for structure %s. %s",
                                    this.getClass().getSimpleName(), structureId, error.getMessage());
                            log.error(message);
                            promise.fail(message);
                        }
                );
        return promise.future();
    }


    private void sendSigTerm(AsyncResult<CompositeFuture> ar, List<Future<String>> structuresFutures) {
        if (ar.failed()) {
            String message = String.format("[StatisticsPresences@%s::sendSigTerm] Some structures failed during processing",
                    this.getClass().getSimpleName());
            log.error(message, ar.cause().getMessage());
        } else {
            List<String> structureResultIds = structuresFutures.stream()
                    .filter(Future::succeeded)
                    .map(Future::result)
                    .collect(Collectors.toList());

            log.info(String.format("[StatisticsPresences@%s::sendSigTerm] process succeeded for structures %s.",
                    this.getClass().getSimpleName(), structureResultIds));
        }


        log.info("[StatisticsPresences@IndicatorWorker::sendSigTerm] Sending term signal");
        vertx.undeploy(vertx.getOrCreateContext().deploymentID());

    }
}
