package fr.openent.statistics_presences.indicator;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.StatisticsUser;
import fr.openent.presences.model.StructureStatisticsUser;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.bean.Report;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import fr.wseduc.webutils.template.TemplateProcessor;
import fr.wseduc.webutils.template.lambdas.I18nLambda;
import fr.wseduc.webutils.template.lambdas.LocaleDateLambda;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.busmods.BusModBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class ProcessingScheduledManual extends BusModBase implements Handler<Message<JsonObject>> {
    Logger log = LoggerFactory.getLogger(ProcessingScheduledManual.class);
    private CommonServiceFactory commonServiceFactory;
    TemplateProcessor templateProcessor;
    Long start = null;

    @Override
    public void start() {
        super.start();
        this.commonServiceFactory = new CommonServiceFactory(vertx);
        eb.consumer(this.getClass().getName(), this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(Message<JsonObject> eventMessage) {
        Boolean isWaitingEndProcess = eventMessage.body().getBoolean(Field.ISWAITINGENDPROCESS, false);
        if (Boolean.FALSE.equals(isWaitingEndProcess)) eventMessage.reply(new JsonObject().put(Field.STATUS, Field.OK));
        log.info("[" + this.getClass().getSimpleName() + "] receiving from route /process/statistics/tasks");
        start = System.currentTimeMillis();
        initTemplateProcessor();
        Future<List<Report>> init;
        JsonObject params = new JsonObject();
        List<String> studentIds;
        if (eventMessage.body().containsKey(Field.STRUCTURE_STATISTICS_USERS)) {
            List<StructureStatisticsUser> structureStatisticsUserList = IModelHelper.toList(eventMessage.body().getJsonArray(Field.STRUCTURE_STATISTICS_USERS), StructureStatisticsUser.class);
            init = this.processIndicators(structureStatisticsUserList);
            studentIds = structureStatisticsUserList.stream()
                    .flatMap(structureStatisticsUser -> structureStatisticsUser.getStatisticsUsers().stream())
                    .map(StatisticsUser::getId)
                    .collect(Collectors.toList());
        } else {
            //Deprecated
            List<String> structures = eventMessage.body().getJsonArray(Field.STRUCTURE, new JsonArray()).getList();
            studentIds = eventMessage.body().getJsonArray(Field.STUDENTIDS, new JsonArray()).getList();
            init = fetchUsers(structures, studentIds)
                    .compose(this::processIndicators);
        }

        init.compose(this::generateReport)
                .compose(report -> {
                    params.put(Field.RESULT, report);
                    return this.commonServiceFactory.getStatisticsPresencesService().clearWaitingList(studentIds);
                })
                .onSuccess(success -> {
                    if (Boolean.TRUE.equals(isWaitingEndProcess))
                        eventMessage.reply(new JsonObject().put(Field.STATUS, Field.OK));
                    else
                        log.info(params.getString(Field.RESULT));
                })
                .onFailure(error -> {
                    String message = String.format("[StatisticsPresences@ProcessingScheduledManual::handle] " +
                            "Processing scheduled manual task failed. See previous logs. %s", error.getMessage());
                    log.error(message);
                    error.printStackTrace();
                    if (Boolean.TRUE.equals(isWaitingEndProcess))
                        eventMessage.reply(new JsonObject()
                                .put(Field.STATUS, Field.ERROR)
                                .put(Field.MESSAGE, message));
                });
    }

    protected void initTemplateProcessor() {
        templateProcessor = new TemplateProcessor().escapeHTML(false);
        templateProcessor.setLambda("i18n", new I18nLambda("fr"));
        templateProcessor.setLambda("datetime", new LocaleDateLambda("fr"));
    }

    protected Future<JsonObject> fetchUsers(List<String> structureIds, List<String> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return fetchUsersFromStructures(structureIds);
        if (structureIds == null || structureIds.isEmpty()) return Future.succeededFuture(new JsonObject());
        return Future.succeededFuture(new JsonObject().put(structureIds.get(0), studentIds));
    }

    /**
     * Fetch user list in database. The list contains all users identifier that need to be proceed;
     *
     * @return Future handling result
     */
    @SuppressWarnings("unchecked")
    private Future<JsonObject> fetchUsersFromStructures(List<String> structures) {
        Promise<JsonObject> promise = Promise.promise();
        commonServiceFactory.userService().fetchAllStudentsFromStructure(structures)
                .onSuccess(structuresResult -> {
                    JsonObject structuresObject = new JsonObject();
                    ((List<JsonObject>) structuresResult.getList()).forEach(struct -> {
                        JsonArray users = struct.getJsonArray("users");
                        if (!users.isEmpty()) {
                            structuresObject.put(struct.getString("structure"), users);
                        }
                    });
                    promise.complete(structuresObject);
                })
                .onFailure(promise::fail);
        return promise.future();
    }


    /**
     * Launch indicators process. The process compute values for each user and store it in the database.
     *
     * @param structures structure map. Contains in key structure identifier and in value an array containing each structure
     *                   student to proceed
     * @return Future handling result
     * @deprecated Replaced by {@link #processIndicators(List)}
     */
    @Deprecated
    private Future<List<Report>> processIndicators(JsonObject structures) {
        Promise<List<Report>> promise = Promise.promise();
        List<Future<Report>> indicatorFutures = new ArrayList<>();
        for (Indicator indicator : StatisticsPresences.indicatorMap.values()) {
            indicatorFutures.add(indicator.process(structures));
        }

        Future.join(indicatorFutures)
                .onSuccess(success -> {
                    List<Report> reports = new ArrayList<>();
                    for (Future<Report> reportFuture : indicatorFutures) {
                        reports.add(reportFuture.result());
                    }
                    promise.complete(reports);
                })
                .onFailure(error -> {
                    log.error(String.format("[StatisticsPresences@ProcessingScheduledManual::processIndicators] Some indicator failed during processing. %s", error.getMessage()));
                    promise.fail(error.getMessage());
                });

        return promise.future();
    }

    /**
     * Launch indicators process. The process compute values for each user and store it in the database.
     *
     * @param structureStatisticsUserList user stats data group by structure
     * @return Future handling result
     */
    private Future<List<Report>> processIndicators(List<StructureStatisticsUser> structureStatisticsUserList) {
        Promise<List<Report>> promise = Promise.promise();
        IndicatorGeneric.process(vertx, structureStatisticsUserList)
                .onSuccess(report -> promise.complete(Arrays.asList(report)))
                .onFailure(error -> {
                    log.error(String.format("[StatisticsPresences@ProcessingScheduledManual::processIndicators] Some indicator failed during processing. %s", error.getMessage()));
                    promise.fail(error.getMessage());
                });

        return promise.future();
    }

    /**
     * generate and log a report
     *
     * @param reports Reports list
     * @return Future handling result
     */
    private Future<String> generateReport(List<Report> reports) {
        Promise<String> promise = Promise.promise();
        Long end = System.currentTimeMillis();
        JsonObject params = new JsonObject()
                .put("date", start)
                .put("start", start)
                .put("end", end)
                .put("runTime", end - start)
                .put("indicators", new JsonArray(reports.stream().map(Report::toJSON).collect(Collectors.toList())));

        templateProcessor.processTemplate("indicators/report.txt", params, report -> {
            if (report == null) {
                promise.fail(new RuntimeException("Report is null. Maybe template is not found? Please check logs."));
            } else {
                promise.complete(report);
            }
        });

        return promise.future();
    }
    
    /**
     * @deprecated Replaced by {@link fr.openent.statistics_presences.service.StatisticsPresencesService#clearWaitingList(List)}
     */
    protected Future<Void> clearWaitingList(List<String> studentIds) {
        if (studentIds.isEmpty()) {
            return Future.succeededFuture();
        }
        Promise<Void> promise = Promise.promise();
        String query = String.format("DELETE FROM %s.user WHERE id IN " + Sql.listPrepared(studentIds) + " ;", StatisticsPresences.DB_SCHEMA);
        JsonArray params = new JsonArray().addAll(new JsonArray(studentIds));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[Statistics@%s::clearWaitingList] Fail to clear waiting list %s",
                        this.getClass().getSimpleName(), either.left().getValue()));
                promise.fail(either.left().getValue());
            }
            else promise.complete();
        }));

        return promise.future();
    }
}
