package fr.openent.statistics_presences.indicator;


import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ComputeStatistics extends ProcessingScheduledManual {

    private final CommonServiceFactory commonServiceFactory;

    /**
     * Even though this class is designed to be a Verticle worker mode, we can instantiate
     * this class in such a way as to run it "manually"
     *
     * @param commonServiceFactory  serviceFactory given in order to access some components/services {@link CommonServiceFactory}
     */
    public ComputeStatistics(CommonServiceFactory commonServiceFactory) {
        this.commonServiceFactory = commonServiceFactory;
    }

    /**
     * This method will process recompute statistics
     * It is the same thing as the method `handle` triggered by its vertx consumer except there are no report log
     * WARNING : Must instantiate this class before using this method
     *
     * @param structures  list of structure identifier List of {@link String}
     * @param studentIds  list of students identifier List {@link String}
     */
    public Future<Void> start(List<String> structures, List<String> studentIds) {
        Promise<Void> promise = Promise.promise();
        initTemplateProcessor();
        fetchUsers(structures, studentIds)
                .compose(this::processManualIndicators)
                .onSuccess(promise::complete)
                .onFailure(error -> {
                    String message = String.format("[StatisticsPresences@%s@::manualStart] An error has occurred during " +
                            "manual process to indicator(s): %s", this.getClass().getSimpleName(), error.getMessage());
                    log.error(message);
                    error.printStackTrace();
                    promise.fail(error.getMessage());
                });
        return promise.future();
    }

    /**
     * Launch indicators manually (meaning without using Verticle context worker)
     *
     * @param structures structure map. Contains in key structure identifier and in value an array containing each structure
     *                   student to proceed
     * @return Future handling result
     */
    private Future<Void> processManualIndicators(JsonObject structures) {
        Promise<Void> promise = Promise.promise();
        List<Future<JsonObject>> indicatorFutures = new ArrayList<>();
        for (Indicator indicator : StatisticsPresences.indicatorMap.values()) {
            indicatorFutures.add(indicator.manualProcess(structures));
        }
        FutureHelper.join(indicatorFutures)
                .onSuccess(success -> promise.complete())
                .onFailure(error -> {
                    log.error(String.format("[StatisticsPresences@ProcessingScheduledManual::processIndicators] Some indicator failed during processing. %s", error.getMessage()));
                    promise.fail(error.getMessage());
                });

        return promise.future();
    }
}
