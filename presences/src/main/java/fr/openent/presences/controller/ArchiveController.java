package fr.openent.presences.controller;

import fr.openent.presences.common.service.StructureService;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.service.ArchiveService;
import fr.openent.presences.service.CommonPresencesServiceFactory;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;

import java.util.List;
import java.util.stream.Collectors;

public class ArchiveController extends ControllerHelper {

    private final StructureService structureService;
    private final ArchiveService archiveService;

    public ArchiveController(CommonPresencesServiceFactory commonPresencesServiceFactory) {
        super();
        this.structureService = commonPresencesServiceFactory.structureService();
        this.archiveService = commonPresencesServiceFactory.archiveService();
    }

    @Get("/event/archives/export")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdminFilter.class)
    @ApiDoc("Generate event archives")
    public void archiveEventsExport(final HttpServerRequest request) {
        MultiMap params = request.params();
        List<String> structureIds = params.getAll("structureId");

        if (structureIds.isEmpty()) {
            badRequest(request);
        } else {
            getActiveStructures(structureIds, false)
                    .compose(structureService::fetchStructuresInfos)
                    .compose(structures -> archiveService.processEventExportWorker(structures,
                            Renders.getHost(request),
                            I18n.acceptLanguage(request)))
                    .onSuccess(res -> renderJson(request, res))
                    .onFailure(err -> badRequest(request));
        }
    }

    /**
     * fetch all structures and filter current structures fetched from http params to make sure we will use truth structure identifier
     * (as long as optionFetchAllStructures is not set as true)
     *
     * @param structuresIds             list of structure identifier fetched from {@link MultiMap params's}
     * @param optionFetchAllStructures  if this option is set true, will not take structureIds into account and return all structures actives
     *                                  setting this option false will simply filter structureids
     *                                  BE CAUTIOUS while setting this parameter as true
     *
     * @return {@link Future} of {@link List<String>}
     */
    @SuppressWarnings("unchecked")
    private Future<List<String>> getActiveStructures(List<String> structuresIds, Boolean optionFetchAllStructures) {
        Promise<List<String>> promise = Promise.promise();
        this.structureService.fetchActiveStructure()
                .onSuccess(structures -> {
                    List<String> filteredStructure = structuresIds.stream()
                            .filter(structureParam -> ((List<JsonObject>) structures.getList())
                                    .stream()
                                    .anyMatch(sActive -> sActive.getString(Field.ID).equals(structureParam)))
                            .collect(Collectors.toList());

                    promise.complete(Boolean.TRUE.equals(optionFetchAllStructures) ?
                            ((List<JsonObject>) structures.getList()).stream().map(s -> s.getString(Field.ID)).collect(Collectors.toList()) :
                            filteredStructure);
                })
                .onFailure(promise::fail);

        return promise.future();
    }

}
