package fr.openent.incidents.worker;

import fr.openent.incidents.export.*;
import fr.openent.incidents.service.*;
import fr.openent.presences.common.export.*;
import fr.openent.presences.common.helper.*;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.model.*;
import io.vertx.core.*;
import io.vertx.core.json.*;
import org.entcore.common.user.*;

import java.util.*;

public class IncidentsExportWorker extends ExportWorker {
    private IncidentsService incidentsService;
    private PunishmentService punishmentService;

    @Override
    protected String getFolderName() {
        return ExportFolderName.INCIDENTS;
    }

    protected Future<Void> init(JsonObject config) {
        Promise<Void> promise = Promise.promise();
        CommonIncidentsServiceFactory commonPresencesServiceFactory = new CommonIncidentsServiceFactory(vertx);
        incidentsService = commonPresencesServiceFactory.incidentsService();
        punishmentService = commonPresencesServiceFactory.punishmentService();
        promise.complete();
        return promise.future();
    }

    @Override
    protected Future<ExportFile> getData(String action, String exportType, JsonObject params) {
        switch (action) {
            case ExportActions.EXPORT_INCIDENTS:
                this.exportNotification = "presences.export_incidents";
                return getIncidents(params);
            case ExportActions.EXPORT_PUNISHMENTS:
                this.exportNotification = "incidents.export_punishments";
                return getPunishments(params);
            default:
                String message = String.format("[Incidents@%s::getData] invalid action %s", this.getClass().getName(), action);
                log.error(message);
                return Future.failedFuture(message);
        }
    }

    @SuppressWarnings("unchecked")
    private Future<ExportFile> getIncidents(JsonObject params) {
        Promise<ExportFile> promise = Promise.promise();
        String local = params.getString(Field.LOCALE);
        String domain = params.getString(Field.DOMAIN);

        String structureId = params.getString(Field.STRUCTUREID);
        String startDate = params.getString(Field.STARTDATE);
        String endDate = params.getString(Field.ENDDATE);
        List<String> studentIds = params.getJsonArray(Field.STUDENTIDS).getList();
        String field = params.getString(Field.FIELD);
        boolean reverse = params.getBoolean(Field.REVERSE);

        incidentsService.get(structureId, startDate, endDate, studentIds,
                null, false, field, reverse, event -> {
                    if (event.isLeft()) {
                        String message = String.format("[Incidents@%s::exportIncidents] Failed to fetch incidents",
                                this.getClass().getSimpleName());
                        log.error(message, event.left().getValue());
                        exportLogs.addLog(message);
                        promise.fail(message);
                    } else {
                        JsonArray incidents = event.right().getValue();
                        List<String> csvHeaders = new ArrayList<>(Arrays.asList(
                                "incidents.csv.header.date", "incidents.csv.header.place",
                                "incidents.csv.header.type", "incidents.csv.header.description",
                                "incidents.csv.header.seriousness", "incidents.csv.header.protagonists",
                                "incidents.csv.header.partner", "incidents.csv.header.processed"));
                        IncidentsCSVExport ice = new IncidentsCSVExport(incidents, domain, local);
                        ice.setHeader(csvHeaders, domain, local);
                        promise.complete(ice.getExportFile(domain, local));
                    }
                });

        return promise.future();
    }

    @SuppressWarnings("unchecked")
    private Future<ExportFile> getPunishments(JsonObject params) {
        Promise<ExportFile> promise = Promise.promise();
        String locale = params.getString(Field.LOCALE);
        String domain = params.getString(Field.DOMAIN);

        String id = params.getString(Field.ID);
        String structureId = params.getString(Field.STRUCTURE_ID);
        String startAt = params.getString(Field.START_AT);
        String endAt = params.getString(Field.END_AT);
        List<String> studentIds = params.getJsonArray(Field.STUDENT_ID).getList();
        List<String> groupIds = params.getJsonArray(Field.GROUP_ID).getList();
        List<String> typeIds = params.getJsonArray(Field.TYPE_ID).getList();
        List<String> processStates = params.getJsonArray(Field.PROCESS).getList();
        String order = params.getString(Field.ORDER) != null ?  params.getString(Field.ORDER) : Field.DATE;
        boolean reverse = params.getString(Field.REVERSE) != null && Boolean.parseBoolean(params.getString(Field.REVERSE));

        UserInfos user = UserInfosHelper.getUserInfosFromJSON(params.getJsonObject(Field.USER));

        punishmentService.get(user, id, null, structureId, startAt, endAt, studentIds, groupIds, typeIds, processStates,
                false, order, reverse, null, null, null)
                .onFailure(fail -> {
                    String message = String.format("[Incidents@%s::getPunishments] Failed to fetch punishments",
                            this.getClass().getSimpleName());
                    log.error(message, fail);
                    exportLogs.addLog(message);
                })
                .onSuccess(res -> {
                    JsonArray punishments = res.getJsonArray(Field.ALL);
                    List<String> csvHeaders = new ArrayList<>(Arrays.asList(
                            "incidents.punishments.csv.header.student.lastName",
                            "incidents.punishments.csv.header.student.firstName",
                            "incidents.punishments.csv.header.classname",
                            "incidents.punishments.csv.header.type",
                            "incidents.punishments.csv.header.start.date",
                            "incidents.punishments.csv.header.end.date",
                            "incidents.punishments.csv.header.slots",
                            "incidents.punishments.csv.header.description",
                            "incidents.punishments.csv.header.owner",
                            "incidents.punishments.csv.header.processed"));
                    PunishmentsCSVExport pce = new PunishmentsCSVExport(punishments, domain, locale);
                    pce.setHeader(csvHeaders, domain, locale);
                    promise.complete(pce.getExportFile(domain, locale));
                });

        return promise.future();
    }
}
