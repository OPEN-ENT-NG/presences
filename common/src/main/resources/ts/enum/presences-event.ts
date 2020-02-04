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