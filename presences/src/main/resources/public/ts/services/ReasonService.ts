import {idiom as lang, ng} from 'entcore'
import { http, HttpResponse } from 'entcore-toolkit';
import {Reason, ReasonRequest} from "@presences/models/Reason";
import {REASON_TYPE_ID} from "@common/core/enum/reason-type-id";

export interface ReasonService {
    getReasons(structureId: string, reasonTypeId?: Number): Promise<Reason[]>;

    create(reasonBody: ReasonRequest, reasonTypeId?: Number): Promise<HttpResponse>;

    update(reasonBody: ReasonRequest): Promise<HttpResponse>;

    delete(reasonId: number): Promise<HttpResponse>;
}

export const reasonService: ReasonService = {
    getReasons: async (structureId: string, reasonTypeId?: Number): Promise<Reason[]> => {
        try {
            let reasonTypeIdParam: string = reasonTypeId != null && reasonTypeId != undefined ? `&reasonTypeId=${reasonTypeId}` : '';
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

    create: async (reasonBody: ReasonRequest, reasonTypeId?: Number): Promise<HttpResponse> => {
        let reasonTypeIdParam: string = reasonTypeId ? `?reasonTypeId=${reasonTypeId}` : '';
        return http.post(`/presences/reason${reasonTypeIdParam}`, reasonBody);
    },

    update: async (reasonBody: ReasonRequest): Promise<HttpResponse> => {
        return http.put(`/presences/reason`, reasonBody);
    },

    delete: async (reasonId: number): Promise<HttpResponse> => {
        return http.delete(`/presences/reason?id=${reasonId}`);
    },
};

export const ReasonService = ng.service('ReasonService', (): ReasonService => reasonService);
