package fr.openent.presences.enums;

import fr.openent.presences.common.security.IWorkflowActionsCouple;

public enum WorkflowActionsCouple implements IWorkflowActionsCouple {

    READ_EVENT(WorkflowActions.READ_EVENT, WorkflowActions.READ_EVENT_RESTRICTED),
    READ_PRESENCE(WorkflowActions.READ_PRESENCE, WorkflowActions.READ_PRESENCE_RESTRICTED),
    MANAGE_ABSENCE_STATEMENTS(WorkflowActions.MANAGE_ABSENCE_STATEMENTS, WorkflowActions.MANAGE_ABSENCE_STATEMENTS_RESTRICTED),
    MANAGE_EXEMPTION(WorkflowActions.MANAGE_EXEMPTION, WorkflowActions.MANAGE_EXEMPTION_RESTRICTED),
    READ_EXEMPTION(WorkflowActions.READ_EXEMPTION, WorkflowActions.READ_EXEMPTION_RESTRICTED),
    SEARCH_STUDENTS(WorkflowActions.SEARCH_STUDENTS, WorkflowActions.SEARCH_RESTRICTED),
    SEARCH(WorkflowActions.SEARCH, WorkflowActions.SEARCH_RESTRICTED),
    SEARCH_VIESCO(WorkflowActions.SEARCH_VIESCO, WorkflowActions.SEARCH_VIESCO_RESTRICTED),
    VIEW_STATISTICS(WorkflowActions.VIEW_STATISTICS, WorkflowActions.VIEW_STATISTICS_RESTRICTED);

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
