import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';

export interface Reason {
    id: number;
    label: string;
    structure_id: string;
    proving: boolean;
    comment: string;
    default: boolean;
    group: boolean;
    hidden: boolean;
    absence_compliance: boolean;
    regularisable: boolean;
    used?: boolean;
}

export interface ReasonRequest {
    id?: number;
    label: string;
    absenceCompliance: boolean;
    hidden?: boolean;
    regularisable?: boolean;
    structureId?: string;
}

export interface ReasonService {
    getReasons(structureId: string): Promise<Reason[]>;
    create(reasonBody: ReasonRequest): Promise<AxiosResponse>;
    update(reasonBody: ReasonRequest): Promise<AxiosResponse>;
    delete(reasonId: number): Promise<AxiosResponse>;
}

export const reasonService : ReasonService = {
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
        return await http.delete(`/presences/reason?reasonId=${reasonId}`);
    },
};

export const ReasonService = ng.service('ReasonService', (): ReasonService => reasonService);
