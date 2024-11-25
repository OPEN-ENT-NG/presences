package fr.openent.presences.worker;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.helper.MapHelper;
import fr.openent.presences.service.*;
import fr.openent.presences.service.impl.*;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.*;
import org.vertx.java.busmods.BusModBase;

import java.util.*;
import java.util.stream.Collectors;

public class CreateDailyPresenceWorker extends BusModBase implements Handler<Message<JsonObject>> {
    private CommonPresencesServiceFactory commonPresencesServiceFactory;

    private EmailSender emailSender;
    private RegisterService registerService;
    private final Logger log = LoggerFactory.getLogger(CreateDailyPresenceWorker.class);


    @Override
    public void start() {
        super.start();
        this.commonPresencesServiceFactory = new CommonPresencesServiceFactory(vertx,
                new StorageFactory(vertx, config).getStorage(), config);
        this.registerService = new DefaultRegisterService(commonPresencesServiceFactory);
        this.emailSender = new EmailFactory(vertx, config).getSender();
        eb.consumer(this.getClass().getName(), this);
    }

    @Override
    public void handle(Message<JsonObject> eventMessage) {
        eventMessage.reply(new JsonObject().put("status", "ok"));
        logger.info("Calling Worker");
        processCreateDailyPresences();
    }

    private void getStructures(JsonArray ids, Promise<JsonArray> promise) {
        String query = "MATCH (s:Structure) WHERE s.id IN {ids} RETURN s.name as name, s.id as id;";
        JsonObject params = new JsonObject().put("ids", ids);
        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(FutureHelper.handlerEitherPromise(promise)));
    }

    @SuppressWarnings("unchecked")
    private void processCreateDailyPresences() {
        String today = DateHelper.getCurrentDay();
        String currentTime = DateHelper.getCurrentDate(DateHelper.HOUR_MINUTES);

        String queryStructures = "SELECT id_etablissement as id FROM " + Presences.dbSchema + " .etablissements_actifs";

        JsonObject result = new JsonObject();
        List<Future<?>> futures = new ArrayList<>();
        Sql.getInstance().prepared(queryStructures, new JsonArray(), SqlResult.validResultHandler(resultStructures -> {
            Promise<JsonArray> structurePromise = Promise.promise();
            if (resultStructures.isLeft()) {
                Promise<JsonObject> promise = Promise.promise();
                futures.add(promise.future());
                log.error(resultStructures.left().getValue());
                result.put("errorMessage", "Structures recovery failed");
                promise.complete();
            } else {
                // Tableau Structures
                result.put(Field.STRUCTURES, new JsonObject());
                JsonArray structureIds = resultStructures.right().getValue();
                List<String> stuctures = ((List<JsonObject>) structureIds.getList()).stream().map(structure -> structure.getString("id")).collect(Collectors.toList());
                if (!structureIds.isEmpty()) {
                    futures.add(structurePromise.future());
                    getStructures(new JsonArray(stuctures), structurePromise);
                }
                for (int i = 0; i < structureIds.size(); i++) {
                    String structureId = structureIds.getJsonObject(i).getString(Field.ID);
                    Promise<JsonObject> promise = Promise.promise();
                    futures.add(promise.future());
                    registerService.createStructureCoursesRegisterFuture(today, today, currentTime, currentTime,
                            result, structureId, "true", promise);
                }
            }

            Future.join(futures).onComplete(resultFutures -> {
                String title = "[" + config.getString("host") + "][Présences] Rapport d'ouverture des appels: " +
                        ((getRegistersCreationWorked(result) == Boolean.TRUE) ? "succès." : "échec.");
                JsonObject structureMap = structurePromise.future() != null && structurePromise.future().succeeded()
                        ? MapHelper.transformToMap(structurePromise.future().result(), Field.ID) : new JsonObject();
                String message = getFormattedMessage(result, structureMap);
                sendMail(title, message);
            });

        }));
    }

    private Boolean getRegistersCreationWorked(JsonObject workerResult) {
        String error = workerResult.getString("errorMessage", null);
        if (error != null) {
            return false;
        } else {
            JsonObject structures = workerResult.getJsonObject("structures");
            String errorStructure = structures.getString("errorMessage", null);
            if (errorStructure != null) {
                return false;
            }

            for (String structureId : structures.fieldNames()) {
                JsonObject structure = structures.getJsonObject(structureId);
                String errorStructures = structure.getString("errorMessage", null);

                if (errorStructures != null) {
                    return false;
                } else {
                    JsonObject coursesErrors = structure.getJsonObject("coursesErrors");
                    Set<String> coursesIds = coursesErrors.fieldNames();
                    if (coursesIds.size() > 0) {
                        return false;
                    }

                }
            }

        }

        return true;
    }

    private String getFormattedMessage(JsonObject workerResult, JsonObject structureMap) {
        StringBuilder message = new StringBuilder("<span>Rapport du " + DateHelper.getCurrentDayWithHours() + ".</span><br>");
        String error = workerResult.getString("errorMessage", null);

        if (error != null) {
            message.append("<span>Aucun appel n'a été créé: ").append(error).append("</span>");

        } else {
            JsonObject structures = workerResult.getJsonObject("structures");

            String errorStructures = structures.getString("errorMessage", null);
            if (errorStructures != null) {
                message.append("<span>Erreur à la récupération des structures, aucun appel n'a été créé: ").append(errorStructures).append("</span>");
                return message.toString();
            }

            Iterator<String> structureIds = structures.fieldNames().iterator();
            Integer succeededCoursesNumber = 0;
            StringBuilder structuresMessage = new StringBuilder();
            structuresMessage.append("<div style='margin-left: 10px;'>");
            while (structureIds.hasNext()) {
                String structureId = structureIds.next();
                StringBuilder structureMessage = new StringBuilder("<br><span> Structure '" + (structureMap.containsKey(structureId) ? structureMap.getJsonObject(structureId).getString("name", structureId) : structureId) + "':</span><br>");
                JsonObject structure = structures.getJsonObject(structureId);
                String errorStructure = structure.getString("errorMessage", null);


                if (errorStructure != null) {
                    structureMessage.append("<span style='margin-left: 10px;'> Aucun appel n'a été créé sur cette structure: ").append(errorStructure).append("<br>");
                } else {
                    structuresMessage.append("<ul>");

                    Integer structureCoursesNumber = structure.getInteger("succeededCoursesNumber", 0);

                    structureMessage.append("<li> Nombre d'appels créés: ").append(structureCoursesNumber).append("</li><br>");
                    succeededCoursesNumber += structureCoursesNumber;
                    JsonObject coursesErrors = structure.getJsonObject("coursesErrors");
                    Set<String> coursesIds = coursesErrors.fieldNames();

                    for (String courseId : coursesIds) {
                        String errorMessage = coursesErrors.getString(courseId);
                        String courseMessage = "<li> Cours '" + courseId + "': Pas d'appel créé: " + errorMessage + "</li>";
                        structureMessage.append(courseMessage);
                    }
                    structuresMessage.append("</ul><br>");
                }
                structuresMessage.append(structureMessage);
            }
            structuresMessage.append("</div>");
            message.append("<span>Total appels créés: ").append(succeededCoursesNumber).append(".</span><br><br>");
            message.append(structuresMessage);
        }

        return message.toString();
    }

    private void sendMail(String title, String message) {
        JsonArray listMails = config.getJsonArray("mails-list-cron", new JsonArray());

        if (listMails.isEmpty()) {
            log.info(message);
        } else {
            for (Object o : listMails) {
                String mail = (String) o;
                emailSender.sendEmail(
                        null,
                        mail,
                        null,
                        null,
                        title,
                        message,
                        null,
                        false,
                        event -> {
                            if (event.failed()) {
                                log.error("[Presence@DailyRegistersCreation] Failed to send mail", event.cause());
                            } else if ("error".equals(event.result().body().getString("status"))) {
                                log.error("[Presence@DailyRegistersCreation] Failed to send mail", event.result().body().getString("message", ""));
                            }
                        });
            }
        }
    }

}
