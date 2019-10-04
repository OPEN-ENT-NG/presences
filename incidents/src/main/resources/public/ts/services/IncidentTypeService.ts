import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';

export interface IncidentType {
    id: number;
    structureId: string;
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
    create(incidentsTypeBody: IncidentTypeRequest): Promise<AxiosResponse>;
    update(incidentsTypeBody: IncidentTypeRequest): Promise<AxiosResponse>;
    delete(reasonId: number): Promise<AxiosResponse>;
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

    create: async (incidentsTypeBody: IncidentTypeRequest): Promise<AxiosResponse> => {
        return await http.post(`/incidents/type`, incidentsTypeBody);
    },

    update: async (incidentsTypeBody: IncidentTypeRequest): Promise<AxiosResponse> => {
        return await http.put(`/incidents/type`, incidentsTypeBody);
    },

    delete: async (incidentsTypeId: number): Promise<AxiosResponse> => {
        return await http.delete(`/incidents/type?id=${incidentsTypeId}`);
    },
};

export const IncidentsTypeService = ng.service('IncidentsTypeService', (): IncidentsTypeService => incidentsTypeService);
