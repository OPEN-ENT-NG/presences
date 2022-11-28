package fr.openent.presences.helper.init;

import fr.openent.presences.enums.ReasonType;
import fr.openent.presences.model.*;

import java.util.ArrayList;
import java.util.List;

public class DefaultInit2DPresencesHelper implements IInitPresencesHelper {
    private static final Integer NB_ACTIONS = 5;
    private static final Integer NB_DISCIPLINES = 4;

    protected DefaultInit2DPresencesHelper() {
    }

    public List<ReasonModel> getReasonsInit() {
        List<ReasonModel> reasons = new ArrayList<>();

        //Absence reason
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.0").setAlertExclude(true).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.1").setAlertExclude(true).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.2").setAlertExclude(true).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.3").setAlertExclude(true).setProving(false).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.4").setAlertExclude(true).setProving(false).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.5").setAlertExclude(true).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.6").setAlertExclude(true).setProving(false).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.7").setAlertExclude(true).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.8").setAlertExclude(true).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.9").setAlertExclude(true).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.10").setAlertExclude(true).setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.11").setAlertExclude(true).setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.12").setAlertExclude(true).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.13").setAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.14").setAlertExclude(true).setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.15").setAlertExclude(true).setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.16").setAlertExclude(true).setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.17").setAlertExclude(true).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.18").setAlertExclude(true).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.19").setAlertExclude(true).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.20").setAlertExclude(true).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.21").setAlertExclude(true).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.22").setAlertExclude(true).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.23").setAlertExclude(true).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.24").setAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.25").setAlertExclude(true).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.26").setAlertExclude(true).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.27").setAlertExclude(true).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.ABSENCE));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.28").setAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));

        //Lateness reason
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.29").setAlertExclude(true).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.LATENESS));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.30").setAlertExclude(true).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.LATENESS));
        reasons.add(new ReasonModel().setLabel("presences.reasons.init.2d.31").setAlertExclude(false).setProving(true).setAbsenceCompliance(true).setReasonTypeId(ReasonType.LATENESS));

        return reasons;
    }

    @Override
    public List<Action> getActionsInit() {
        List<Action> actions = new ArrayList<>();
        for (int i = 0; i < NB_ACTIONS; i++) {
            Action action = new Action(i)
                    .setLabel("presences.actions.init.2d." + i)
                    .setAbbreviation("presences.actions.abbr.init.2d." + i);
            actions.add(action);
        }
        return actions;
    }

    @Override
    public Settings getSettingsInit() {
        return new Settings(5, 3, 3, 3);
    }

    @Override
    public List<Discipline> getDisciplinesInit() {
        List<Discipline> disciplines = new ArrayList<>();
        for (int i = 0; i < NB_DISCIPLINES; i++) {
            Discipline discipline = new Discipline(i)
                    .setLabel("presences.discipline.init.2d." + i);
            disciplines.add(discipline);
        }
        return disciplines;
    }
}
