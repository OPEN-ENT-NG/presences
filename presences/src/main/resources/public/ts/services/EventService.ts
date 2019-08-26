import {ng} from 'entcore'
import http from 'axios';

export interface ReasonType {
    id: number;
    label: string;
    structure_id: string;
    proving: boolean;
    comment: string;
    default: boolean;
    group: boolean;
}

export interface EventService {
    getReasonsType(structureId: string): Promise<ReasonType[]>
}

export const eventService : EventService = {
    getReasonsType: async (structureId: string) => {
        try {
            const {data} = await http.get(`/presences/event/reason/types?structureId=${structureId}`);
            return data;
        } catch (err) {
            throw err;
        }
    }
};

export const EventService = ng.service('EventService', (): EventService => eventService);
