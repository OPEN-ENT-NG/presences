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
}