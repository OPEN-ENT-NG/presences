package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.export.ExemptionCSVExport;
import fr.openent.presences.model.Exemption.ExemptionBody;
import fr.openent.presences.security.ExportRight;
import fr.openent.presences.security.ManageExemptionRight;
import fr.openent.presences.service.ExemptionService;
import fr.openent.presences.service.impl.DefaultExemptionService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.http.response.DefaultResponseHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ExemptionController extends ControllerHelper {
    private ExemptionService exemptionService;
    private GroupService groupService;
    private EventBus eb;

    public ExemptionController(EventBus eb) {
        super();
        this.exemptionService = new DefaultExemptionService(eb);
        this.groupService = new DefaultGroupService(eb);
        this.eb = eb;
    }

    @Get("/exemptions")
    @ApiDoc("Retrieve exemptions")
    @SecuredAction(Presences.READ_EXEMPTION)
    public void getExemptions(final HttpServerRequest request) {
        if (!request.params().contains("structure_id") || !request.params().contains("start_date") || !request.params().contains("end_date") || !request.params().contains("page")) {
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
        if (!request.params().contains("structure_id") || !request.params().contains("start_date") || !request.params().contains("end_date")) {
            badRequest(request);
            return;
        }
        getExemptionsORCreateCSV(request, true);
    }

    private void getExemptionsORCreateCSV(HttpServerRequest request, boolean wantCSV) {
        //Manage pagination
        String page = request.getParam("page");

        //get usefull data to get
        String structure_id = String.valueOf(request.getParam("structure_id"));
        String start_date = String.valueOf(request.getParam("start_date"));
        String end_date = String.valueOf(request.getParam("end_date"));
        List<String> student_ids = request.params().contains("student_id") ? new ArrayList<String>(Arrays.asList(request.getParam("student_id").split("\\s*,\\s*"))) : new ArrayList<String>();
        List<String> audience_ids = request.params().contains("audience_id") ? new ArrayList<String>(Arrays.asList(request.getParam("audience_id").split("\\s*,\\s*"))) : null;

        //get class's users
        if (audience_ids != null && !audience_ids.isEmpty() && audience_ids.size() > 0) {
            groupService.getGroupStudents(audience_ids, audiences -> {
                if (audiences.isLeft()) {
                    log.error("[Presences@ExemptionController] Failed to retrieve student identifiers based on audiences identifiers", audiences.left().getValue());
                    renderError(request);
                    return;
                }

                JsonArray students = audiences.right().getValue();
                ((List<JsonObject>) students.getList()).forEach(student -> student_ids.add(student.getString("id")));
                if (wantCSV) {
                    csvResponse(request, structure_id, start_date, end_date, student_ids);
                } else {
                    paginateResponse(request, page, structure_id, start_date, end_date, student_ids);

                }
            });
        } else {
            if (wantCSV) {
                csvResponse(request, structure_id, start_date, end_date, student_ids);
            } else {
                paginateResponse(request, page, structure_id, start_date, end_date, student_ids);
            }
        }
    }

    private void csvResponse(HttpServerRequest request, String structure_id, String start_date, String end_date, List<String> student_ids) {
        String field = request.params().contains("order") ? request.getParam("order") : "date";
        boolean reverse = request.params().contains("reverse") && Boolean.parseBoolean(request.getParam("reverse"));

        exemptionService.get(structure_id, start_date, end_date, student_ids, null, field, reverse, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                JsonArray exemptions = event.right().getValue();
                List<String> csvHeaders = Arrays.asList(
                        "presences.exemptions.csv.header.student.firstName",
                        "presences.exemptions.csv.header.student.lastName",
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
            }
        });
    }


    private void paginateResponse(final HttpServerRequest request, String page, String structure_id, String start_date, String end_date, List<String> student_ids) {
        String field = request.params().contains("order") ? request.getParam("order") : "date";
        boolean reverse = request.params().contains("reverse") && Boolean.parseBoolean(request.getParam("reverse"));

        Future<JsonArray> exemptionsFuture = Future.future();
        Future<JsonObject> pageNumberFuture = Future.future();

        CompositeFuture.all(exemptionsFuture, pageNumberFuture).setHandler(event -> {
            if (event.failed()) {
                renderError(request, JsonObject.mapFrom(event.cause()));
            } else {
                JsonObject res = new JsonObject()
                        .put("page", Integer.parseInt(page))
                        .put("page_count", pageNumberFuture.result().getLong("count") / Presences.PAGE_SIZE)
                        .put("values", exemptionsFuture.result());

                renderJson(request, res);
            }
        });

        exemptionService.get(structure_id, start_date, end_date, student_ids, page, field, reverse, FutureHelper.handlerJsonArray(exemptionsFuture));
        exemptionService.getPageNumber(structure_id, start_date, end_date, student_ids, field, reverse, FutureHelper.handlerJsonObject(pageNumberFuture));
    }

    @Post("/exemptions")
    @ApiDoc("Create given exemptions")
    @SecuredAction(Presences.MANAGE_EXEMPTION)
    @Trace(Actions.EXEMPTION_CREATION)
    public void createExemptions(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "exemption", exemptions -> {
            ExemptionBody exemptionBody = new ExemptionBody(exemptions);
            exemptionService.create(exemptionBody, DefaultResponseHandler.arrayResponseHandler(request));
        });
    }

    @Put("/exemption/:id")
    @ApiDoc("Update given exemption")
    @ResourceFilter(ManageExemptionRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.EXEMPTION_UPDATE)
    public void updateExemption(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }

        RequestUtils.bodyToJson(request, pathPrefix + "exemption", exemption -> {
            ExemptionBody exemptionBody = new ExemptionBody(exemption);
            Integer id = Integer.parseInt(request.params().get("id"));
            exemptionService.update(id, exemptionBody, DefaultResponseHandler.defaultResponseHandler(request));
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