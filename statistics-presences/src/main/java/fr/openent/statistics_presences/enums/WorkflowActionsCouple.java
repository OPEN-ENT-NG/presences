package fr.openent.statistics_presences.enums;

import fr.openent.presences.common.security.IWorkflowActionsCouple;

public enum WorkflowActionsCouple implements IWorkflowActionsCouple {
    STATISTICS_PRESENCES_MANAGE(WorkflowActions.STATISTICS_PRESENCES_MANAGE, WorkflowActions.STATISTICS_PRESENCES_MANAGE_RESTRICTED);

    private final WorkflowActions unrestrictedAction;
    private final WorkflowActions restrictedAction;

    WorkflowActionsCouple(WorkflowActions unrestrictedAction, WorkflowActions restrictedAction) {
        this.unrestrictedAction = unrestrictedAction;
        this.restrictedAction = restrictedAction;
    }

    @Override
    public String getUnrestrictedAction() {
        return unrestrictedAction.toString();
    }

    @Override
    public String getRestrictedAction() {
        return restrictedAction.toString();
    }
}
