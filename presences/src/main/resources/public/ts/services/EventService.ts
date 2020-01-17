import {ng} from 'entcore'
import http from 'axios';
import {EventResponse, Events} from "../models";
import {DateUtils} from "@common/utils";

export interface EventService {
    get(notebook): Promise<{ pageCount: number, events: EventResponse[], all: EventResponse[] }>;
}

export interface EventRequest {
    structureId: string;
    startDate: string;
    endDate: string;
    eventType: string;
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
            const eventType = `&eventType=${eventRequest.eventType}`;
            const userId = eventRequest.userId.length === 0 ? "" : `&eventType=${eventRequest.userId}`;
            const classes = eventRequest.classes.length === 0 ? "" : `&eventType=${eventRequest.classes}`;
            const regularized = eventRequest.regularized ? `&regularized=${!eventRequest.regularized}` : "";
            const page = `&page=${eventRequest.page}`;
            const urlParams = `${structureId}${startDate}${endDate}${eventType}${userId}${classes}${regularized}${page}`;

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
};

export const EventService = ng.service('EventService',
    (): EventService => eventService);
