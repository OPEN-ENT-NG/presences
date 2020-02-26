import {Event, EventResponse, Events} from "../models";
import {Reason} from "@presences/models/Reason";

export interface EventsFilter {
    startDate: Date;
    endDate: Date;
    students: any;
    classes: any;
    absences: boolean;
    late: boolean;
    departure: boolean;
    regularized: boolean;
    regularizedNotregularized: boolean;
    allReasons: boolean,
    noReasons: boolean,
    reasons: Reason,
    unjustified: boolean,
    justifiedNotRegularized: boolean,
    justifiedRegularized: boolean,
    noFilter: boolean
}

export interface EventsFormFilter {
    absences: boolean,
    late: boolean,
    departure: boolean,
    unjustified: boolean,
    justifiedNotRegularized: boolean,
    justifiedRegularized: boolean,
    allReasons: boolean,
    reasonIds: number[]
}

export class EventsUtils {

    public static readonly ALL_EVENTS = {
        event: 'event',
        absence: 'absence'
    };

    public static addEventsArray = function (item: Event, eventsId: number[]): void {
        if (item.type === EventsUtils.ALL_EVENTS.event) {
            if (eventsId.indexOf(item.id) === -1) {
                eventsId.push(item.id);
            }
        }
    };

    public static getReasonIds = function (events): number[] {
        let reasonIds = [];
        events.forEach(item => {
            reasonIds.push(item.reason_id);
            if ('events' in item) {
                item.events.forEach(itemEvent => {
                    reasonIds.push(itemEvent.reason_id);
                });
            }
        });
        return reasonIds;
    };

    /**
     * Method to fetch all ids in the concerned dayHistory and events
     * from one student's event.all
     */
    public static fetchEvents(event: EventResponse, fetchedEventIds: number[]) {
        event.events.forEach(event => {
            EventsUtils.addEventsArray(event, fetchedEventIds);
            if ('events' in event) {
                event.events.forEach(ee => {
                    EventsUtils.addEventsArray(ee, fetchedEventIds);
                });
            }
        });
    }

    public static resetEventId(eventId: number) {
        eventId = null;
    }

    public static setStudentToSync = (events: Events, eventsFilter: EventsFilter) => {
        events.userId = eventsFilter.students ? eventsFilter.students
            .map(students => students.id)
            .filter(function () {
                return true
            })
            .toString() : '';
    };

    public static setClassToSync = (events: Events, eventsFilter: EventsFilter) => {
        events.classes = eventsFilter.classes ? eventsFilter.classes
            .map(classes => classes.id)
            .filter(function () {
                return true
            })
            .toString() : '';
    };

    /* ----------------------------
    counsellor regularisation methods
    ---------------------------- */
    public static hasSameEventsCounsellor = (events: Event[]): boolean => {
        let counsellorArray: Array<boolean> = events.map(event => event.counsellor_regularisation);
        return new Set(counsellorArray).size === 1;
    };

    public static isEachEventsCounsellorRegularized = (events: Event[]): boolean => {
        return events.every(event => event.counsellor_regularisation);
    };

    public static initGlobalCounsellor = (event: EventResponse): boolean => {
        let counsellorArray: Array<boolean> = event.events.map(event => event.counsellor_regularisation);
        return new Set(counsellorArray).size === 1;
    };

    /* ----------------------------
    reasons methods
    ---------------------------- */
    public static hasSameEventsReason = (events: Event[]): boolean => {
        let reasonArray: Array<number> = events.map(event => event.reason_id);
        return new Set(reasonArray).size === 1;
    };

    public static initGlobalReason = (event: EventResponse): number => {
        let reasonArray: Array<number> = event.events.map(event => event.reason_id);
        /* reasonArray[0] corresponds all same reasonId, 0 if they are differents */
        return new Set(reasonArray).size === 1 ? reasonArray[0] : 0;
    };

    /* ----------------------------
    event type methods
    ---------------------------- */

    /* As at least a event where type is absence*/
    public static hasTypeEventAbsence = (events: Event[]): boolean => {
        let hasEventTypeAbsence: boolean = false;
        events.forEach(elem => {
            if (elem.type_id === 1) {
                hasEventTypeAbsence = true;
            }
        });
        return hasEventTypeAbsence;
    }

}