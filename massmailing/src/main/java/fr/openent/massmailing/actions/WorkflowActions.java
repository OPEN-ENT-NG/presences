package fr.openent.massmailing.actions;

import fr.openent.massmailing.Massmailing;

public enum WorkflowActions {
    MANAGE(Massmailing.MANAGE);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString() {
        return this.actionName;
    }
}
