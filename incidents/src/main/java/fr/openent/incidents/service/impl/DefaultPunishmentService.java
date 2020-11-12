package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.helper.PunishmentHelper;
import fr.openent.incidents.model.Punishment;
import fr.openent.incidents.service.PunishmentService;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.user.UserInfos;

import java.util.*;

public class DefaultPunishmentService implements PunishmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPunishmentService.class);
    private final PunishmentHelper punishmentHelper;
    private Punishment punishment = new Punishment();


    public DefaultPunishmentService(EventBus eb) {
        this.punishmentHelper = new PunishmentHelper(eb);
    }

    @Override
    public void create(UserInfos user, JsonObject body, Handler<AsyncResult<JsonArray>> handler) {

        JsonArray student_ids = body.getJsonArray("student_ids");
        JsonArray results = new JsonArray();
        List<Future> futures = new ArrayList<>();

        for (Object oStudent_id : student_ids) {
            Future future = Future.future();
            futures.add(future);
            String student_id = (String) oStudent_id;
            Punishment createPunishment = new Punishment();
            createPunishment.setFromJson(body);
            createPunishment.setStudentId(student_id);

            createPunishment.persistMongo(user, result -> {
                if (result.failed()) {
                    future.fail(result.cause().getMessage());
                } else {
                    results.add(result.result());
                    future.complete();
                }
            });
        }

        CompositeFuture.join(futures).setHandler(event -> {
            if (event.failed()) {
                handler.handle(Future.failedFuture(event.cause().toString()));
                return;
            }
            handler.handle(Future.succeededFuture(results));
        });
    }

    @Override
    public void update(UserInfos user, JsonObject body, Handler<AsyncResult<JsonObject>> handler) {
        Punishment updatePunishment = new Punishment();
        updatePunishment.setFromJson(body);
        updatePunishment.persistMongo(user, result -> {
            if (result.failed()) {
                handler.handle(Future.failedFuture(result.cause().getMessage()));
                return;
            }
            handler.handle(Future.succeededFuture(result.result()));
        });
    }

    @Override
    public void get(UserInfos user, MultiMap body, boolean isStudent, Handler<AsyncResult<JsonObject>> handler) {
        punishmentHelper.getQuery(user, body, isStudent, queryResult -> {
            if (queryResult.failed()) {
                handler.handle(Future.failedFuture(queryResult.cause().getMessage()));
                return;
            }

            String id = body.get("id");

            if (id != null && !id.equals("")) {
                punishmentHelper.getPunishment(punishment.getTable(), queryResult.result(), handler);
            } else {
                Future<Long> countFuture = Future.future();
                Future<JsonArray> findFuture = Future.future();

                /* PAGINATE QUERY PUNISHMENTS */
                String pageString = body.get("page");
                String limitString = body.get("limit");
                String offsetString = body.get("offset");
                Integer limit, offset, page = null;
                int aPageNumber = Incidents.PAGE_SIZE;

                if (pageString != null && !pageString.equals("")) {
                    page = Integer.parseInt(pageString);
                    offset = page * aPageNumber;
                    limit = aPageNumber;
                } else {
                    offset = offsetString != null && !offsetString.equals("") ? Integer.parseInt(offsetString) : 0;
                    limit = limitString != null && !limitString.equals("") ? Integer.parseInt(limitString) : -1;
                }

                punishmentHelper.getPunishments(punishment.getTable(), queryResult.result(), limit, offset, result -> {
                    if (result.failed()) {
                        findFuture.fail(result.cause());
                        return;
                    }
                    findFuture.complete(result.result());
                });

                punishmentHelper.countPunishments(punishment.getTable(), queryResult.result(), result -> {
                    if (result.failed()) {
                        countFuture.fail(result.cause());
                        return;
                    }
                    countFuture.complete(result.result());
                });

                Integer finalPage = page;
                CompositeFuture.all(countFuture, findFuture).setHandler(resultFuture -> {
                    if (resultFuture.failed()) {
                        handler.handle(Future.failedFuture(resultFuture.cause()));
                    } else {
                        formatPunishmentsResult(countFuture.result(), findFuture.result(), finalPage, limit, offset, handler);
                    }
                });
            }
        });
    }

    private void formatPunishmentsResult(Long punishmentsNumber, JsonArray punishments,
                                        Integer page, Integer limit, Integer offset, Handler<AsyncResult<JsonObject>> handler) {
        JsonObject finalResult = new JsonObject();
        if (page != null) {
            Long pageCount = punishmentsNumber <= Incidents.PAGE_SIZE ?
                    0 : (long) Math.ceil(punishmentsNumber / (double) Incidents.PAGE_SIZE);
            finalResult
                    .put("page", page)
                    .put("page_count", pageCount);
        } else {
            finalResult
                    .put("limit", limit)
                    .put("offset", offset);
        }

        finalResult.put("all", punishments);
        handler.handle(Future.succeededFuture(finalResult));
    }

    @Override
    public void count(UserInfos user, MultiMap body, boolean isStudent, Handler<AsyncResult<Long>> handler) {
        punishmentHelper.getQuery(user, body, isStudent, result -> {
            if(result.failed()) {
                handler.handle(Future.failedFuture(result.cause().getMessage()));
                return;
            }
            punishmentHelper.countPunishments(punishment.getTable(), result.result(), handler);
        });
    }

    @Override
    public void delete(UserInfos user, MultiMap body, Handler<AsyncResult<JsonObject>> handler) {
        JsonObject query = new JsonObject()
                .put("_id", body.get("id"));
        MongoDb.getInstance().delete(punishment.getTable(), query, message -> {
            Either<String, JsonObject> messageCheck = MongoDbResult.validResult(message);
            if (messageCheck.isLeft()) {
                handler.handle(Future.failedFuture("[Incidents@Punishment::persistMongo] Failed to delete punishment"));
            } else {
                JsonObject result = messageCheck.right().getValue();
                handler.handle(Future.succeededFuture(result));
            }
        });
    }
}
