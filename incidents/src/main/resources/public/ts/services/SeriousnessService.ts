import {ng} from 'entcore'
import { http, HttpResponse } from 'entcore-toolkit';

export interface Seriousness {
    id: number;
    structure_id?: string;
    label: string;
    level: number;
    hidden: boolean;
    used?: boolean;
    exclude_alert_seriousness: boolean;
}

export interface SeriousnessRequest {
    id?: number;
    structureId?: string,
    label?: string;
    level?: number;
    hidden?: boolean;
    excludeAlertSeriousness?: boolean;
}

export interface SeriousnessService {
    get(structureId: string): Promise<Seriousness[]>;
    create(seriousnessBody: SeriousnessRequest): Promise<HttpResponse>;
    update(seriousnessBody: SeriousnessRequest): Promise<HttpResponse>;
    delete(seriousnessId: number): Promise<HttpResponse>;
}

export const seriousnessService : SeriousnessService = {
    get: async (structureId: string): Promise<Seriousness[]> => {
        try {
            const {data} = await http.get(`/incidents/seriousnesses?structureId=${structureId}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    create: async (seriousnessBody: SeriousnessRequest): Promise<HttpResponse> => {
        return await http.post(`/incidents/seriousness`, seriousnessBody);
    },

    update: async (seriousnessBody: SeriousnessRequest): Promise<HttpResponse> => {
        return await http.put(`/incidents/seriousness`, seriousnessBody);
    },

    delete: async (seriousnessId: number): Promise<HttpResponse> => {
        return await http.delete(`/incidents/seriousness?id=${seriousnessId}`);
    },
};

export const SeriousnessService = ng.service('SeriousnessService', (): SeriousnessService => seriousnessService);
