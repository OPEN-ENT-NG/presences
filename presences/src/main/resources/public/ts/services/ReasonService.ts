import {idiom as lang, ng} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {Reason, ReasonRequest} from "@presences/models/Reason";

export interface ReasonService {
    getReasons(structureId: string, reasonTypeId?: Number): Promise<Reason[]>;

    create(reasonBody: ReasonRequest, reasonTypeId?: Number): Promise<AxiosResponse>;

    update(reasonBody: ReasonRequest): Promise<AxiosResponse>;

    delete(reasonId: number): Promise<AxiosResponse>;
}

export const reasonService: ReasonService = {
    getReasons: async (structureId: string, reasonTypeId?: Number): Promise<Reason[]> => {
        try {
            let reasonTypeIdParam: string = reasonTypeId ? `&reasonTypeId=${reasonTypeId}` : '';
            const {data} = await http.get(`/presences/reasons?structureId=${structureId}${reasonTypeIdParam}`);
            data.map((reason: Reason) => {
                if (reason.id === -1) {
                    reason.label = lang.translate(reason.label);
                    reason.hidden = true;
                }
            });
            return data;
        } catch (err) {
            throw err;
        }
    },

    create: async (reasonBody: ReasonRequest, reasonTypeId?: Number): Promise<AxiosResponse> => {
        let reasonTypeIdParam: string = reasonTypeId ? `?reasonTypeId=${reasonTypeId}` : '';
        return http.post(`/presences/reason${reasonTypeIdParam}`, reasonBody);
    },

    update: async (reasonBody: ReasonRequest): Promise<AxiosResponse> => {
        return http.put(`/presences/reason`, reasonBody);
    },

    delete: async (reasonId: number): Promise<AxiosResponse> => {
        return http.delete(`/presences/reason?id=${reasonId}`);
    },
};

export const ReasonService = ng.service('ReasonService', (): ReasonService => reasonService);
