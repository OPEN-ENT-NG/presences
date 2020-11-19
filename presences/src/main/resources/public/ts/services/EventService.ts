import {ng} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {ActionBody, EventResponse, Events, IStudentEventRequest, IStudentEventResponse} from '../models';
import {DateUtils} from '@common/utils';
import {EventAbsenceSummary} from '@presences/models/Event/EventAbsenceSummary';

export interface EventService {
    get(eventRequest: EventRequest): Promise<{ pageCount: number, events: EventResponse[], all: EventResponse[] }>;

    getEventActions(event_id: number): Promise<ActionBody[]>;

    createAction(actionBody: ActionBody): Promise<AxiosResponse>;

    getStudentEvent(studentEventRequest: IStudentEventRequest): Promise<IStudentEventResponse>;

    exportCSV(eventRequest: EventRequest): string;

    getAbsentsCounts(structureId: string): Promise<EventAbsenceSummary>;
}

export interface EventRequest {
    structureId: string;
    startDate: string;
    endDate: string;
    noReason: boolean;
    eventType: string;
    listReasonIds: string;
    regularized?: boolean;
    userId: string;
    classes: string;
    page?: number;
}

export const eventService: EventService = {
    get: async (eventRequest: EventRequest): Promise<{ pageCount: number, events: EventResponse[], all: EventResponse[] }> => {
        let dateFormat = DateUtils.FORMAT['YEAR-MONTH-DAY'];
        try {
            const structureId = `?structureId=${eventRequest.structureId}`;
            const startDate = `&startDate=${DateUtils.format(eventRequest.startDate, dateFormat)}`;
            const endDate = `&endDate=${DateUtils.format(eventRequest.endDate, dateFormat)}`;
            const noReason = eventRequest.noReason ? `&noReason=${eventRequest.noReason}` : "";
            const eventType = `&eventType=${eventRequest.eventType}`;
            const listReasonIds = eventRequest.listReasonIds ? `&reasonIds=${eventRequest.listReasonIds}` : "";
            const userId = eventRequest.userId.length === 0 ? "" : `&userId=${eventRequest.userId}`;
            const classes = eventRequest.classes.length === 0 ? "" : `&classes=${eventRequest.classes}`;
            const regularized = (eventRequest.regularized !== undefined && eventRequest.regularized !== null)
                ? `&regularized=${eventRequest.regularized}` : "";
            const page = `&page=${eventRequest.page}`;
            const urlParams = `${structureId}${startDate}${endDate}${noReason}${eventType}${listReasonIds}${userId}${classes}${regularized}${page}`;

            const {data} = await http.get(`/presences/events${urlParams}`);

            return {
                pageCount: data.page_count,
                events: data.all,
                all: Events.buildEventResponse(data.all, eventRequest.page)
            };
        } catch (err) {
            throw err;
        }
    },
    
    getEventActions: async (eventId: number): Promise<ActionBody[]> => {
        try {
            const {data} = await http.get(`/presences/events/${eventId}/actions`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    createAction: async (actionBody: ActionBody): Promise<AxiosResponse> => {
        return http.post(`/presences/events/actions`, actionBody);
    },

    getStudentEvent: async (studentEventRequest: IStudentEventRequest): Promise<IStudentEventResponse> => {
        try {
            const structure_id: string = `?structure_id=${studentEventRequest.structure_id}`;
            const start_at: string = `&start_at=${studentEventRequest.start_at}`;
            const end_at: string = `&end_at=${studentEventRequest.end_at}`;

            let types: string = '';
            if (studentEventRequest.type) {
                studentEventRequest.type.forEach((type: string) => {
                    types += `&type=${type}`;
                });
            }

            let limit: string = '';
            if (studentEventRequest.limit) {
                limit = `&limit=${studentEventRequest.limit}`;
            }

            let offset: string = '';
            if (studentEventRequest.offset) {
                offset = `&offset=${studentEventRequest.offset}`;
            }

            const urlParams = `${structure_id}${start_at}${end_at}${types}${limit}${offset}`;
            const {data} = await http.get(`/presences/students/${studentEventRequest.student_id}/events${urlParams}`);
            return data
        } catch (err) {
            throw err;
        }
    },

    exportCSV(eventRequest: EventRequest): string {
        const dateFormat: string = DateUtils.FORMAT['YEAR-MONTH-DAY'];

        const url: string = `/presences/events/export`;
        const structureId: string = `?structureId=${eventRequest.structureId}`;
        const startDate: string = `&startDate=${DateUtils.format(eventRequest.startDate, dateFormat)}`;
        const endDate: string = `&endDate=${DateUtils.format(eventRequest.endDate, dateFormat)}`;
        const noReason: string = eventRequest.noReason ? `&noReason=${eventRequest.noReason}` : "";
        const eventType: string = `&eventType=${eventRequest.eventType}`;
        const listReasonIds: string = eventRequest.listReasonIds ? `&reasonIds=${eventRequest.listReasonIds}` : "";
        const userId: string = eventRequest.userId.length === 0 ? "" : `&userId=${eventRequest.userId}`;
        const classes: string = eventRequest.classes.length === 0 ? "" : `&classes=${eventRequest.classes}`;
        const regularized: string = eventRequest.regularized ? `&regularized=${!eventRequest.regularized}` : "";

        return `${url}${structureId}${startDate}${endDate}${noReason}${eventType}${listReasonIds}${userId}${classes}${regularized}`;
    },


    /**
     * Get counts of current absences.
     *
     * @param structureId       structure id
     */
    async getAbsentsCounts(structureId: string): Promise<EventAbsenceSummary> {
        try {
            let url: string = `/presences/events/absences/summary`;

            if (structureId) {
                url += `?structureId=${structureId}`;
                url += `&currentDate=${DateUtils.getCurrentDate(DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC'])}`;
            }

            const {data}: AxiosResponse = await http.get(url);
            return data;
        } catch (err) {
            throw err;
        }
    }
};

export const EventService = ng.service('EventService',
    (): EventService => eventService);
