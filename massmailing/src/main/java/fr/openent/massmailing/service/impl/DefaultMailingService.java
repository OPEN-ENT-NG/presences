package fr.openent.massmailing.service.impl;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.helper.MailingHelper;
import fr.openent.massmailing.model.Mailing.Mailing;
import fr.openent.massmailing.service.MailingService;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.List;

public class DefaultMailingService implements MailingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMailingService.class);
    private static String defaultStartTime = "00:00:00";
    private static String defaultEndTime = "23:59:59";

    private MailingHelper mailingHelper;

    public DefaultMailingService() {
        this.mailingHelper = new MailingHelper();
    }

    @Override
    public void getMailings(String structureId, String startDate, String endDate, List<String> mailingTypes, List<String> eventTypes,
                            List<String> studentsIds, Integer page, Handler<Either<String, JsonArray>> handler) {
        fetchMailings(structureId, startDate, endDate, mailingTypes, eventTypes, studentsIds, page, mailingAsync -> {
            if (mailingAsync.failed()) {
                String message = "[Massmailing@DefaultMailingService] Failed to retrieve mailings history";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message + " " + mailingAsync.cause()));
            }
            List<Mailing> mailings = mailingAsync.result();

            Future<JsonObject> studentFuture = Future.future();
            Future<JsonObject> recipientFuture = Future.future();

            List<String> studentIds = new ArrayList<>();
            List<String> recipientIds = new ArrayList<>();

            mailings.forEach(mailing -> {
                studentIds.add(mailing.getStudent().getId());
                recipientIds.add(mailing.getRecipient().getId());
            });

            CompositeFuture.all(studentFuture, recipientFuture).setHandler(finalResultAsync -> {
                if (finalResultAsync.failed()) {
                    String message = "[Presences@DefaultPresenceService] Failed to add mailingEvent, Students or Recipient to mailing";
                    LOGGER.error(message + finalResultAsync.cause());
                    handler.handle(new Either.Left<>(message + finalResultAsync.cause()));
                } else {
                    handler.handle(new Either.Right<>(mailingHelper.toMailingsJsonArray(mailings)));
                }
            });

            mailingHelper.addStudentToMailing(structureId, mailings, studentIds, studentFuture);
            mailingHelper.addRecipientToMailing(mailings, recipientIds, recipientFuture);
        });
    }

    private void fetchMailings(String structureId, String startDate, String endDate, List<String> mailingTypes, List<String> eventTypes,
                               List<String> studentsIds, Integer page, Handler<AsyncResult<List<Mailing>>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT mailing.*, to_json(me) as mailing_event FROM " + Massmailing.dbSchema + ".mailing as mailing" +
                " INNER JOIN " + Massmailing.dbSchema + ".mailing_event as me on (me.mailing_id = mailing.id)" +
                " WHERE mailing.structure_id = ? AND (mailing.created >= ? AND mailing.created <= ?) ";
        params.add(structureId)
                .add(startDate + " " + defaultStartTime)
                .add(endDate + " " + defaultEndTime);

        /* Add mailing types filter */
        if (!mailingTypes.isEmpty()) {
            query += " AND mailing.type IN " + Sql.listPrepared(mailingTypes);
            params.addAll(new JsonArray(mailingTypes));
        }

        /* Add event types filter */
        if (!eventTypes.isEmpty()) {
            query += " AND me.event_type IN " + Sql.listPrepared(eventTypes);
            params.addAll(new JsonArray(eventTypes));
        }

        /* Add students filter */
        if (!studentsIds.isEmpty()) {
            query += " AND mailing.student_id IN " + Sql.listPrepared(studentsIds);
            params.addAll(new JsonArray(studentsIds));
        }

        query += " GROUP BY mailing.id, me.id, mailing.created ORDER BY mailing.created DESC OFFSET ? LIMIT ? ";
        params.add(Massmailing.PAGE_SIZE * page);
        params.add(Massmailing.PAGE_SIZE);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(mailingAsync -> {
            if (mailingAsync.isLeft()) {
                String message = "[Massmailing@DefaultMailingService] Failed to retrieve mailings history";
                LOGGER.error(message);
                handler.handle(Future.failedFuture(message + " " + mailingAsync.left().getValue()));
            } else {
                handler.handle(Future.succeededFuture(this.mailingHelper.getMailingListFromJsonArray(mailingAsync.right().getValue())));
            }
        }));
    }

    @Override
    public void getMailingsPageNumber(String structureId, String startDate, String endDate, List<String> mailingTypes,
                                      List<String> eventTypes, List<String> studentsIds, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT COUNT(mailing.id) FROM " + Massmailing.dbSchema + ".mailing as mailing" +
                " INNER JOIN " + Massmailing.dbSchema + ".mailing_event as me on (me.mailing_id = mailing.id)" +
                " WHERE mailing.structure_id = ? AND (mailing.created >= ? AND mailing.created <= ?) ";
        params.add(structureId).add(startDate).add(endDate);

        /* Add mailing types filter */
        if (!mailingTypes.isEmpty()) {
            query += " AND mailing.type IN " + Sql.listPrepared(mailingTypes);
            params.addAll(new JsonArray(mailingTypes));
        }

        /* Add event types filter */
        if (!eventTypes.isEmpty()) {
            query += " AND me.event_type IN " + Sql.listPrepared(eventTypes);
            params.addAll(new JsonArray(eventTypes));
        }

        /* Add students filter */
        if (!studentsIds.isEmpty()) {
            query += " AND mailing.student_id IN " + Sql.listPrepared(studentsIds);
            params.addAll(new JsonArray(studentsIds));
        }
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
