package fr.openent.presences.enums;

import fr.openent.presences.Presences;

public enum WorkflowActions {
    CREATE_REGISTER(Presences.CREATE_REGISTER),
    READ_REGISTER(Presences.READ_REGISTER),
    SEARCH(Presences.SEARCH),
    EXPORT(Presences.EXPORT),
    NOTIFY(Presences.NOTIFY),
    CREATE_EVENT(Presences.CREATE_EVENT),
    MANAGE_EXEMPTION(Presences.MANAGE_EXEMPTION),
    MANAGE(Presences.MANAGE),
    ALERT(Presences.ALERTS_WIDGET);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString() {
        return this.actionName;
    }
}
