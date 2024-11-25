package fr.openent.presences.worker;

import fr.openent.presences.common.export.*;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.*;
import fr.openent.presences.model.*;
import fr.openent.presences.service.*;
import io.vertx.core.*;
import io.vertx.core.json.*;
import org.entcore.common.storage.*;

import java.util.*;

public class PresencesExportWorker extends ExportWorker {
    private ExportEventService exportEventService;

   @Override
   protected String getFolderName() {
       return ExportFolderName.PRESENCES;
   }

    protected Future<Void> init(JsonObject config) {
        Promise<Void> promise = Promise.promise();
        Storage storage = new StorageFactory(vertx, new JsonObject()).getStorage();
        CommonPresencesServiceFactory commonPresencesServiceFactory = new CommonPresencesServiceFactory(vertx,
                storage, config);
        exportEventService = commonPresencesServiceFactory.exportEventService();
        promise.complete();
        return promise.future();
    }

    @Override
    protected Future<ExportFile> getData(String action, String exportType, JsonObject params) {
        switch(action) {
            case ExportActions.EXPORT_EVENTS:
                this.exportNotification = "presences.export_events";
                return getEvents(exportType, params);
            default:
                String message = String.format("[Presences@%s::getData] invalid action %s", this.getClass().getName(), action);
                log.error(message);
                return Future.failedFuture(message);
        }
    }

    @SuppressWarnings("unchecked")
    private Future<ExportFile> getEvents(String type, JsonObject params) {
        Promise<ExportFile> promise = Promise.promise();
        String local = params.getString(Field.LOCALE);
        String domain = params.getString(Field.DOMAIN);

        String structureId = params.getString(Field.STRUCTUREID);
        String startDate = params.getString(Field.STARTDATE);
        String endDate = params.getString(Field.ENDDATE);
        List<String> eventType = params.getJsonArray(Field.EVENTTYPE, new JsonArray()).getList();
        List<String> reasonIds = params.getJsonArray(Field.REASONIDS, new JsonArray()).getList();
        Boolean noReason = params.getBoolean(Field.NOREASON);
        Boolean noReasonLateness = params.getBoolean(Field.NOREASONLATENESS);
        List<String> userIds = params.getJsonArray(Field.USERIDS, new JsonArray()).getList();
        JsonArray userIdFromClasses = params.getJsonArray(Field.USERIDFROMCLASSES, new JsonArray());
        List<String> classes = params.getJsonArray(Field.CLASSES, new JsonArray()).getList();
        List<String> restrictedClasses = params.getJsonArray(Field.RESTRICTEDCLASSES, new JsonArray()).getList();
        Boolean regularized = params.getBoolean(Field.REGULARIZED);
        Boolean followed = params.getBoolean(Field.FOLLOWED);
        Boolean canSeeAllStudent = params.getBoolean(Field.CANSEEALLSTUDENT);

        if (ExportType.CSV.type().equals(type)) {
            exportEventService.getCsvData(structureId, startDate, endDate, eventType, reasonIds, noReason, noReasonLateness, userIds, userIdFromClasses,
                    classes, restrictedClasses, regularized, followed, event ->
                            exportEventService.processCsvEvent(domain, local, event)
                                    .onFailure(fail -> {
                                        this.exportLogs.addLog(fail.getMessage());
                                        promise.fail(fail.getMessage());
                                    })
                                    .onSuccess(promise::complete));

        } else if (ExportType.PDF.type().equals(type)) {
            exportEventService.getPdfData(canSeeAllStudent, domain, local, structureId, startDate, endDate, eventType, reasonIds,
                            noReason, noReasonLateness, userIds, userIdFromClasses, regularized, followed)
                    .compose(v -> exportEventService.processPdfEvent(v))
                    .onSuccess(res -> {
                        ExportFile exportFile = new ExportFile(res.getContent(), "application/pdf; charset=utf-8", res.getName());
                        promise.complete(exportFile);
                    })
                    .onFailure(err -> {
                        String message = "An error has occurred during export pdf process";
                        String logMessage = String.format("[Presences@%s::getEvents] %s : %s",
                                this.getClass().getSimpleName(), message, err.getMessage());
                        log.error(logMessage);
                        this.exportLogs.addLog(logMessage);
                        promise.fail(err.getMessage());
                    });
        }

        return promise.future();
    }

}
