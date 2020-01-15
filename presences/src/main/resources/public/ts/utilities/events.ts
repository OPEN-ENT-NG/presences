import {Event, EventResponse, Events} from "../models";
import {_} from "entcore";
import {DateUtils} from "@common/utils";

export interface EventsFilter {
    startDate: Date;
    endDate: Date;
    students: any;
    classes: any;
    absences: boolean;
    late: boolean;
    departure: boolean;
    regularized: boolean;
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
        });
        return reasonIds;
    };

    public static initGlobalCounsellorRegularisation = function (event: EventResponse): boolean {
        let regularizedArray = [];
        event.events.forEach(e => {
            regularizedArray.push(e.counsellor_regularisation);
            if ('events' in e && e.events.length > 0) {
                e.events.forEach(itemEvent => {
                    regularizedArray.push(itemEvent.counsellor_regularisation);
                });
            }
        });
        return regularizedArray.reduce((current, initial) => initial && current)
    };

    public static initGlobalReason = function (event: EventResponse) {
        let reasonIds = EventsUtils.getReasonIds(event.events);
        if (!reasonIds.every((val, i, arr) => val === arr[0])) {
            event.globalReason = 0;
        } else {
            event.globalReason = parseInt(_.uniq(reasonIds));
            if (isNaN(event.globalReason)) {
                event.globalReason = null;
            }
        }
    };

    public static manageEventDrop(events: Events, eventsFilter: EventsFilter, eventId: number, history, event, index) {
        if (history.counsellor_regularisation && eventsFilter.regularized) {
            const {id} = history;
            event.events = event.events.filter(evt => evt.id !== id);
        }
        if (event.events.length === 0 && eventsFilter.regularized) {
            events.all = events.all.filter((evt, i) => i !== index);
            eventId = null;
        } else {
            event.globalCounsellorRegularisation = EventsUtils.initGlobalCounsellorRegularisation(event);
            EventsUtils.initGlobalReason(event);
        }
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