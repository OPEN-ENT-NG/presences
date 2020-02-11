import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {Reason, ReasonRequest} from "@presences/models/Reason";

export interface ReasonService {
    getReasons(structureId: string): Promise<Reason[]>;

    create(reasonBody: ReasonRequest): Promise<AxiosResponse>;

    update(reasonBody: ReasonRequest): Promise<AxiosResponse>;

    delete(reasonId: number): Promise<AxiosResponse>;
}

export const reasonService: ReasonService = {
    getReasons: async (structureId: string): Promise<Reason[]> => {
        try {
            const {data} = await http.get(`/presences/reasons?structureId=${structureId}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    create: async (reasonBody: ReasonRequest): Promise<AxiosResponse> => {
        return await http.post(`/presences/reason`, reasonBody);
    },

    update: async (reasonBody: ReasonRequest): Promise<AxiosResponse> => {
        return await http.put(`/presences/reason`, reasonBody);
    },

    delete: async (reasonId: number): Promise<AxiosResponse> => {
        return await http.delete(`/presences/reason?id=${reasonId}`);
    },
};

export const ReasonService = ng.service('ReasonService', (): ReasonService => reasonService);
