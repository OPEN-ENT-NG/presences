package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.*;
import fr.openent.presences.security.*;
import fr.openent.presences.service.SearchService;
import fr.openent.presences.service.impl.DefaultSearchService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class SearchController extends ControllerHelper {

    private final EventBus eb;
    private final GroupService groupService;
    private final SearchService searchService;
    private final UserService userService;


    public SearchController(EventBus eb) {
        super();
        this.eb = eb;
        this.groupService = new DefaultGroupService(eb);
        this.searchService = new DefaultSearchService(eb);
        this.userService = new DefaultUserService();
    }

    @Get("/search/users")
    @ApiDoc("Search for users")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SearchRight.class)
    public void searchForUsers(HttpServerRequest request) {
        if (request.params().contains(Field.Q) && !"".equals(request.params().get(Field.Q).trim())
                && request.params().contains(Field.FIELD)
                && request.params().contains(Field.PROFILE)
                && request.params().contains(Field.STRUCTUREID)) {

            UserUtils.getUserInfos(eb, request, user -> {
                boolean hasRestrictedRight = WorkflowActionsCouple.SEARCH.hasOnlyRestrictedRight(user, UserType.TEACHER.equals(user.getType()));
                String restrictedTeacherId = hasRestrictedRight ? user.getUserId() : null;

                String query = request.getParam(Field.Q);
                List<String> fields = request.params().getAll(Field.FIELD);
                String profile = request.getParam(Field.PROFILE);
                String structureId = request.getParam(Field.STRUCTUREID);

                JsonObject action = new JsonObject()
                        .put("action", "user.search")
                        .put(Field.Q, query)
                        .put(Field.FIELDS, new JsonArray(fields))
                        .put(Field.PROFILE, profile)
                        .put(Field.STRUCTUREID, structureId)
                        .put(Field.USERID, restrictedTeacherId);

                callViescolaireEventBus(action, request);

            });

        } else {
            badRequest(request);
        }
    }

    @Get("/search/students")
    @ApiDoc("Search for students")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SearchStudents.class)
    public void searchStudents(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            boolean hasRight = WorkflowActionsCouple.SEARCH.hasOnlyRestrictedRight(user, UserType.TEACHER.equals(user.getType()));
            String userId = hasRight ? user.getUserId() : null;
        if (request.params().contains(Field.Q) && !"".equals(request.params().get(Field.Q).trim())
                && request.params().contains(Field.FIELD)
                && request.params().contains(Field.STRUCTUREID)) {
            String query = request.getParam(Field.Q);
            List<String> fields = request.params().getAll(Field.FIELD);
            String structureId = request.getParam(Field.STRUCTUREID);

            JsonObject action = new JsonObject()
                    .put("action", "user.search")
                    .put(Field.Q, query)
                    .put(Field.FIELDS, new JsonArray(fields))
                    .put(Field.PROFILE, UserType.STUDENT)
                    .put(Field.STRUCTUREID, structureId)
                    .put(Field.USERID, userId);
            callViescolaireEventBus(action, request);
        } else {
            badRequest(request);
        }});
    }

    @Get("/search/groups")
    @ApiDoc("Search for groups")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SearchStudents.class)
    public void searchGroups(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            boolean hasRestrictedRight = WorkflowActionsCouple.SEARCH.hasOnlyRestrictedRight(user, UserType.TEACHER.equals(user.getType()));
            String userId = hasRestrictedRight ? user.getUserId() : null;
            if (request.params().contains(Field.Q)
                    && !"".equals(request.params().get(Field.Q).trim())
                    && request.params().contains(Field.FIELD)
                    && request.params().contains(Field.STRUCTUREID)) {
                String query = request.getParam(Field.Q);
                List<String> fields = request.params().getAll(Field.FIELD);
                String structureId = request.getParam(Field.STRUCTUREID);

                searchService.searchGroups(query, fields, structureId, userId, arrayResponseHandler(request));
            } else {
                badRequest(request);
            }
        });
    }

    @Get("/search")
    @ApiDoc("Search for a student or a group")
    @SecuredAction(Presences.SEARCH_STUDENTS)
    public void search(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            boolean hasRestrictedRight = WorkflowActionsCouple.SEARCH_STUDENTS.hasOnlyRestrictedRight(user, UserType.TEACHER.equals(user.getType()));
            String userId = hasRestrictedRight ? user.getUserId() : null;

            if (request.params().contains(Field.Q) && !"".equals(request.params().get(Field.Q).trim())
                    && request.params().contains(Field.STRUCTUREID)) {
                searchService.search(request.getParam(Field.Q), request.getParam(Field.STRUCTUREID),
                        userId, arrayResponseHandler(request));
            }
        });
    }

    @Get("/users")
    @ApiDoc("get students based on group id fetched")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SearchStudents.class)
    public void getStudentsFromGroupId(HttpServerRequest request) {
        if (request.params().contains("groupId") && request.params().contains("type")) {
            GroupType type = "CLASS".equals(request.getParam("type")) ? GroupType.CLASS : GroupType.GROUP;
            groupService.getGroupUsers(request.getParam("groupId"), type, arrayResponseHandler(request));
        }
    }

    @Get("/children")
    @ApiDoc("get children from relative")
    @SecuredAction(Presences.READ_CHILDREN)
    public void getChildren(HttpServerRequest request) {
        if (request.params().contains("relativeId")) {
            userService.getChildren(request.getParam("relativeId"), arrayResponseHandler(request));
        } else {
            log.error("[Presences@SearchController::getChildren] Failed to get children, probably no relativeId given");
            badRequest(request);
        }
    }

    @Get("/user/:id/child")
    @ApiDoc("get info as user info")
    @SecuredAction(Presences.READ_OWN_INFO)
    public void getChildUser(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains("id") && user.getUserId().equals(request.getParam("id"))) {
                userService.getChildInfo(request.getParam("id"), defaultResponseHandler(request));
            } else {
                log.error("[Presences@SearchController::getChildUser] Failed to get child own info, " +
                        "probably no id given or no match with user id");
                badRequest(request);
            }
        });
    }

    private void callViescolaireEventBus(JsonObject action, HttpServerRequest request) {
        eb.send("viescolaire", action, handlerToAsyncHandler(event -> {
            JsonObject body = event.body();
            if (!"ok".equals(body.getString("status"))) {
                log.error("[Presences@SearchController::callViescolaireEventBus] An error has occured while using viescolaire eb");
                renderError(request);
                return;
            }
            renderJson(request, body.getJsonArray("results"));
        }));
    }


    @Get("/rights/search/users")
    @SecuredAction(Presences.SEARCH)
    public void searchUsers(HttpServerRequest request) {
        request.response().setStatusCode(501).end();
    }

}
