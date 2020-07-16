package fr.openent.incidents.enums;

import fr.openent.incidents.Incidents;

public enum WorkflowActions {
    MANAGE_INCIDENT(Incidents.MANAGE_INCIDENT),
    SANCTION_CREATE(Incidents.SANCTION_CREATE),
    PUNISHMENT_CREATE(Incidents.PUNISHMENT_CREATE),
    PUNISHMENTS_VIEW(Incidents.PUNISHMENTS_VIEW),
    SANCTIONS_VIEW(Incidents.SANCTIONS_VIEW),
    STUDENT_EVENTS_VIEW(Incidents.STUDENT_EVENTS_VIEW);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString() {
        return this.actionName;
    }
}
