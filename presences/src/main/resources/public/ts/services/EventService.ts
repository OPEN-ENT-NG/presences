import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {ActionBody, EventResponse, Events} from "../models";
import {DateUtils} from "@common/utils";

export interface EventService {
    get(notebook): Promise<{ pageCount: number, events: EventResponse[], all: EventResponse[] }>;

    getEventActions(event_id: number): Promise<ActionBody[]>;

    createAction(actionBody: ActionBody): Promise<AxiosResponse>;
}

export interface EventRequest {
    structureId: string;
    startDate: string;
    endDate: string;
    noReason: boolean;
    eventType: string;
    listReasonIds: string;
    regularized: boolean;
    userId: string;
    classes: string;
    page: number;
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
            const classes = eventRequest.classes.length === 0 ? "" : `&eventType=${eventRequest.classes}`;
            const regularized = eventRequest.regularized ? `&regularized=${!eventRequest.regularized}` : "";
            const page = `&page=${eventRequest.page}`;
            const urlParams = `${structureId}${startDate}${endDate}${noReason}${eventType}${listReasonIds}${userId}${classes}${regularized}${page}`;

            const {data} = await http.get(`/presences/events${urlParams}`);

            return {
                pageCount: data.page_count,
                events: data.all,
                all: Events.buildEventResponse(data.all)
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
    }
};

export const EventService = ng.service('EventService',
    (): EventService => eventService);
