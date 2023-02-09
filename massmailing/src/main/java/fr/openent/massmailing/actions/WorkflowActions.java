package fr.openent.massmailing.actions;

import fr.openent.massmailing.Massmailing;

public enum WorkflowActions {
    VIEW(Massmailing.VIEW),
    MANAGE(Massmailing.MANAGE),
    MANAGE_RESTRICTED(Massmailing.MANAGE_RESTRICTED);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString() {
        return this.actionName;
    }
}
