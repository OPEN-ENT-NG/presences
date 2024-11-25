package fr.openent.presences.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.entcore.common.pdf.Pdf;

import java.util.List;

public interface ExportAbsenceService {

    /**
     * Get events. Will after export this into csv (or PDF)
     *
     * @param domain      domain request sent
     * @param local       accepted langage
     * @param structureId Structure identifier
     * @param teacherId   teacher identifier
     * @param audienceIds audience ids to filter
     * @param studentIds  student ids to filter
     * @param reasonIds   reason ids to filter
     * @param startAt     start date from which we need retrieve data
     * @param endAt       end date until which we need retrieve data
     * @param regularized filter on regularized data (or not)
     * @param followed    filter on followed data (or not)
     * @param halfBoarder filter on half boarder students (or not)
     * @param internal    filter on internal students (or not)
     * @return {@link Pdf} pdf with list of absences
     */
    Future<Pdf> generatePdf(String domain, String local, String structureId, String teacherId,
                            List<String> audienceIds, List<String> studentIds, List<Integer> reasonIds,
                            String startAt, String endAt, Boolean regularized, Boolean noReason,
                            Boolean followed, Boolean halfBoarder, Boolean internal);


    /**
     * @param domain      domain request sent
     * @param local       accepted langage
     * @param structureId Structure identifier
     * @param teacherId   teacher identifier
     * @param audienceIds audience ids to filter
     * @param studentIds  student ids to filter
     * @param reasonIds   reason ids to filter
     * @param startAt     start date from which we need retrieve data
     * @param endAt       end date until which we need retrieve data
     * @param regularized filter on regularized data (or not)
     * @param followed    filter on followed data (or not)
     * @param halfBoarder filter on half boarder students (or not)
     * @param internal    filter on internal students (or not)
     * @return JsonObject containing JsonArray ABSENCE with absences to export
     */
    Future<JsonObject> getAbsencesData(String domain, String local, String structureId, String teacherId,
                                       List<String> audienceIds, List<String> studentIds, List<Integer> reasonIds,
                                       String startAt, String endAt, Boolean regularized, Boolean noReason,
                                       Boolean followed, Boolean halfBoarder, Boolean internal);
}
