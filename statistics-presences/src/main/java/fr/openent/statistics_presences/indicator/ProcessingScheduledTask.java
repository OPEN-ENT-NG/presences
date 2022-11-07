package fr.openent.statistics_presences.indicator;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.bean.Report;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.template.TemplateProcessor;
import fr.wseduc.webutils.template.lambdas.I18nLambda;
import fr.wseduc.webutils.template.lambdas.LocaleDateLambda;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessingScheduledTask implements Handler<Long> {
    Logger log = LoggerFactory.getLogger(ProcessingScheduledTask.class);
    Vertx vertx;
    EmailSender emailSender;
    JsonObject config;
    TemplateProcessor templateProcessor;
    Long start = null;

    public ProcessingScheduledTask(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
        this.emailSender = new EmailFactory(vertx, null).getSender();
    }

    @Override
    public void handle(Long event) {
        start = System.currentTimeMillis();
        initTemplateProcessor();
        fetchUsersToProcess()
                .compose(this::processIndicators)
                .compose(this::generateReport)
                .compose(this::sendReport)
                .compose(this::clearWaitingList)
                .onComplete(ar -> {
                    if (ar.failed()) {
                        log.error(String.format("[Statistics@ProcessingScheduledTask::handle] " +
                                "Processing scheduled task failed. See previous logs. %s", ar.cause().getMessage()));
                    }
                });
    }

    private void initTemplateProcessor() {
        templateProcessor = new TemplateProcessor(vertx, "template").escapeHTML(false);
        templateProcessor.setLambda("i18n", new I18nLambda("fr"));
        templateProcessor.setLambda("datetime", new LocaleDateLambda("fr"));
    }

    private Future<Void> clearWaitingList(Void unused) {
        Future<Void> future = Future.future();
        String query = String.format("TRUNCATE TABLE %s.user;", StatisticsPresences.DB_SCHEMA);
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) future.fail(either.left().getValue());
            else future.complete();
        }));

        return future;
    }

    /**
     * Fetch user list in database. The list contains all users identifier that need to be proceed;
     *
     * @return Future handling result
     */
    private Future<JsonObject> fetchUsersToProcess() {
        Future<JsonObject> future = Future.future();
        String query = String.format("SELECT structure, json_agg(id) as users FROM %s.user GROUP BY structure", StatisticsPresences.DB_SCHEMA);
        
        Sql.getInstance().raw(query, SqlResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[Statistics@ProcessingScheduledTask::fetchUsersToProcess] " +
                        "Failed to retrieve users to process. %s", either.left().getValue()));
                future.fail(either.left().getValue());
            } else {
                JsonArray result = either.right().getValue();
                JsonObject structures = new JsonObject();
                ((List<JsonObject>) result.getList()).forEach(structure -> {
                    JsonArray users = new JsonArray(structure.getString("users"));
                    if (!users.isEmpty()) {
                        structures.put(structure.getString("structure"), users);
                    }
                });
                future.complete(structures);
            }
        }));

        return future;
    }

    /**
     * Launch indicators process. The process compute values for each user and store it in the database.
     *
     * @param structures structure map. Contains in key structure identifier and in value an array containing each structure
     *                   student to proceed
     * @return Future handling result
     */
    private Future<List<Report>> processIndicators(JsonObject structures) {
        Promise<List<Report>> promise = Promise.promise();
        List<Future<Report>> indicatorFutures = new ArrayList<>();
        for (Indicator indicator : StatisticsPresences.indicatorMap.values()) {
            indicatorFutures.add(indicator.process(structures));
        }

        FutureHelper.join(indicatorFutures)
                .onSuccess(ar -> {
                    List<Report> reports = new ArrayList<>();
                    for (Future<Report> reportFuture : indicatorFutures) {
                        reports.add(reportFuture.result());
                    }

                    promise.complete(reports);
                })
                .onFailure(fail -> {
                    log.error(String.format("[Statistics@ProcessingScheduledTask::processIndicators] " +
                            "Some indicator failed during processing. %s", fail.getMessage()));
                    promise.fail(fail.getCause());
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
        Future<String> future = Future.future();
        Long end = System.currentTimeMillis();
        JsonObject params = new JsonObject()
                .put("date", start)
                .put("start", start)
                .put("end", end)
                .put("runTime", end - start)
                .put("indicators", new JsonArray(reports.stream().map(Report::toJSON).collect(Collectors.toList())));

        templateProcessor.processTemplate("indicators/report.txt", params, report -> {
            if (report == null) {
                future.fail(new RuntimeException("[Statistics@ProcessingScheduledTask::generateReport] " +
                        "Report is null. Maybe template is not found? Please check logs."));
            } else {
                future.complete(report);
            }
        });

        return future;
    }

    private Future<Void> sendReport(String report) {
        Promise<Void> promise = Promise.promise();
        List<Future> futures = new ArrayList<>();
        JsonArray recipients = config.getJsonArray("report-recipients", new JsonArray());
        if (recipients.isEmpty()) {
            log.info(report);
            return Future.succeededFuture();
        }

        String title = String.format("[%s] Rapport de calcul statistiques", config.getString("host"));
        for (int i = 0; i < recipients.size(); i++) {
            Future emailFuture = Promise.promise().future();
            futures.add(emailFuture);
            emailSender.sendEmail(null, recipients.getString(i), null, null, title, report, null, false, emailFuture);
        }

        CompositeFuture.join(futures)
                .onSuccess(ar -> promise.complete())
                .onFailure(promise::fail);

        return promise.future();
    }
}
