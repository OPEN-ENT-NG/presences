package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.service.ExemptionService;
import fr.openent.presences.service.impl.ExemptionServiceImpl;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.rs.Delete;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.response.DefaultResponseHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;


public class ExemptionController extends ControllerHelper {
    private ExemptionService exemptionService;
    private EventBus eb;

    public ExemptionController(EventBus eb) {
        super();
        this.exemptionService = new ExemptionServiceImpl(eb);
        this.eb = eb;
    }

    @Get("/exemptions")
    public void getExemptions(final HttpServerRequest request) {
        if (!request.params().contains("structure_id") || !request.params().contains("start_date") || !request.params().contains("end_date")) {
            badRequest(request);
            return;
        }
        String structure_id = String.valueOf(request.getParam("structure_id"));
        String start_date = String.valueOf(request.getParam("start_date"));
        String end_date = String.valueOf(request.getParam("end_date"));
        List<String> student_ids = request.params().contains("student_id") ? new ArrayList<String>(Arrays.asList(request.getParam("student_id").split("\\s*,\\s*"))) : null;
        List<String> audience_ids = request.params().contains("audience_id") ? new ArrayList<String>(Arrays.asList(request.getParam("audience_id").split("\\s*,\\s*"))) : null;

        if (audience_ids != null && !audience_ids.isEmpty() && audience_ids.size() > 0) {

            JsonObject action = new JsonObject()
                    .put("action", "user.getElevesRelatives")
                    .put("idsClass", audience_ids);

            eb.send(Presences.ebViescoAddress, action, handlerToAsyncHandler((Handler<Message<JsonObject>>) message -> {
                JsonObject body = message.body();
                if ("ok".equals(body.getString("status"))) {
                    JsonArray stuent_ids_fromClasses = body.getJsonArray("results");
                    for (int i = 0; i < stuent_ids_fromClasses.size(); i++) {
                        JsonObject student = stuent_ids_fromClasses.getJsonObject(i);
                        student_ids.add(student.getString("idNeo4j"));
                    }
                    exemptionService.get(structure_id, start_date, end_date, student_ids, DefaultResponseHandler.arrayResponseHandler(request));

                } else {
                    JsonObject error = new JsonObject()
                            .put("error", body.getString("message"));
                    Renders.renderJson(request, error, 400);
                }
            }));
        } else {
            exemptionService.get(structure_id, start_date, end_date, student_ids, DefaultResponseHandler.arrayResponseHandler(request));

        }
    }

    @Post("/exemptions")
    public void createExemptions(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "exemption", exemptions -> {
            exemptionService.create(
                    exemptions.getString("structure_id"),
                    exemptions.getJsonArray("student_id"),
                    exemptions.getString("subject_id"),
                    exemptions.getString("start_date"),
                    exemptions.getString("end_date"),
                    exemptions.getBoolean("attendance"),
                    exemptions.getString("comment"),
                    DefaultResponseHandler.arrayResponseHandler(request));
        });
    }

    @Put("/exemption/:id")
    public void updateExemption(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }

        RequestUtils.bodyToJson(request, pathPrefix + "exemption", exemption -> {
            exemptionService.update(
                    request.getParam("id"),
                    exemption.getString("structure_id"),
                    exemption.getString("student_id"),
                    exemption.getString("subject_id"),
                    exemption.getString("start_date"),
                    exemption.getString("end_date"),
                    exemption.getBoolean("attendance"),
                    exemption.getString("comment"),
                    DefaultResponseHandler.defaultResponseHandler(request));
        });
    }

    @Delete("/exemption")
    public void deleteExemption(final HttpServerRequest request) {
        List<String> exemption_ids = request.params().contains("id") ? Arrays.asList(request.getParam("id").split("\\s*,\\s*")) : null;
        exemptionService.delete(exemption_ids, DefaultResponseHandler.arrayResponseHandler(request));
    }
}