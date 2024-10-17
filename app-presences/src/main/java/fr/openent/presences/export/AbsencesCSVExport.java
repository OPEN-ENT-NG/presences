package fr.openent.presences.export;

import fr.openent.presences.common.helper.CSVExport;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.Absence;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AbsencesCSVExport extends CSVExport {

    private final List<Absence> absences;

    public AbsencesCSVExport(List<JsonObject> absences, HttpServerRequest request) {
        super();
        setRequest(request);
        this.absences = absences.stream().map(currentAbsence -> {
                    Absence absence = new Absence(currentAbsence, new ArrayList<>());
                    absence.getReason().setLabel(currentAbsence.getString(Field.REASON, ""));
                    return absence;
        }).collect(Collectors.toList());
        this.filename = "presences.absences.csv.filename";
        setHeader(Arrays.asList(
                "presences.csv.header.student.lastName",
                "presences.csv.header.student.firstName",
                "presences.exemptions.csv.header.audiance",
                "presences.absence.reason",
                "presences.created.by",
                "presences.registry.csv.header.start.date",
                "presences.registry.csv.header.end.date",
                "presences.widgets.absences.regularized",
                "presences.id"));
    }

    @Override
    public void generate() {
        for (Absence absence : absences) {
            this.value.append(getLine(absence));
        }
    }

    private String getLine(Absence absence) {
        String line = absence.getStudent().getLastName() + SEPARATOR;
        line += absence.getStudent().getFirstName() + SEPARATOR;
        line += absence.getStudent().getClassName() + SEPARATOR;
        line += absence.getReason().getLabel() + SEPARATOR;
        line += absence.getOwner().getName() + SEPARATOR;
        line += DateHelper.getDateString(absence.getStartDate(), DateHelper.DAY_MONTH_YEAR_HOUR_MINUTES) + SEPARATOR;
        line += DateHelper.getDateString(absence.getEndDate(), DateHelper.DAY_MONTH_YEAR_HOUR_MINUTES) + SEPARATOR;
        line += absence.getRegularized() + SEPARATOR;
        line += absence.getId() + SEPARATOR;
        return line + EOL;
    }
}
