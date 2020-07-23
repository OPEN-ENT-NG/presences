package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.security.AbsenceStatementsCreateRight;
import fr.openent.presences.security.AbsenceStatementsGetFileRight;
import fr.openent.presences.security.AbsenceStatementsViewRight;
import fr.openent.presences.service.StatementAbsenceService;
import fr.openent.presences.service.impl.DefaultStatementAbsenceService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserUtils;

import java.text.ParseException;
import java.util.Date;

public class StatementAbsenceController extends ControllerHelper {

    private StatementAbsenceService statementAbsenceService;
    private EventBus eb;
    private Storage storage;

    public StatementAbsenceController(EventBus eventBus, Storage storage) {
        super();
        this.statementAbsenceService = new DefaultStatementAbsenceService(storage);
        this.storage = storage;
        this.eb = eventBus;
    }


    @Get("/statements/absences")
    @ApiDoc("Get statement absences")
    @ResourceFilter(AbsenceStatementsViewRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            statementAbsenceService.get(user, request.params(), result -> {
                if (result.failed()) {
                    renderError(request);
                    return;
                }
                renderJson(request, result.result());
            });
        });
    }


    @Post("/statements/absences")
    @ApiDoc("Create statement absence")
    @ResourceFilter(AbsenceStatementsCreateRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void create(HttpServerRequest request) {
        request.setExpectMultipart(true);
        UserUtils.getUserInfos(eb, request, user -> {
            request.endHandler(resultHandler -> {
                if (!(user.getChildrenIds().contains(request.getFormAttribute("student_id")))) {
                    unauthorized(request);
                }
                saveAbsenceStatement(request, null);
            });
        });
    }

    @Post("/statements/absences/attachment")
    @ApiDoc("Create statement absence with an attachment")
    @ResourceFilter(AbsenceStatementsCreateRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void createWithFile(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            storage.writeUploadFile(request, resultUpload -> {
                if (!"ok".equals(resultUpload.getString("status"))) {
                    String message = "[Presences@DefaultStatementAbsenceController:createWithFile] Failed to save file.";
                    log.error(message + " " + resultUpload.getString("message"));
                    renderError(request);
                    return;
                }

                String file_id = resultUpload.getString("_id");
                if (!(user.getChildrenIds().contains(request.getFormAttribute("student_id")))) {
                    deleteFile(file_id);
                    unauthorized(request);
                }

                saveAbsenceStatement(request, file_id);
            });
        });
    }

    @Put("/statements/absences/:id/validate")
    @ApiDoc("Validate statement absence")
    @SecuredAction(Presences.MANAGE_ABSENCE_STATEMENTS)
    @Trace(Actions.ABSENCE_STATEMENT_VALIDATE)
    public void validate(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, body -> {
            UserUtils.getUserInfos(eb, request, user -> {
                body.put("id", request.params().get("id"));
                statementAbsenceService.validate(user, body, result -> {
                    if (result.failed()) {
                        renderError(request);
                        return;
                    }
                    renderJson(request, result.result());
                });
            });
        });
    }

    @Get("/statements/absences/:idStatement/attachment/:id")
    @ApiDoc("Get statement absence file")
    @ResourceFilter(AbsenceStatementsGetFileRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getFile(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            statementAbsenceService.getFile(user, request.params(), resultFile -> {
                if (resultFile.failed()) {
                    notFound(request);
                    return;
                }

                JsonObject result = resultFile.result();
                String studentName = result.getJsonObject("student").getString("name").replace(" ", "_");
                String createdAt = DateHelper.getDateString(result.getString("created_at"), DateHelper.YEAR_MONTH_DAY);
                String name = "Declaration_" + studentName + (createdAt.equals("") ? "" : "_" + createdAt);
                storage.sendFile(request.getParam("id"), name, request, false, new JsonObject());
            });
        });
    }


    /**
     * Delete file from storage based on identifier
     *
     * @param fileId File identifier to delete
     */
    private void deleteFile(String fileId) {
        storage.removeFile(fileId, e -> {
            if (!"ok".equals(e.getString("status"))) {
                log.error("[Presences@DefaultStatementAbsenceService:deleteFile] An error occurred while removing " + fileId + " file.");
            }
        });
    }

    private void saveAbsenceStatement(HttpServerRequest request, String file_id) {
        JsonObject body = new JsonObject();
        body.put("attachment_id", file_id);

        request.formAttributes().entries().forEach(entry -> {
            body.put(entry.getKey(), entry.getValue());
        });

        statementAbsenceService.create(body, request, result -> {
            if (result.failed()) {
                if (file_id != null) deleteFile(file_id);
                renderError(request);
                return;
            }
            renderJson(request, result.result());
        });
    }
}
