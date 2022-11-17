package fr.openent.presences.helper.init;

import fr.openent.presences.enums.ReasonType;
import fr.openent.presences.model.*;

import java.util.ArrayList;
import java.util.List;

public class Init1DPresencesHelper implements IInitPresencesHelper {
    private static final Integer NB_DISCIPLINES = 4;

    protected Init1DPresencesHelper() {
    }

    @Override
    public List<ReasonModel> getReasonsInit() {
        List<ReasonModel> reasons = new ArrayList<>();

        //Absence reason
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.0").setAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.1").setAlertExclude(false).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.2").setAlertExclude(false).setProving(false).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.3").setAlertExclude(false).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.4").setAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.5").setAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.6").setAlertExclude(false).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.7").setAlertExclude(false).setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.8").setAlertExclude(false).setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.9").setAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.10").setAlertExclude(true).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.11").setAlertExclude(false).setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.12").setAlertExclude(false).setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.13").setAlertExclude(false).setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.14").setAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.15").setAlertExclude(false).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.16").setAlertExclude(false).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.17").setAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.18").setAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.19").setAlertExclude(false).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.20").setAlertExclude(false).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.21").setAlertExclude(true).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));

        //Lateness reason
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.22").setAlertExclude(false).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.LATENESS));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.23").setAlertExclude(false).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.LATENESS));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.1d.24").setAlertExclude(true).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.LATENESS));

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
