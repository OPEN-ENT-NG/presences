import {ng} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {ActionBody, EventResponse, Events, IEventBody, IStudentEventRequest, IStudentEventResponse} from '../models';
import {DateUtils} from '@common/utils';
import {EventAbsenceSummary} from '@presences/models/Event/EventAbsenceSummary';
import {EXPORT_TYPE, ExportType} from "@common/core/enum/export-type.enum";

export interface EventService {
    get(eventRequest: EventRequest): Promise<{ pageCount: number, events: EventResponse[], all: EventResponse[] }>;

    getEventActions(event_id: number): Promise<ActionBody[]>;

    createAction(actionBody: ActionBody): Promise<AxiosResponse>;

    getStudentEvent(studentEventRequest: IStudentEventRequest): Promise<IStudentEventResponse>;

    export(eventRequest: EventRequest, exportType: ExportType): string;

    getAbsentsCounts(structureId: string, startDate: string, endDate: string): Promise<EventAbsenceSummary>;

    createLatenessEvent(eventBody: IEventBody, structureId: string): Promise<AxiosResponse>;

    updateEvent(eventId: number, eventBody: IEventBody): Promise<AxiosResponse>;

    deleteEvent(eventId: number): Promise<AxiosResponse>;
}

export interface EventRequest {
    structureId: string;
    startDate: string;
    endDate: string;
    startTime?: string;
    endTime?: string;
    noReason?: boolean;
    noReasonLateness?: boolean;
    eventType: string;
    listReasonIds?: string;
    regularized?: boolean;
    followed?: boolean;
    notFollowed?: boolean;
    userId?: string;
    userIds?: string[];
    classes?: string;
    classesIds?: string[];
    page?: number;
}

export const eventService: EventService = {
    get: async (eventRequest: EventRequest): Promise<{ pageCount: number, events: EventResponse[], all: EventResponse[] }> => {
        let dateFormat = DateUtils.FORMAT['YEAR-MONTH-DAY'];
        try {
            const structureId: string = `?structureId=${eventRequest.structureId}`;
            const startDate: string = `&startDate=${DateUtils.format(eventRequest.startDate, dateFormat)}`;
            const endDate: string = `&endDate=${DateUtils.format(eventRequest.endDate, dateFormat)}`;
            const startTime: string = eventRequest.startTime ? `&startTime=${eventRequest.startTime}` : '';
            const endTime: string = eventRequest.endTime ? `&endTime=${eventRequest.endTime}` : '';
            const noReason: string = eventRequest.noReason ? `&noReason=${eventRequest.noReason}` : '';
            const noReasonLateness: string = eventRequest.noReasonLateness ? `&noReasonLateness=${eventRequest.noReasonLateness}` : '';
            const eventType: string = eventRequest.eventType ? `&eventType=${eventRequest.eventType}` : '';
            const listReasonIds: string = eventRequest.listReasonIds ? `&reasonIds=${eventRequest.listReasonIds}` : '';
            const userId: string = eventRequest.userId && eventRequest.userId.length > 0 ? `&userId=${eventRequest.userId}` : '';

            let userIds: string = '';
            if (eventRequest.userIds && eventRequest.userIds.length > 0) {
                eventRequest.userIds.forEach(userId => userIds += `&userId=${userId}`);
            }

            const classes: string = eventRequest.classes && eventRequest.classes.length > 0 ? `&classes=${eventRequest.classes}` : '';

            let classesIds: string = '';
            if (eventRequest.classesIds && eventRequest.classesIds.length > 0) {
                eventRequest.classesIds.forEach(classId => classesIds += `&classes=${classId}`);
            }
            const regularized: string = (eventRequest.regularized !== undefined && eventRequest.regularized !== null)
                ? `&regularized=${eventRequest.regularized}` : '';

            const followed: string = (eventRequest.followed != null || eventRequest.notFollowed != null)
                    && (eventRequest.followed === !eventRequest.notFollowed) ? `&followed=${eventRequest.followed}` : '';

            const page: string = eventRequest.page ? `&page=${eventRequest.page}` : '';


            const urlParams: string = `${structureId}${startDate}${endDate}${startTime}${endTime}${noReason}${noReasonLateness}` +
            `${eventType}${listReasonIds}${userId}${userIds}${classes}${classesIds}${regularized}${followed}${page}`;

            const {data}: AxiosResponse = await http.get(`/presences/events${urlParams}`);

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

    export(eventRequest: EventRequest, exportType: ExportType): string {
        const dateFormat: string = DateUtils.FORMAT['YEAR-MONTH-DAY'];

        const url: string = `/presences/events/export`;
        const type: string = `?type=${EXPORT_TYPE[exportType]}`;
        const structureId: string = `&structureId=${eventRequest.structureId}`;
        const startDate: string = `&startDate=${DateUtils.format(eventRequest.startDate, dateFormat)}`;
        const endDate: string = `&endDate=${DateUtils.format(eventRequest.endDate, dateFormat)}`;
        const noReason: string = eventRequest.noReason ? `&noReason=${eventRequest.noReason}` : "";
        const eventType: string = `&eventType=${eventRequest.eventType}`;
        const listReasonIds: string = eventRequest.listReasonIds ? `&reasonIds=${eventRequest.listReasonIds}` : "";
        const userId: string = eventRequest.userId.length === 0 ? "" : `&userId=${eventRequest.userId}`;
        const classes: string = eventRequest.classes.length === 0 ? "" : `&classes=${eventRequest.classes}`;
        const regularized: string = (eventRequest.regularized != null) ? `&regularized=${(eventRequest.regularized)}` : "";
        const basedUrl: string = `${url}${type}${structureId}`;

        return `${basedUrl}${startDate}${endDate}${noReason}${eventType}${listReasonIds}${userId}${classes}${regularized}`;
    },


    /**
     * Get counts of current absences.
     *
     * @param structureId       structure id
     * @param startDate         start date filter (optional)
     * @param endDate           end date filter (optional)
     */
    async getAbsentsCounts(structureId: string, startDate: string, endDate: string): Promise<EventAbsenceSummary> {
        try {
            let url: string = `/presences/events/absences/summary`;

            if (structureId) {
                url += `?structureId=${structureId}`;
                url += `&currentDate=${DateUtils.getCurrentDate(DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC'])}`;
            }

            if (startDate && endDate) {
                url += `&startAt=${startDate}&endAt=${endDate}`;
            }

            const {data}: AxiosResponse = await http.get(url);
            return data;
        } catch (err) {
            throw err;
        }
    },

    /**
     * Create Lateness Event (special treatment (see API)
     *
     * @param eventBody     eventBody
     * @param structureId   structure identifier
     */
    createLatenessEvent(eventBody: IEventBody, structureId: string): Promise<AxiosResponse> {
        return http.post(`/presences/events/${structureId}/lateness`, eventBody);
    },

    /**
     * Update Event
     *
     * @param eventId       event identifier
     * @param eventBody     eventBody
     */
    updateEvent(eventId: number, eventBody: IEventBody): Promise<AxiosResponse> {
        return http.put(`/presences/events/${eventId}`, eventBody);
    },

    /**
     * Delete Event
     *
     * @param eventId       event identifier
     */
    deleteEvent(eventId: number): Promise<AxiosResponse> {
        return http.delete(`/presences/events/${eventId}`);
    }
};

export const EventService = ng.service('EventService', (): EventService => eventService);
