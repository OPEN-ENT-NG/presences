import {Event, EventResponse, Events} from "../models";
import {DateUtils} from "@common/utils";
import {Reason} from "@presences/services";

export interface EventsFilter {
    startDate: Date;
    endDate: Date;
    students: any;
    classes: any;
    absences: boolean;
    late: boolean;
    departure: boolean;
    regularized: boolean;
    allReasons: boolean,
    noReasons: boolean,
    reasons: Reason,
    unjustified: boolean,
    justifiedNotRegularized: boolean,
    justifiedRegularized: boolean
}

export class EventsUtils {

    public static readonly ALL_EVENTS = {
        event: 'event',
        absence: 'absence'
    };

    public static addEventsAndAbsencesArray = function (item: Event, eventsId: number[], absencesId: number[]): void {
        if (item.type === EventsUtils.ALL_EVENTS.event) {
            if (eventsId.indexOf(item.id) === -1) {
                eventsId.push(item.id);
            }
        }
        if (item.type === EventsUtils.ALL_EVENTS.absence) {
            if (absencesId.indexOf(item.id) === -1) {
                absencesId.push(item.id);
            }
        }
    };

    public static filterHistory(events: Array<Event>): Array<Event> {
        let tmpEvent: Array<Event> = JSON.parse(JSON.stringify(events));
        let longestTimeEvent: Event = tmpEvent.sort(DateUtils.compareTime('start_date', 'end_time'))[tmpEvent.length - 1];
        if (longestTimeEvent.type === this.ALL_EVENTS.absence) {
            let newEvents: Array<Event> = [];
            longestTimeEvent.events = [];
            events.filter(e => e.type === this.ALL_EVENTS.event).forEach(event => {
                if (!DateUtils.isBetween(event.start_date, event.end_date,
                    longestTimeEvent.start_date, longestTimeEvent.end_date)) {
                    newEvents.push(event);
                } else {
                    longestTimeEvent.events.push(event);
                }
            });
            newEvents.push(longestTimeEvent);
            return newEvents;
        } else {
            return events;
        }
    }

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
    public static fetchEventsAbsencesId(event: EventResponse, fetchedEventIds: number[], fetchedAbsenceIds: number[]) {
        event.dayHistory.forEach(dayHistoryElement => {
            dayHistoryElement.events.forEach(event => {
                EventsUtils.addEventsAndAbsencesArray(event, fetchedEventIds, fetchedAbsenceIds);
            })
        });
        event.events.forEach(event => {
            EventsUtils.addEventsAndAbsencesArray(event, fetchedEventIds, fetchedAbsenceIds);
            if ('events' in event) {
                event.events.forEach(ee => {
                    EventsUtils.addEventsAndAbsencesArray(ee, fetchedEventIds, fetchedAbsenceIds);
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