package fr.openent.presences.worker;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.model.Course;
import fr.openent.presences.service.CourseService;
import fr.openent.presences.service.RegisterService;
import fr.openent.presences.service.impl.DefaultCourseService;
import fr.openent.presences.service.impl.DefaultRegisterService;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
import org.entcore.common.user.UserUtils;
import org.vertx.java.busmods.BusModBase;

import java.util.*;

public class CreateDailyPresenceWorker extends BusModBase implements Handler<Message<JsonObject>> {
    private EmailSender emailSender;
    private CourseService courseService;
    private RegisterService registerService;
    private final Logger log = LoggerFactory.getLogger(CreateDailyPresenceWorker.class);


    @Override
    public void start() {
        super.start();
        this.courseService = new DefaultCourseService(eb);
        this.registerService = new DefaultRegisterService(eb);
        this.emailSender = new EmailFactory(vertx, config).getSender();
        eb.consumer(this.getClass().getName(), this::handle);
    }

    @Override
    public void handle(Message<JsonObject> eventMessage) {
        eventMessage.reply(new JsonObject().put("status", "ok"));
        logger.info("Calling Worker");
        processCreateDailyPresences();
    }

    private void processCreateDailyPresences() {
        String yesterday = DateHelper.getYesterday();

        String queryStructures = "SELECT id_etablissement as id FROM " + Presences.dbSchema + " .etablissements_actifs";

        JsonObject result = new JsonObject();
        // TODO create future
        List<Future> futures = new ArrayList<>();
        Sql.getInstance().prepared(queryStructures, new JsonArray(), SqlResult.validResultHandler(resultStructures -> {
            if (resultStructures.isLeft()) {
                Future<JsonObject> future = Future.future();
                futures.add(future);
                log.error(resultStructures.left().getValue());
                result.put("errorMessage", "Structures recovery failed");
                future.complete();
            } else {
                // Tableau Structures
                result.put("structures", new JsonObject());
                JsonArray structureIds = resultStructures.right().getValue();
                for (int i = 0; i < structureIds.size(); i++) {
                    String structureId = structureIds.getJsonObject(i).getString("id");
                    Future<JsonObject> future = Future.future();
                    futures.add(future);
                    createStructureCoursesRegister(structureId, yesterday, yesterday, resultCreations -> {
                        try {
                            if (resultCreations.succeeded()) {
                                result.getJsonObject("structures").put(structureId, resultCreations.result());
                            } else {
                                result.getJsonObject("structures").put("errorMessage", resultCreations.cause().getMessage());
                            }
                            future.complete();
                        } catch (Error e) {
                            log.error(e.getMessage());
                            future.fail(e.getMessage());
                        }
                    });
                }
            }

            CompositeFuture.join(futures).setHandler(resultFutures -> {
                String title = "[" + config.getString("host") + "][Présences] Rapport d'ouverture des appels: " + (getRegistersCreationWorked(result) ? "succès." : "échec.");
                String message = getFormattedMessage(result);
                sendMail(title, message);
            });

        }));
    }

    private void createStructureCoursesRegister(String structureId, String startDate, String endDate, Handler<AsyncResult<JsonObject>> handler) {
        List<Future> futures = new ArrayList<>();
        courseService.listCourses(
                structureId, new ArrayList<>(), new ArrayList<>(),
                startDate, endDate, true, true, null, resultCourses -> {
                    if (resultCourses.isLeft()) {
                        log.error(resultCourses.left().getValue());
                        handler.handle(Future.failedFuture("Courses recovery failed: " + resultCourses.left().getValue()));
                    } else {
                        JsonObject result = new JsonObject()
                                .put("succeededCoursesNumber", 0)
                                .put("coursesErrors", new JsonObject());

                        List<Course> courses = resultCourses.right().getValue().getList();
                        for (Course course : courses) {
                            JsonArray teachers = course.getTeachers();
                            Integer registerId = course.getRegisterId();
                            if (registerId != null) {
                                continue;
                            }

                            JsonObject register = new JsonObject()
                                    .put("start_date", course.getStartDate())
                                    .put("end_date", course.getEndDate())
                                    .put("subject_id", course.getSubjectId())
                                    .put("structure_id", structureId)
                                    .put("course_id", course.getId())
                                    .put("split_slot", true)
                                    .put("groups", course.getGroups())
                                    .put("classes", course.getClasses());

                            Future<JsonObject> future = Future.future();
                            futures.add(future);
                            if (teachers.size() < 1) {
                                getFirstCounsellorId(structureId, resultCounsellor -> {
                                    try {
                                        if (resultCounsellor.succeeded()) {
                                            createRegister(resultCounsellor.result(), register, resultRegister -> {
                                                if (resultRegister.failed()) {
                                                    log.error(resultRegister.cause().getMessage());
                                                    result.getJsonObject("coursesErrors").put(course.getId(), resultRegister.cause().getMessage());
                                                } else {
                                                    result.put("succeededCoursesNumber", result.getInteger("succeededCoursesNumber") + 1);
                                                }
                                            });
                                        } else {
                                            log.error(resultCounsellor.cause().getMessage());
                                            result.getJsonObject("coursesErrors").put(course.getId(), resultCounsellor.cause().getMessage());
                                        }
                                        future.complete();
                                    } catch (Error e) {
                                        log.error(e.getMessage());
                                        future.fail(e.getMessage());
                                    }
                                });
                            } else {
                                createRegister(teachers.getJsonObject(0).getString("id"), register, resultRegister -> {
                                    try {
                                        if (resultRegister.failed()) {
                                            log.error(resultRegister.cause().getMessage());
                                            result.getJsonObject("coursesErrors").put(course.getId(), resultRegister.cause().getMessage());
                                        } else {
                                            result.put("succeededCoursesNumber", result.getInteger("succeededCoursesNumber") + 1);
                                        }
                                        future.complete();
                                    } catch (Error e) {
                                        log.error(e.getMessage());
                                        future.fail(e.getMessage());
                                    }
                                });
                            }
                        }
                        CompositeFuture.join(futures).setHandler(resultFutures -> {
                            if (resultFutures.succeeded()) {
                                handler.handle(Future.succeededFuture(result));
                            } else {
                                handler.handle(Future.failedFuture(resultFutures.cause().getMessage()));
                            }
                        });
                    }
                }
        );
    }

    private void getFirstCounsellorId(String structureId, Handler<AsyncResult<String>> handler) {
        String queryCounsellor = "MATCH (u:User)-[:IN]->(g:ProfileGroup)-[:DEPENDS]->(s:Structure {id:{structureId}}) WHERE ANY(function IN u.functions WHERE function =~ '.*EDU\\\\$EDUCATION\\\\$E0030.*') RETURN u";

        Neo4j.getInstance().execute(queryCounsellor, new JsonObject().put("structureId", structureId), Neo4jResult.validResultHandler(resultCounsellor -> {
            if (resultCounsellor.isRight()) {
                JsonArray counsellors = resultCounsellor.right().getValue();
                if (counsellors.size() > 0) {
                    handler.handle(Future.succeededFuture(counsellors.getJsonObject(0).getString("id")));
                } else {
                    handler.handle(Future.failedFuture("No counsellor found on this structure"));
                }
            } else {
                handler.handle(Future.failedFuture(resultCounsellor.left().getValue()));
            }
        }));
    }

    private void createRegister(String userId, JsonObject register, Handler<AsyncResult<Boolean>> handler) {
        if (userId == null || userId.trim().equals("")) {
            handler.handle(Future.failedFuture("No user found to assign register"));
            return;
        }

        UserUtils.getUserInfos(eb, userId, user -> {
            registerService.create(register, user, resultRegister -> {
                if (resultRegister.isLeft()) {
                    handler.handle(Future.failedFuture(resultRegister.left().toString()));
                } else {
                    handler.handle(Future.succeededFuture(true));
                }
            });
        });
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

            Iterator<String> structureIds = structures.fieldNames().iterator();
            while (structureIds.hasNext()) {
                String structureId = structureIds.next();
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

    private String getFormattedMessage(JsonObject workerResult) {
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
                StringBuilder structureMessage = new StringBuilder("<br><span> Structure '" + structureId + "':</span><br>");
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
