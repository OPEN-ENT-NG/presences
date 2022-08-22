/**
 * Events used to communicate between sniplets
 * Sibling action
 * SEND
 *
 * Parents action
 * TRANSMIT
 * SEND_BACK
 *
 * Another sibling respond
 * RESPOND
 */
export enum PRESENCES_DISCIPLINE {
    SEND = 'presence-discipline:send-form',
    TRANSMIT = 'presence-discipline:transmit-form',
    SEND_BACK = 'presence-discipline:send-back-form',
    RESPOND = 'presence-discipline:respond-form'
}

export enum PRESENCES_ACTION {
    SEND = 'presence-action:send-form',
    TRANSMIT = 'presence-action:transmit-form',
    SEND_BACK = 'presence-action:send-back-form',
    RESPOND = 'presence-action:respond-form'
}

export enum ABSENCE_FORM_EVENTS {
    EDIT = 'absence-form:edit',
    OPEN = 'absence-form:open',
    EDIT_EVENT = 'event-form:edit'
}

export enum LATENESS_FORM_EVENTS {
    EDIT = 'lateness-form:edit',
    OPEN = 'lateness-form:open'
}

export enum EVENTS_FORM {
    SUBMIT = 'events-form:submit'
}

export enum EVENTS_SEARCH {
    STUDENT = 'events-search:student',
    GROUP = 'events-search:group'
}

export enum EVENTS_DATE {
    EVENT_LIST_SAVE = 'events-date:event-list-save',
    ABSENCES_SAVE = 'events-date:absences-save',
    EVENT_LIST_SEND = 'events-date:event-list-send',
    ABSENCES_SEND = 'events-date:absences-send',
    EVENT_LIST_REQUEST = 'events-date-request:event-list-request',
    ABSENCES_REQUEST = 'events-date-request:absences-request'
}