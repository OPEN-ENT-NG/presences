package fr.openent.incidents.enums;

import fr.openent.incidents.Incidents;

public enum WorkflowActions {
    MANAGE_INCIDENT(Incidents.MANAGE_INCIDENT);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString() {
        return this.actionName;
    }
}
