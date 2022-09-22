package fr.openent.massmailing.controller;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.actions.*;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.security.CanAccessMassMailing;
import fr.openent.massmailing.service.MailingService;
import fr.openent.massmailing.service.impl.DefaultMailingService;
import fr.openent.presences.common.helper.*;
import fr.openent.presences.common.service.*;
import fr.openent.presences.common.service.impl.*;
import fr.openent.presences.core.constants.*;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.*;

import java.util.List;
import java.util.stream.*;


public class MailingController extends ControllerHelper {
    private final MailingService mailingService;
    private final GroupService groupService;
    private final UserService userService;
    private final Storage storage;

    public MailingController(EventBus eb, Storage storage) {
        this.mailingService = new DefaultMailingService();
        this.groupService = new DefaultGroupService(eb);
        this.userService = new DefaultUserService();
        this.storage = storage;
    }

    @Get("/mailings")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CanAccessMassMailing.class)
    @ApiDoc("Get mailings")
    @SuppressWarnings("unchecked")
    public void getMailings(HttpServerRequest request) {
        MultiMap params = request.params();
        List<String> mailingTypes = params.getAll(Field.MAILTYPE);

        if (!isValidParams(request) || (!mailingTypes.isEmpty() && isEachMailingTypeValid(mailingTypes))) {
            badRequest(request);
            return;
        }

        String structureId = params.get(Field.STRUCTURE);
        String startDate = params.get(Field.START);
        String endDate = params.get(Field.END);
        List<String> eventTypes = params.getAll(Field.TYPE);
        List<String> studentsIds = params.getAll(Field.STUDENT);
        List<String> groupsIds = params.getAll(Field.GROUP);
        Integer page = Integer.parseInt(params.get(Field.PAGE));


        UserUtils.getUserInfos(eb, request, userInfos -> {
            boolean hasRestrictedRight = WorkflowActionsCouple.VIEW.hasOnlyRestrictedRight(userInfos, UserType.TEACHER.equals(userInfos.getType()));
            String teacherId = hasRestrictedRight ? userInfos.getUserId() : null;

            Future<List<String>> studentsFromTeacherFuture = this.userService.getStudentsFromTeacher(teacherId, structureId);
            Future<JsonArray> groupStudentsFuture = this.groupService.getGroupStudents(groupsIds);

            CompositeFuture.all(studentsFromTeacherFuture, groupStudentsFuture)
                    .onFailure(fail -> renderError(request))
                    .onSuccess(evt -> {

                        List<String> restrictedStudentIds = studentsFromTeacherFuture.result();

                        /* Add student id fetched from group to our studentsIds list */
                        JsonArray studentIdsFromGroup = groupStudentsFuture.result();

                        ((List<JsonObject>) studentIdsFromGroup.getList())
                                .forEach(o -> {
                                    if (!studentsIds.contains(o.getString(Field.ID))) {
                                        studentsIds.add(o.getString(Field.ID, ""));
                                    }
                                });

                        List<String> studentIdsList;

                        if (studentsIds.isEmpty()) {
                            studentIdsList = restrictedStudentIds;
                        } else {
                            studentIdsList = restrictedStudentIds.isEmpty() ? studentsIds :
                                    studentsIds.stream().filter(restrictedStudentIds::contains)
                                            .collect(Collectors.toList());
                        }


                        Promise<JsonArray> mailingPromise = Promise.promise();
                        Promise<JsonObject> mailingPagePromise = Promise.promise();

                        CompositeFuture.all(mailingPromise.future(), mailingPagePromise.future())
                                .onFailure(fail -> renderError(request, JsonObject.mapFrom(fail.getCause())))
                                .onSuccess(event -> {

                                    boolean restrictedHasNoStudents = !restrictedStudentIds.isEmpty() && studentIdsList.isEmpty();

                                    int pageCount = mailingPagePromise.future().result()
                                            .getInteger(Field.COUNT) <= Massmailing.PAGE_SIZE ? 0
                                            : (int) Math.ceil((double) mailingPagePromise.future().result()
                                            .getInteger(Field.COUNT) / (double) Massmailing.PAGE_SIZE);


                                    JsonObject mailingsResult = new JsonObject()
                                            .put(Field.PAGE, restrictedHasNoStudents ? 0 : page)
                                            .put(Field.PAGE_COUNT, restrictedHasNoStudents ? 0 : pageCount)
                                            .put(Field.ALL, restrictedHasNoStudents ? new JsonArray() :
                                                    mailingPromise.future().result());

                                    renderJson(request, mailingsResult);
                                });

                        mailingService.getMailings(structureId, startDate, endDate, mailingTypes, eventTypes, studentIdsList,
                                page, FutureHelper.handlerJsonArray(mailingPromise));
                        mailingService.getMailingsPageNumber(structureId, startDate, endDate, mailingTypes, eventTypes, studentIdsList,
                                FutureHelper.handlerJsonObject(mailingPagePromise));


                    });
        });
    }

    private boolean isValidParams(HttpServerRequest request) {
        MultiMap params = request.params();
        return params.contains(Field.STRUCTURE) &&
                params.contains(Field.START) &&
                params.contains(Field.END) &&
                params.contains(Field.PAGE);
    }

    private boolean isEachMailingTypeValid(List<String> mailingTypes) {
        for (String mailingType : mailingTypes) {
            if (!mailingType.equals(MailingType.MAIL.toString()) ||
                    !mailingType.equals(MailingType.PDF.toString()) ||
                    !mailingType.equals(MailingType.SMS.toString())) {
                return false;
            }
        }
        return true;
    }


    @Get("/mailings/:idMailing/file/:id")
    @ApiDoc("Get file when mailing is pdf type")
    @ResourceFilter(CanAccessMassMailing.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getFileMailing(HttpServerRequest request) {
        mailingService.getMailing(
                request.getParam("structure"),
                Long.parseLong(request.getParam("idMailing")),
                request.getParam("id"),
                result -> {
                    if (result.failed()) {
                        renderError(request);
                        return;
                    }

                    JsonObject mailing = result.result();
                    JsonObject metadata = mailing.getString("metadata") != null ? new JsonObject(mailing.getString("metadata")) : null;
                    storage.sendFile(mailing.getString("file_id"), metadata != null ? metadata.getString("filename") : "", request, false, metadata);
                });
    }
}