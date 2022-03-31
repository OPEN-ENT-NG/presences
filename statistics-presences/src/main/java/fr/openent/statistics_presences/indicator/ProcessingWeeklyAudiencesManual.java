package fr.openent.statistics_presences.indicator;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.presences.Presences;
import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.helper.RegisterHelper;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.vertx.java.busmods.BusModBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class ProcessingWeeklyAudiencesManual extends BusModBase implements Handler<Message<JsonObject>> {
    public static final Integer STATE_IN_PROGRESS = 2;
    public static final Integer STATE_DONE = 3;

    Logger log = LoggerFactory.getLogger(ProcessingWeeklyAudiencesManual.class);
    private CommonServiceFactory commonServiceFactory;

    @Override
    public void start() {
        super.start();
        this.commonServiceFactory = new CommonServiceFactory(vertx);
        eb.consumer(this.getClass().getName(), this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(Message<JsonObject> eventMessage) {
        log.info(String.format("[StatisticsPresences@%s::process] receiving from route /process/weekly/audiences/tasks",
                this.getClass().getSimpleName()));
        eventMessage.reply(new JsonObject().put(Field.STATUS, Field.OK));

        List<String> structureIds = eventMessage.body().getJsonArray(Field.STRUCTUREIDS, new JsonArray()).getList();
        String startAt = eventMessage.body().getString(Field.STARTAT);
        String endAt = eventMessage.body().getString(Field.ENDAT);

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
                .onFailure(err -> {
                    String message = String.format("[StatisticsPresences@%s::sendSigTerm] Some structures failed during processing",
                            this.getClass().getSimpleName());
                    log.error(message, err.getMessage());
                })
                .onSuccess(res -> {
                    List<String> structureResultIds = structuresFutures.stream()
                            .filter(Future::succeeded)
                            .map(Future::result)
                            .collect(Collectors.toList());

                    log.info(String.format("[StatisticsPresences@%s::sendSigTerm] process succeeded for structures %s.",
                            this.getClass().getSimpleName(), structureResultIds));
                });
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


        Presences.getInstance().getRegistersWithGroups(structureId, null, Arrays.asList(STATE_DONE, STATE_IN_PROGRESS),
                        startAt, endAt)
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
}
