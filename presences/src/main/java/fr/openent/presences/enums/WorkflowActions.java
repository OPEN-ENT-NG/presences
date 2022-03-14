package fr.openent.presences.enums;

import fr.openent.presences.Presences;

public enum WorkflowActions {
    CREATE_REGISTER(Presences.CREATE_REGISTER),
    READ_REGISTER(Presences.READ_REGISTER),
    SEARCH(Presences.SEARCH),
    SEARCH_RESTRICTED(Presences.SEARCH_RESTRICTED),
    SEARCH_STUDENTS(Presences.SEARCH_STUDENTS),
    EXPORT(Presences.EXPORT),
    READ_EVENT(Presences.READ_EVENT),
    READ_EVENT_RESTRICTED(Presences.READ_EVENT_RESTRICTED),
    NOTIFY(Presences.NOTIFY),
    CREATE_EVENT(Presences.CREATE_EVENT),
    MANAGE_EXEMPTION(Presences.MANAGE_EXEMPTION),
    MANAGE(Presences.MANAGE),
    ALERT(Presences.ALERTS_WIDGET),
    ALERT_STUDENT_NUMBER(Presences.ALERTS_STUDENT_NUMBER),
    CREATE_PRESENCE(Presences.CREATE_PRESENCE),
    MANAGE_PRESENCE(Presences.MANAGE_PRESENCE),
    CREATE_ACTION(Presences.CREATE_ACTION),
    ABSENCES_WIDGET(Presences.ABSENCES_WIDGET),
    STUDENT_EVENTS_VIEW(Presences.STUDENT_EVENTS_VIEW),
    ABSENCE_STATEMENTS_VIEW(Presences.ABSENCE_STATEMENTS_VIEW),
    MANAGE_ABSENCE_STATEMENTS(Presences.MANAGE_ABSENCE_STATEMENTS),
    ABSENCE_STATEMENTS_CREATE(Presences.ABSENCE_STATEMENTS_CREATE),
    MANAGE_FORGOTTEN_NOTEBOOK(Presences.MANAGE_FORGOTTEN_NOTEBOOK),
    MANAGE_COLLECTIVE_ABSENCES(Presences.MANAGE_COLLECTIVE_ABSENCES),
    STATISTICS_ACCESS_DATA(Presences.STATISTICS_ACCESS_DATA),
    CALENDAR_VIEW(Presences.CALENDAR_VIEW);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString() {
        return this.actionName;
    }
}
