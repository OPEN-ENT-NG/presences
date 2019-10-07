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
export enum INCIDENTS_TYPE_EVENT {
    SEND = 'incident-type:send-form',
    TRANSMIT = 'incident-type:transmit-form',
    SEND_BACK = 'incident-type:send-back-form',
    RESPOND = 'incident-type:respond-form'
}

export enum INCIDENTS_PARTNER_EVENT {
    SEND = 'incident-partner:send-form',
    TRANSMIT = 'incident-partner:transmit-form',
    SEND_BACK = 'incident-partner:send-back-form',
    RESPOND = 'incident-partner:respond-form'
}

export enum INCIDENTS_PLACE_EVENT {
    SEND = 'incident-place:send-form',
    TRANSMIT = 'incident-place:transmit-form',
    SEND_BACK = 'incident-place:send-back-form',
    RESPOND = 'incident-place:respond-form'
}

export enum INCIDENTS_PROTAGONIST_TYPE_EVENT {
    SEND = 'incident-protagonistType:send-form',
    TRANSMIT = 'incident-protagonistType:transmit-form',
    SEND_BACK = 'incident-protagonistType:send-back-form',
    RESPOND = 'incident-protagonistType:respond-form'
}

export enum INCIDENTS_SERIOUSNESS_EVENT {
    SEND = 'incident-seriousness:send-form',
    TRANSMIT = 'incident-seriousness:transmit-form',
    SEND_BACK = 'incident-seriousness:send-back-form',
    RESPOND = 'incident-seriousness:respond-form'
}