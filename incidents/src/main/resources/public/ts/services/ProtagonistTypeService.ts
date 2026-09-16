import {ng} from 'entcore'
import { http, HttpResponse } from 'entcore-toolkit';

export interface ProtagonistType {
    id: number;
    structureId: string;
    structure_id?: string;
    label: string;
    hidden: boolean;
    used?: boolean;
}

export interface ProtagonistTypeRequest {
    id?: number;
    structureId?: string,
    label?: string;
    hidden?: boolean;
}

export interface ProtagonistTypeService {
    get(structureId: string): Promise<ProtagonistType[]>;
    create(protagonistTypeBody: ProtagonistTypeRequest): Promise<HttpResponse>;
    update(protagonistTypeBody: ProtagonistTypeRequest): Promise<HttpResponse>;
    delete(protagonistTypeId: number): Promise<HttpResponse>;
}

export const protagonistTypeService : ProtagonistTypeService = {
    get: async (structureId: string): Promise<ProtagonistType[]> => {
        try {
            const {data} = await http.get(`/incidents/protagonists/type?structureId=${structureId}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    create: async (protagonistTypeBody: ProtagonistTypeRequest): Promise<HttpResponse> => {
        return await http.post(`/incidents/protagonist/type`, protagonistTypeBody);
    },

    update: async (protagonistTypeBody: ProtagonistTypeRequest): Promise<HttpResponse> => {
        return await http.put(`/incidents/protagonist/type`, protagonistTypeBody);
    },

    delete: async (protagonistTypeId: number): Promise<HttpResponse> => {
        return await http.delete(`/incidents/protagonist/type?id=${protagonistTypeId}`);
    },
};

export const ProtagonistTypeService = ng.service('ProtagonistTypeService', (): ProtagonistTypeService => protagonistTypeService);
