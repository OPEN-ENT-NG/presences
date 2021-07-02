import {Absence, EventResponse, Events, EventType, IEvent, IEventBody, ITimeSlot, Lateness} from "../models";
import {Reason} from "@presences/models/Reason";
import {DateUtils} from "@common/utils";

export interface EventsFilter {
    startDate: Date;
    endDate: Date;
    students?: any;
    classes?: any;
    absences?: boolean;
    late?: boolean;
    departure?: boolean;
    regularized?: boolean;
    regularizedNotregularized?: boolean;
    allReasons?: boolean,
    noReasons?: boolean,
    reasons?: Reason,
    reasonIds?: number[],
    timeslots?: {
        start: ITimeSlot;
        end: ITimeSlot;
    },
    unjustified?: boolean,
    justifiedNotRegularized?: boolean,
    justifiedRegularized?: boolean,
    followed?: boolean,
    notFollowed?: boolean,
    noFilter?: boolean;
    page?: number;
}

export interface EventsFormFilter {
    classes?: any;
    students?: any;
    absences?: boolean,
    late?: boolean,
    departure?: boolean,
    unjustified?: boolean,
    justifiedNotRegularized?: boolean,
    justifiedRegularized?: boolean,
    followed?: boolean,
    notFollowed?: boolean,
    allReasons?: boolean,
    noReasons?: boolean,
    reasonIds?: number[],
    timeslots?: {
        start: ITimeSlot;
        end: ITimeSlot;
    }
}

export class EventsUtils {

    public static readonly ALL_EVENTS = {
        event: 'event',
        absence: 'absence'
    };

    public static addEventsArray = (item: IEvent, events: Array<IEvent | EventResponse>): void => {
        if (item.type === EventsUtils.ALL_EVENTS.event && item.type_id === 1
            && events.map((event: IEvent) => event.id).indexOf(item.id) === -1) {
            events.push(item);
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
    public static fetchEvents(event: EventResponse, fetchedEvents: Array<IEvent | EventResponse>) {
        event.events.forEach((event: IEvent) => {
            EventsUtils.addEventsArray(event, fetchedEvents);
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

    /**
     * Refresh one page of the events list data with the provided pageEvents data.
     * @param events        the event list
     * @param pageEvents    the new data to apply
     * @param page          the new data page number
     */
    public static interactiveConcat = (events: EventResponse[], pageEvents: EventResponse[], page: number): EventResponse[] => {

        if (events.length === 0 || pageEvents.length === 0)
            return events;

        const beforePage: EventResponse[] = events.slice(0, events.findIndex((event: EventResponse) => event.page === page));
        const afterPage: EventResponse[] = events.findIndex((event: EventResponse) => event.page > page) != -1 ?
            events.slice(events.findIndex((event: EventResponse) => event.page > page), events.length) : [];

        return beforePage.concat(pageEvents).concat(afterPage);
    };

    public static isValidForm = (event: Absence | Lateness | IEventBody): boolean => {
        return DateUtils.isPeriodValid(event.start_date, event.end_date);
    }

    /* ----------------------------
    counsellor regularisation methods
    ---------------------------- */
    public static hasSameEventsCounsellor = (events: IEvent[]): boolean => {
        let counsellorArray: Array<boolean> = events.map(event => event.counsellor_regularisation);
        return new Set(counsellorArray).size === 1;
    };

    public static isEachEventsCounsellorRegularized = (events: IEvent[]): boolean => {
        return events.every((event: IEvent) => event.counsellor_regularisation);
    };

    public static initGlobalCounsellor = (event: EventResponse): boolean => {
        let counsellorArray: Array<boolean> = event.events.map(event => event.counsellor_regularisation);
        return new Set(counsellorArray).size === 1;
    };

    /* ----------------------------
    reasons methods
    ---------------------------- */
    public static hasSameEventsReason = (events: IEvent[]): boolean => {
        let reasonArray: Array<number> = events.map((event: IEvent) => event.reason_id);
        return new Set(reasonArray).size === 1;
    };

    public static initGlobalReason = (event: EventResponse): number => {
        let reasonArray: Array<number> = event.events.map(event => event.reason_id);
        /* reasonArray[0] corresponds all same reasonId, 0 if they are different */
        return new Set(reasonArray).size === 1 ? reasonArray[0] : 0;
    };

    /* ----------------------------
    event type methods
    ---------------------------- */

    /* As at least a event where type is absence*/
    public static hasTypeEventAbsence = (events: Array<IEvent>): boolean => {
        return events.some((event: IEvent) => event.type_id === EventType.ABSENCE);
    }

}