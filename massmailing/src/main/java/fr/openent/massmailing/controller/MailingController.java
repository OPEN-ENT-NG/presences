package fr.openent.massmailing.controller;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.security.CanAccessMassMailing;
import fr.openent.massmailing.service.MailingService;
import fr.openent.massmailing.service.impl.DefaultMailingService;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.util.List;


public class MailingController extends ControllerHelper {
    private MailingService mailingService;
    private GroupService groupService;

    public MailingController(EventBus eb) {
        this.mailingService = new DefaultMailingService();
        this.groupService = new DefaultGroupService(eb);
    }

    @Get("/mailings")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CanAccessMassMailing.class)
    @ApiDoc("Get mailings")
    public void getMailings(HttpServerRequest request) {
        if (!isValidParams(request)) {
            badRequest(request);
            return;
        }

        MultiMap params = request.params();

        String structureId = params.get("structure");
        String startDate = params.get("start");
        String endDate = params.get("end");
        List<String> mailingTypes = params.getAll("mailType");
        List<String> eventTypes = params.getAll("type");
        List<String> studentsIds = params.getAll("student");
        List<String> groupsIds = params.getAll("group");
        Integer page = Integer.parseInt(params.get("page"));

        if (structureId == null || (!mailingTypes.isEmpty() && isEachMailingTypeValid(mailingTypes))) {
            badRequest(request);
            return;
        }

        this.groupService.getGroupStudents(groupsIds, groupAsync -> {
            if (groupAsync.isLeft()) {
                String message = "[Massmailing@MailingController] Failed to retrieve students for mailings";
                log.error(message);
                renderError(request, JsonObject.mapFrom(message + " " + groupAsync.left().getValue()));
                return;
            }

            /* Add student id fetched from group to our studentsIds list */
            JsonArray studentIdsFromGroup = groupAsync.right().getValue();
            for (int i = 0; i < studentIdsFromGroup.size(); i++) {
                JsonObject o = studentIdsFromGroup.getJsonObject(i);
                if (!studentsIds.contains(o.getString("id", ""))) {
                    studentsIds.add(o.getString("id", ""));
                }
            }
            Future<JsonArray> mailingFuture = Future.future();
            Future<JsonObject> mailingPageFuture = Future.future();

            CompositeFuture.all(mailingFuture, mailingPageFuture).setHandler(mailingAsync -> {
                if (mailingAsync.failed()) {
                    renderError(request, JsonObject.mapFrom(mailingAsync.cause()));
                } else {
                    Integer pageCount = mailingPageFuture.result().getInteger("count") <= Massmailing.PAGE_SIZE ? 0
                            : (int) Math.ceil((double) mailingPageFuture.result().getInteger("count") / (double) Massmailing.PAGE_SIZE);
                    JsonObject mailingsResult = new JsonObject()
                            .put("page", page)
                            .put("page_count", pageCount)
                            .put("all", mailingFuture.result());

                    renderJson(request, mailingsResult);
                }
            });

            mailingService.getMailings(structureId, startDate, endDate, mailingTypes, eventTypes, studentsIds,
                    page, FutureHelper.handlerJsonArray(mailingFuture));
            mailingService.getMailingsPageNumber(structureId, startDate, endDate, mailingTypes, eventTypes, studentsIds,
                    FutureHelper.handlerJsonObject(mailingPageFuture));
        });
    }

    private Boolean isValidParams(HttpServerRequest request) {
        MultiMap params = request.params();
        return params.contains("structure") &&
                params.contains("start") &&
                params.contains("end") &&
                params.contains("page");
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
}