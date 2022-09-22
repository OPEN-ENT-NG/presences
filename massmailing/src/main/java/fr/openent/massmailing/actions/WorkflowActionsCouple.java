package fr.openent.massmailing.actions;

import fr.openent.presences.common.security.IWorkflowActionsCouple;

public enum WorkflowActionsCouple implements IWorkflowActionsCouple {

    VIEW(WorkflowActions.VIEW, WorkflowActions.VIEW_RESTRICTED),
    MANAGE(WorkflowActions.MANAGE, WorkflowActions.MANAGE_RESTRICTED);

    private final WorkflowActions unrestrictedAction;
    private final WorkflowActions restrictedAction;

    WorkflowActionsCouple(WorkflowActions unrestrictedAction, WorkflowActions restrictedAction) {
        this.unrestrictedAction = unrestrictedAction;
        this.restrictedAction = restrictedAction;
    }

    @Override
    public String getUnrestrictedAction() {
        return unrestrictedAction.name();
    }

    @Override
    public String getRestrictedAction() {
        return restrictedAction.name();
    }
}
