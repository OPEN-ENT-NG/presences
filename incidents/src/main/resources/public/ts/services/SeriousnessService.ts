import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';

export interface Seriousness {
    id: number;
    structure_id?: string;
    label: string;
    level: number;
    hidden: boolean;
    used?: boolean;
}

export interface SeriousnessRequest {
    id?: number;
    structureId?: string,
    label?: string;
    level?: number;
    hidden?: boolean;
}

export interface SeriousnessService {
    get(structureId: string): Promise<Seriousness[]>;
    create(seriousnessBody: SeriousnessRequest): Promise<AxiosResponse>;
    update(seriousnessBody: SeriousnessRequest): Promise<AxiosResponse>;
    delete(seriousnessId: number): Promise<AxiosResponse>;
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

    create: async (seriousnessBody: SeriousnessRequest): Promise<AxiosResponse> => {
        return await http.post(`/incidents/seriousness`, seriousnessBody);
    },

    update: async (seriousnessBody: SeriousnessRequest): Promise<AxiosResponse> => {
        return await http.put(`/incidents/seriousness`, seriousnessBody);
    },

    delete: async (seriousnessId: number): Promise<AxiosResponse> => {
        return await http.delete(`/incidents/seriousness?id=${seriousnessId}`);
    },
};

export const SeriousnessService = ng.service('SeriousnessService', (): SeriousnessService => seriousnessService);
