package fr.openent.presences.helper.init;

import fr.openent.presences.model.Action;
import fr.openent.presences.model.Discipline;
import fr.openent.presences.model.Reason;
import fr.openent.presences.model.Settings;

import java.util.ArrayList;
import java.util.List;

public class Init1DPresencesHelper implements IInitPresencesHelper {
    private static final Integer NB_DISCIPLINES = 4;

    protected Init1DPresencesHelper() {
    }

    @Override
    public List<Reason> getReasonsInit() {
        List<Reason> reasons = new ArrayList<>();

        //Absence reason
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.0").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.1").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.2").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.3").setProving(false).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.4").setProving(false).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.5").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.6").setProving(false).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.7").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.8").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.9").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.10").setProving(true).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.11").setProving(true).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.12").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.13").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.14").setProving(true).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.15").setProving(true).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.16").setProving(true).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.17").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.18").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.19").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.20").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.21").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));

        //Lateness reason
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.22").setProving(true).setAbsenceCompliance(false).setReasonTypeId(2));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.23").setProving(true).setAbsenceCompliance(false).setReasonTypeId(2));
        reasons.add(new Reason().setLabel("presences.reasons.init.1d.24").setProving(true).setAbsenceCompliance(false).setReasonTypeId(2));

        return reasons;
    }

    @Override
    public List<Action> getActionsInit() {
        return IInitPresencesHelper.init2DPresencesHelper.getActionsInit();
    }

    @Override
    public Settings getSettingsInit() {
        return new Settings(4, 3, 3, 3);
    }

    @Override
    public List<Discipline> getDisciplinesInit() {
        List<Discipline> disciplines = new ArrayList<>();
        for (int i = 0; i < NB_DISCIPLINES; i++) {
            Discipline discipline = new Discipline(i)
                    .setLabel("presences.discipline.init.1d." + i);
            disciplines.add(discipline);
        }
        return disciplines;
    }
}
