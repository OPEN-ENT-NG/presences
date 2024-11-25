package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.*;
import fr.openent.presences.common.service.*;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.*;
import fr.openent.presences.export.ExemptionCSVExport;
import fr.openent.presences.model.Exemption.ExemptionBody;
import fr.openent.presences.security.ExportRight;
import fr.openent.presences.security.ManageExemptionRight;
import fr.openent.presences.service.*;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.user.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.*;

public class ExemptionController extends ControllerHelper {
    private final ExemptionService exemptionService;
    private final GroupService groupService;
    private final UserService userService;

    public ExemptionController(CommonPresencesServiceFactory commonPresencesServiceFactory) {
        super();
        this.eb = commonPresencesServiceFactory.eventBus();
        this.exemptionService = commonPresencesServiceFactory.exemptionService();
        this.groupService = commonPresencesServiceFactory.groupService();
        this.userService = commonPresencesServiceFactory.userService();
    }

    @Get("/exemptions")
    @ApiDoc("Retrieve exemptions")
    @SecuredAction(Presences.READ_EXEMPTION)
    public void getExemptions(final HttpServerRequest request) {
        if (!request.params().contains(Field.STRUCTURE_ID) || !request.params().contains(Field.START_DATE)
                || !request.params().contains(Field.END_DATE) || !request.params().contains(Field.PAGE)) {
            badRequest(request);
            return;
        }
        getExemptionsORCreateCSV(request, false);
    }

    @Get("/exemptions/export")
    @ApiDoc("Export exemptions")
    @ResourceFilter(ExportRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void exportExemptions(HttpServerRequest request) {
        if (!request.params().contains(Field.STRUCTURE_ID) || !request.params().contains(Field.START_DATE)
                || !request.params().contains(Field.END_DATE)) {
            badRequest(request);
            return;
        }

        getExemptionsORCreateCSV(request, true);
    }

    @SuppressWarnings("unchecked")
    private void getExemptionsORCreateCSV(HttpServerRequest request, boolean wantCSV) {
        //Manage pagination
        String page = request.getParam(Field.PAGE);

        //get useful data to get
        String structureId = String.valueOf(request.getParam(Field.STRUCTURE_ID));
        String startDate = String.valueOf(request.getParam(Field.START_DATE));
        String endDate = String.valueOf(request.getParam(Field.END_DATE));
        List<String> studentIds = request.params().contains(Field.STUDENT_ID)
                ? new ArrayList<>(Arrays.asList(request.getParam(Field.STUDENT_ID).split("\\s*,\\s*"))) : new ArrayList<>();
        List<String> audienceIds = request.params().contains(Field.AUDIENCE_ID)
                ? new ArrayList<>(Arrays.asList(request.getParam(Field.AUDIENCE_ID).split("\\s*,\\s*"))) : null;

        UserUtils.getUserInfos(eb, request, user -> {
            boolean hasRestrictedRight = WorkflowActionsCouple.READ_EXEMPTION.hasOnlyRestrictedRight(user, UserType.TEACHER.equals(user.getType()));
            String restrictedTeacherId = hasRestrictedRight ? user.getUserId() : null;

            //get class's users
            this.userService.getStudentsFromTeacher(restrictedTeacherId, structureId)
                    .onFailure(fail -> {
                        String message = String.format("[Presences@%s::getExemptionsORCreateCSV] Failed to retrieve " +
                                "restricted teacher students", this.getClass().getSimpleName());
                        log.error(message, fail.getMessage());
                        renderError(request);
                    })
                    .onSuccess(restrictedStudentIds-> {

                        if (audienceIds != null && !audienceIds.isEmpty()) {

                            groupService.getGroupStudents(audienceIds)
                                    .onFailure(fail -> {
                                        String message = String.format("[Presences@%s::getExemptionsORCreateCSV] Failed to retrieve student " +
                                                "identifiers based on audiences identifiers", this.getClass().getSimpleName());
                                        log.error(message, fail.getMessage());
                                        renderError(request);
                                    })
                                    .onSuccess(students -> {
                                        ((List<JsonObject>) students.getList())
                                                .forEach(student -> {
                                                    studentIds.add(student.getString(Field.ID));
                                                });

                                        List<String> studentIdList = studentIds;

                                        if (restrictedTeacherId != null) {
                                            if (studentIds.isEmpty()) {
                                                studentIdList = restrictedStudentIds;
                                            } else {
                                                studentIdList = studentIds.stream().filter(restrictedStudentIds::contains).collect(Collectors.toList());
                                            }
                                        }


                                        if (wantCSV) {
                                            csvResponse(request, structureId, startDate, endDate,
                                                    (restrictedStudentIds != null && studentIdList.isEmpty()) ? null : studentIdList);
                                        } else {
                                            paginateResponse(request, page, structureId, startDate, endDate,
                                                    (restrictedStudentIds != null && studentIdList.isEmpty()) ? null : studentIdList);
                                        }
                                    });
                        } else {

                            List<String> studentIdList = studentIds;

                            if (restrictedTeacherId != null) {
                                if (studentIds.isEmpty()) {
                                    studentIdList = restrictedStudentIds;
                                } else {
                                    studentIdList = studentIds.stream().filter(restrictedStudentIds::contains).collect(Collectors.toList());
                                }
                            }

                            if (wantCSV) {
                                csvResponse(request, structureId, startDate, endDate,
                                        (restrictedTeacherId != null && studentIdList.isEmpty()) ? null : studentIdList);
                            } else {
                                paginateResponse(request, page, structureId, startDate, endDate,
                                        (restrictedTeacherId != null && studentIdList.isEmpty()) ? null : studentIdList);
                            }
                        }
                    });
        });
    }

    private void csvResponse(HttpServerRequest request, String structureId, String startDate,
                             String endDate, List<String> studentIds) {
        String field = request.params().contains(Field.ORDER) ? request.getParam(Field.ORDER) : Field.DATE;
        boolean reverse = request.params().contains(Field.REVERSE) && Boolean.parseBoolean(request.getParam(Field.REVERSE));

        exemptionService.get(structureId, startDate, endDate, studentIds, null, field, reverse, event -> {

                JsonArray exemptions = event.right().getValue();
                List<String> csvHeaders = Arrays.asList(
                        "presences.csv.header.student.firstName",
                        "presences.csv.header.student.lastName",
                        "presences.exemptions.csv.header.audiance",
                        "presences.exemptions.csv.header.subject",
                        "presences.exemptions.csv.header.startDate",
                        "presences.exemptions.csv.header.endDate",
                        "presences.exemptions.csv.header.comment",
                        "presences.exemptions.csv.header.attendance");
                ExemptionCSVExport ecs = new ExemptionCSVExport(exemptions);
                ecs.setRequest(request);
                ecs.setHeader(csvHeaders);
                ecs.export();
            });
    }


    private void paginateResponse(final HttpServerRequest request, String page, String structureId, String startDate,
                                  String endDate, List<String> studentIds) {
        String field = request.params().contains(Field.ORDER) ? request.getParam(Field.ORDER) : Field.DATE;
        boolean reverse = request.params().contains(Field.REVERSE)
                && Boolean.parseBoolean(request.getParam(Field.REVERSE));

        Promise<JsonArray> exemptionsPromise = Promise.promise();
        Promise<JsonObject> pageNumberPromise = Promise.promise();

        CompositeFuture.all(exemptionsPromise.future(), pageNumberPromise.future())
                .onFailure(fail -> renderError(request, JsonObject.mapFrom(fail)))
                .onSuccess(event -> {

                    JsonObject res = new JsonObject()
                            .put(Field.PAGE, Integer.parseInt(page))
                            .put(Field.PAGE_COUNT, pageNumberPromise.future().result().getLong(Field.COUNT) / Presences.PAGE_SIZE)
                            .put(Field.VALUES, exemptionsPromise.future().result());

                    // For restricted teachers with no results
                    if (studentIds == null) {
                        res = new JsonObject()
                                .put(Field.PAGE, 0)
                                .put(Field.PAGE_COUNT, 0)
                                .put(Field.VALUES, new JsonArray());
                    }

                    renderJson(request, res);
                });

        exemptionService.get(structureId, startDate, endDate, studentIds, page, field, reverse, FutureHelper.handlerJsonArray(exemptionsPromise));
        exemptionService.getPageNumber(structureId, startDate, endDate, studentIds, field, reverse, FutureHelper.handlerJsonObject(pageNumberPromise));
    }

    @Post("/exemptions")
    @ApiDoc("Create given exemptions")
    @SecuredAction(Presences.MANAGE_EXEMPTION)
    @Trace(Actions.EXEMPTION_CREATION)
    public void createExemptions(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "exemption", exemptions ->
                UserUtils.getUserInfos(eb, request, user -> {
                    boolean hasRestrictedRight = WorkflowActionsCouple.MANAGE_EXEMPTION.hasOnlyRestrictedRight(user, UserType.TEACHER.equals(user.getType()));
                    String restrictedTeacherId = hasRestrictedRight ? user.getUserId() : null;

                    ExemptionBody exemptionBody = new ExemptionBody(exemptions);

                    this.userService.getStudentsFromTeacher(restrictedTeacherId, exemptionBody.getStructureId())
                            .onFailure(fail -> {
                                String message = String.format("[Presences@%s::createExemptions] Failed to retrieve " +
                                        "restricted teacher students", this.getClass().getSimpleName());
                                log.error(message, fail.getMessage());
                                renderError(request);
                            })
                            .onSuccess(restrictedStudentIds -> {
                                boolean hasUnallowedStudentIds = restrictedTeacherId != null
                                        && exemptionBody.getListStudentId().stream().noneMatch(restrictedStudentIds::contains);

                                if (hasUnallowedStudentIds) {
                                    renderError(request);
                                } else {
                                    exemptionService.create(exemptionBody, DefaultResponseHandler.arrayResponseHandler(request));
                                }
                            });
                }));
    }

    @Put("/exemption/:id")
    @ApiDoc("Update given exemption")
    @ResourceFilter(ManageExemptionRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.EXEMPTION_UPDATE)
    public void updateExemption(final HttpServerRequest request) {
        if (!request.params().contains(Field.ID)) {
            badRequest(request);
            return;
        }

        RequestUtils.bodyToJson(request, pathPrefix + "exemption", exemption -> {

            UserUtils.getUserInfos(eb, request, user -> {
                boolean hasRestrictedRight = WorkflowActionsCouple.MANAGE_EXEMPTION.hasOnlyRestrictedRight(user, UserType.TEACHER.equals(user.getType()));
                String restrictedTeacherId = hasRestrictedRight ? user.getUserId() : null;

                ExemptionBody exemptionBody = new ExemptionBody(exemption);
                Integer id = Integer.parseInt(request.params().get(Field.ID));


                this.userService.getStudentsFromTeacher(restrictedTeacherId, exemptionBody.getStructureId())
                        .onFailure(fail -> {
                            String message = String.format("[Presences@%s::updateExemption] Failed to retrieve " +
                                    "restricted teacher students", this.getClass().getSimpleName());
                            log.error(message, fail.getMessage());
                            renderError(request);
                        })
                        .onSuccess(restrictedStudentIds -> {
                            boolean hasUnallowedStudentIds = restrictedTeacherId != null
                                    && exemptionBody.getListStudentId().stream().anyMatch(restrictedStudentIds::contains);

                            if (hasUnallowedStudentIds) {
                                renderError(request);
                            } else {
                                exemptionService.update(id, exemptionBody, DefaultResponseHandler.defaultResponseHandler(request));
                            }
                        });
            });
        });
    }

    @Delete("/exemption")
    @ApiDoc("Update given exemption")
    @ResourceFilter(ManageExemptionRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.EXEMPTION_DELETION)
    public void deleteExemption(final HttpServerRequest request) {
        List<String> exemption_ids = request.params().contains("id") ? Arrays.asList(request.getParam("id").split("\\s*,\\s*")) : null;
        exemptionService.delete(exemption_ids, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Delete("/exemption/recursive")
    @ApiDoc("Update given exemption")
    @ResourceFilter(ManageExemptionRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.EXEMPTION_DELETION)
    public void deleteRecursiveExemption(final HttpServerRequest request) {
        List<String> exemption_ids = request.params().contains("id") ? Arrays.asList(request.getParam("id").split("\\s*,\\s*")) : null;
        exemptionService.deleteRecursive(exemption_ids, DefaultResponseHandler.arrayResponseHandler(request));
    }
}