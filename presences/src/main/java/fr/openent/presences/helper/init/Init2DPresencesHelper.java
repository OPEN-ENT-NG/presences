package fr.openent.presences.helper.init;

import fr.openent.presences.model.Action;
import fr.openent.presences.model.Discipline;
import fr.openent.presences.model.Reason;
import fr.openent.presences.model.Settings;

import java.util.ArrayList;
import java.util.List;

public class Init2DPresencesHelper implements IInitPresencesHelper {
    private static final Integer NB_ACTIONS = 5;
    private static final Integer NB_DISCIPLINES = 4;

    protected Init2DPresencesHelper() {
    }

    public List<Reason> getReasonsInit() {
        List<Reason> reasons = new ArrayList<>();

        //Absence reason
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.0").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.1").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.2").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.3").setProving(false).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.4").setProving(false).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.5").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.6").setProving(false).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.7").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.8").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.9").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.10").setProving(true).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.11").setProving(true).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.12").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.13").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.14").setProving(true).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.15").setProving(true).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.16").setProving(true).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.17").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.18").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.19").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.20").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.21").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.22").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.23").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.24").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.25").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.26").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.27").setProving(true).setAbsenceCompliance(false).setReasonTypeId(1));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.28").setProving(false).setAbsenceCompliance(true).setReasonTypeId(1));

        //Lateness reason
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.29").setProving(true).setAbsenceCompliance(false).setReasonTypeId(2));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.30").setProving(true).setAbsenceCompliance(false).setReasonTypeId(2));
        reasons.add(new Reason().setLabel("presences.reasons.init.2d.31").setProving(true).setAbsenceCompliance(true).setReasonTypeId(2));

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
