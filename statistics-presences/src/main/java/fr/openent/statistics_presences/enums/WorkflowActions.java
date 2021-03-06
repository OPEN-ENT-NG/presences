package fr.openent.statistics_presences.enums;

import fr.openent.statistics_presences.StatisticsPresences;

public enum WorkflowActions {
    STATISTICS_PRESENCES_VIEW(StatisticsPresences.VIEW);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString() {
        return this.actionName;
    }
}
