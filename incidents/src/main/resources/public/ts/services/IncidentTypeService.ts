import {ng} from 'entcore'
import { http, HttpResponse } from 'entcore-toolkit';

export interface IncidentType {
    id: number;
    structure_id?: string;
    label: string;
    hidden: boolean;
    used?: boolean;
}

export interface IncidentTypeRequest {
    id?: number;
    structureId?: string,
    label?: string;
    hidden?: boolean;
}


export interface IncidentsTypeService {
    get(structureId: string): Promise<IncidentType[]>;
    create(incidentsTypeBody: IncidentTypeRequest): Promise<HttpResponse>;
    update(incidentsTypeBody: IncidentTypeRequest): Promise<HttpResponse>;
    delete(reasonId: number): Promise<HttpResponse>;
}

export const incidentsTypeService : IncidentsTypeService = {
    get: async (structureId: string): Promise<IncidentType[]> => {
        try {
            const {data} = await http.get(`/incidents/types?structureId=${structureId}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    create: async (incidentsTypeBody: IncidentTypeRequest): Promise<HttpResponse> => {
        return await http.post(`/incidents/type`, incidentsTypeBody);
    },

    update: async (incidentsTypeBody: IncidentTypeRequest): Promise<HttpResponse> => {
        return await http.put(`/incidents/type`, incidentsTypeBody);
    },

    delete: async (incidentsTypeId: number): Promise<HttpResponse> => {
        return await http.delete(`/incidents/type?id=${incidentsTypeId}`);
    },
};

export const IncidentsTypeService = ng.service('IncidentsTypeService', (): IncidentsTypeService => incidentsTypeService);
