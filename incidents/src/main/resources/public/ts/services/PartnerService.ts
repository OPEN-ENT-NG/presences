import {ng} from 'entcore'
import { http, HttpResponse } from 'entcore-toolkit';

export interface Partner {
    id: number;
    structure_id?: string;
    label: string;
    hidden: boolean;
    used?: boolean;
}

export interface PartnerRequest {
    id?: number;
    structureId?: string,
    label?: string;
    hidden?: boolean;
}


export interface PartnerService {
    get(structureId: string): Promise<Partner[]>;
    create(partnerBody: PartnerRequest): Promise<HttpResponse>;
    update(partnerBody: PartnerRequest): Promise<HttpResponse>;
    delete(partnerId: number): Promise<HttpResponse>;
}

export const partnerService : PartnerService = {
    get: async (structureId: string): Promise<Partner[]> => {
        try {
            const {data} = await http.get(`/incidents/partners?structureId=${structureId}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    create: async (partnerBody: PartnerRequest): Promise<HttpResponse> => {
        return await http.post(`/incidents/partner`, partnerBody);
    },

    update: async (partnerBody: PartnerRequest): Promise<HttpResponse> => {
        return await http.put(`/incidents/partner`, partnerBody);
    },

    delete: async (partnerId: number): Promise<HttpResponse> => {
        return await http.delete(`/incidents/partner?id=${partnerId}`);
    },
};

export const PartnerService = ng.service('PartnerService', (): PartnerService => partnerService);
