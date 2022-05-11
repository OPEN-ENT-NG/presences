package fr.openent.presences.service.impl;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.service.ExportPDFService;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DBService;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.helper.ReasonHelper;
import fr.openent.presences.model.Reason;
import fr.openent.presences.service.*;
import fr.wseduc.webutils.I18n;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.pdf.Pdf;

import java.util.*;

public class DefaultExportAbsenceService extends DBService implements ExportAbsenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExportAbsenceService.class);
    private final AbsenceService absenceService;
    private final ReasonService reasonService;
    private final ExportPDFService exportPDFService;

    public DefaultExportAbsenceService(CommonPresencesServiceFactory commonPresencesServiceFactory) {
        this.absenceService = commonPresencesServiceFactory.absenceService();
        this.reasonService = commonPresencesServiceFactory.reasonService();
        this.exportPDFService = commonPresencesServiceFactory.exportPDFService();
    }

    @Override
    public Future<Pdf> generatePdf(String domain, String local, String structureId, String teacherId,
                                   List<String> audienceIds, List<String> studentIds, List<Integer> reasonIds,
                                   String startAt, String endAt, Boolean regularized, Boolean noReason,
                                   Boolean followed, Boolean halfBoarder, Boolean internal) {
        Promise<Pdf> promise = Promise.promise();

        getAbsencesData(domain, local, structureId, teacherId,
                audienceIds, studentIds, reasonIds,
                startAt, endAt, regularized, noReason, followed, halfBoarder, internal)
                .compose(absences -> exportPDFService.generatePDF(Field.EXPORT_PDF_ABSENCES + "_" + startAt + "_" + endAt,
                        "pdf/absence-list-recap.xhtml", absences))
                .onSuccess(promise::complete)
                .onFailure(err -> {
                    String message = String.format("[Presences@%s::getPdfData]: An error has occurred " +
                            "during absences pdf generation", this.getClass().getSimpleName());
                    LOGGER.error(String.format("%s: %s", message, err.getMessage()));
                    promise.fail(message);
                });

        return promise.future();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Future<JsonObject> getAbsencesData(String domain, String local, String structureId, String teacherId,
                                              List<String> audienceIds, List<String> studentIds, List<Integer> reasonIds,
                                              String startAt, String endAt, Boolean regularized, Boolean noReason,
                                              Boolean followed, Boolean halfBoarder, Boolean internal) {
        Promise<JsonObject> promise = Promise.promise();

        Future<JsonArray> reasonsFuture = reasonService.fetchReason(structureId, EventType.ABSENCE.getType());
        Future<JsonObject> absencesFuture = absenceService
                .get(structureId, teacherId, audienceIds, studentIds, reasonIds,
                        startAt, endAt, regularized, noReason, followed, halfBoarder, internal, null);

        CompositeFuture.all(reasonsFuture, absencesFuture)
                .onSuccess(res -> {
                    List<Reason> reasons = ReasonHelper.getReasonListFromJsonArray(reasonsFuture.result(), Reason.MANDATORY_ATTRIBUTE);
                    promise.complete(mapAbsencesToExportData(domain, local, absencesFuture.result()
                            .getJsonArray(Field.ALL, new JsonArray()).getList(), reasons));
                })
                .onFailure(err -> {
                    String message = String.format("[Presences@%s::getAbsencesData]: An error has occurred " +
                            "during absences data recovery", this.getClass().getSimpleName());
                    LOGGER.error(String.format("%s: %s", message, err.getMessage()));
                    promise.fail(message);
                });

        return promise.future();
    }

    private JsonObject mapAbsencesToExportData(String domain, String local, List<JsonObject> absences, List<Reason> reasons) {
        absences.forEach(absence -> {
            absence.put(Field.DATE, DateHelper.getDateString(absence.getString(Field.START_DATE), DateHelper.YEAR_MONTH_DAY));
            absence.put(Field.DISPLAY_START_DATE, DateHelper.getDateString(absence.getString(Field.START_DATE), DateHelper.HOUR_MINUTES));
            absence.put(Field.DISPLAY_END_DATE, DateHelper.getDateString(absence.getString(Field.END_DATE), DateHelper.HOUR_MINUTES));

            Reason reason = reasons.stream()
                    .filter(currentReason -> currentReason.getId() != null &&
                            currentReason.getId().equals(absence.getInteger(Field.REASON_ID)))
                    .findFirst()
                    .orElse(null);

            String reasonLabel = reason != null ? reason.getLabel() :
                    "presences.memento.absence.type.NO_REASON";
            absence.put(Field.REASON, I18n.getInstance().translate(reasonLabel, domain, local));

            absence.put(Field.REGULARIZED,
                    Boolean.TRUE.equals(absence.getBoolean(Field.COUNSELLOR_REGULARISATION)) ?
                            I18n.getInstance().translate("presences.exemptions.csv.attendance.true", domain, local) :
                            I18n.getInstance().translate("presences.exemptions.csv.attendance.false", domain, local));
        });

        return new JsonObject().put(EventType.ABSENCE.name(), absences);
    }

}