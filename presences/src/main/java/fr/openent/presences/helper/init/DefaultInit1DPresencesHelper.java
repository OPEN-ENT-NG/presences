package fr.openent.presences.helper.init;

import fr.openent.presences.enums.ReasonType;
import fr.openent.presences.model.*;

import java.util.ArrayList;
import java.util.List;

public class DefaultInit1DPresencesHelper implements IInitPresencesHelper {
    private static final Integer NB_DISCIPLINES = 4;

    protected DefaultInit1DPresencesHelper() {
    }

    @Override
    public List<ReasonModel> getReasonsInit() {
        List<ReasonModel> reasons = new ArrayList<>();

        //Absence reason
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.0").setUnregularizedAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.1").setUnregularizedAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.2").setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.3").setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.4").setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.5").setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));

        //Lateness reason
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.6").setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.LATENESS));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.7").setLatenessAlertExclude(false).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.LATENESS));

        return reasons;
    }

    @Override
    public List<Action> getActionsInit() {
        return IInitPresencesHelper.defaultInit2DPresencesHelper.getActionsInit();
    }

    @Override
    public Settings getSettingsInit() {
        return new Settings(4, 5, 999, 999);
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
