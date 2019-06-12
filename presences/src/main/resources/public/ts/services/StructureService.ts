import {ng} from 'entcore'
import http from 'axios';

export interface TimeSlot {
    name: string;
    startHour: string;
    endHour: string;
    id: string;
}

export interface StructureService {
    getSlotProfile(structureId: string): Promise<TimeSlot[]>
}

export const StructureService = ng.service('StructureService', (): StructureService => ({
    getSlotProfile: async (structureId) => {
        try {
            const {data} = await http.get(`/viescolaire/structures/${structureId}/time-slot`);
            return data;
        } catch (err) {
            throw err;
        }
    }
}));

