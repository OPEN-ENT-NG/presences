package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.*;
import fr.openent.presences.common.service.*;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.*;
import fr.openent.presences.export.StatementAbsencesCSVExport;
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
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserUtils;

import java.util.*;
import java.util.stream.Collectors;

public class StatementAbsenceController extends ControllerHelper {

    private final StatementAbsenceService statementAbsenceService;
    private final EventBus eb;
    private final Storage storage;
    private final UserService userService;

    public StatementAbsenceController(EventBus eventBus, Storage storage) {
        super();
        this.statementAbsenceService = new DefaultStatementAbsenceService(storage);
        this.storage = storage;
        this.eb = eventBus;
        this.userService = new DefaultUserService();
    }


    @Get("/statements/absences")
    @ApiDoc("Get statement absences")
    @ResourceFilter(AbsenceStatementsViewRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            String structureId = request.getParam(Field.STRUCTURE_ID);

            boolean hasRestrictedRight = WorkflowActionsCouple.MANAGE_ABSENCE_STATEMENTS.hasOnlyRestrictedRight(user, UserType.TEACHER.equals(user.getType()));
            String teacherId = hasRestrictedRight ? user.getUserId() : null;

            this.userService.getStudentsFromTeacher(teacherId, structureId)
                    .onFailure(fail -> renderError(request))
                    .onSuccess(studentIds ->
                            statementAbsenceService.get(user, request.params(), studentIds, result -> {
                                if (result.failed()) {
                                    renderError(request);
                                    return;
                                }
                                renderJson(request, result.result());
                            }));
        });
    }

    @Get("/statements/absences/export")
    @ApiDoc("Export statement absences")
    @ResourceFilter(AbsenceStatementsViewRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void export(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            String structureId = request.getParam(Field.STRUCTURE_ID);

            boolean hasRestrictedRight = WorkflowActionsCouple.MANAGE_ABSENCE_STATEMENTS.hasOnlyRestrictedRight(user, UserType.TEACHER.equals(user.getType()));
            String teacherId = hasRestrictedRight ? user.getUserId() : null;

            this.userService.getStudentsFromTeacher(teacherId, structureId)
                    .onFailure(fail -> renderError(request))
                    .onSuccess(studentIds ->
                            statementAbsenceService.get(user, request.params(), studentIds, result -> {
                                if (result.failed()) {
                                    renderError(request);
                                    return;
                                }

                                setParent(result.result().getJsonArray(Field.ALL), listResult -> {
                                    JsonArray statementAbsences = listResult.result();
                                    List<String> csvHeaders = new ArrayList<>(Arrays.asList(
                                            "presences.csv.header.student.lastName", "presences.csv.header.student.firstName",
                                            "presences.csv.header.parent.lastName", "presences.csv.header.parent.firstName",
                                            "presences.statements.absence.csv.header.start.at.date", "presences.statements.absence.csv.header.start.at.hour",
                                            "presences.statements.absence.csv.header.end.at.date", "presences.statements.absence.csv.header.end.at.hour",
                                            "presences.statements.absence.csv.header.description",
                                            "presences.statements.absence.csv.header.treated.at.date", "presences.statements.absence.csv.header.treated.at.hour",
                                            "presences.statements.absence.csv.header.created.at.date", "presences.statements.absence.csv.header.created.at.hour"));
                                    StatementAbsencesCSVExport csv = new StatementAbsencesCSVExport(statementAbsences);
                                    csv.setRequest(request);
                                    csv.setHeader(csvHeaders);
                                    csv.export();
                                });
                            }));
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
                saveAbsenceStatement(request, null, null, user.getUserId());
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
                String metadata = resultUpload.getJsonObject("metadata").toString();
                if (!(user.getChildrenIds().contains(request.getFormAttribute("student_id")))) {
                    deleteFile(file_id);
                    unauthorized(request);
                }

                saveAbsenceStatement(request, file_id, metadata, user.getUserId());
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
                JsonObject metadata = result.getString("metadata") != null ? new JsonObject(result.getString("metadata")) : new JsonObject();
                storage.sendFile(request.getParam("id"), !metadata.isEmpty() ? name : "", request, false, metadata);
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

    private void saveAbsenceStatement(HttpServerRequest request, String file_id, String metadata, String parent_id) {
        JsonObject body = new JsonObject();
        body.put("attachment_id", file_id);
        body.put("metadata", metadata);
        body.put("parent_id", parent_id);

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


    private void setParent(JsonArray dataList, Handler<AsyncResult<JsonArray>> handler) {
        List<String> parentIds = ((List<JsonObject>) dataList.getList())
                .stream()
                .map(res -> res.getString("parent_id"))
                .collect(Collectors.toList());

        userService.getUsers(parentIds, resUsers -> {
            if (resUsers.isLeft()) {
                String message = "[Presences@StatementAbsenceController::setParent] Failed to get parents";
                log.error(message);
                handler.handle(Future.failedFuture(message));
                return;
            }

            Map<String, JsonObject> parentMap = new HashMap<>();
            resUsers.right().getValue().forEach(oParent -> {
                JsonObject parent = (JsonObject) oParent;
                parentMap.put(parent.getString("id"), parent);
            });

            dataList.forEach(oRes -> {
                JsonObject res = (JsonObject) oRes;
                res.put("parent", parentMap.get(res.getString("parent_id")));
                res.remove("parent_id");
            });

            handler.handle(Future.succeededFuture(dataList));
        });
    }

}
