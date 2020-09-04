package fr.openent.presences.enums;

import fr.openent.presences.Presences;

public enum WorkflowActions {
    CREATE_REGISTER(Presences.CREATE_REGISTER),
    READ_REGISTER(Presences.READ_REGISTER),
    SEARCH(Presences.SEARCH),
    SEARCH_STUDENTS(Presences.SEARCH_STUDENTS),
    EXPORT(Presences.EXPORT),
    READ_EVENT(Presences.READ_EVENT),
    NOTIFY(Presences.NOTIFY),
    CREATE_EVENT(Presences.CREATE_EVENT),
    MANAGE_EXEMPTION(Presences.MANAGE_EXEMPTION),
    MANAGE(Presences.MANAGE),
    ALERT(Presences.ALERTS_WIDGET),
    CREATE_PRESENCE(Presences.CREATE_PRESENCE),
    MANAGE_PRESENCE(Presences.MANAGE_PRESENCE),
    CREATE_ACTION(Presences.CREATE_ACTION),
    ABSENCES_WIDGET(Presences.ABSENCES_WIDGET),
    STUDENT_EVENTS_VIEW(Presences.STUDENT_EVENTS_VIEW),
    ABSENCE_STATEMENTS_VIEW(Presences.ABSENCE_STATEMENTS_VIEW),
    MANAGE_ABSENCE_STATEMENTS(Presences.MANAGE_ABSENCE_STATEMENTS),
    ABSENCE_STATEMENTS_CREATE(Presences.ABSENCE_STATEMENTS_CREATE),
    MANAGE_FORGOTTEN_NOTEBOOK(Presences.MANAGE_FORGOTTEN_NOTEBOOK);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString() {
        return this.actionName;
    }
}
