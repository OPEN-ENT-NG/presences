import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';

export interface Place {
    id: number;
    structureId: string;
    label: string;
    hidden: boolean;
    used?: boolean;
}

export interface PlaceRequest {
    id?: number;
    structureId?: string,
    label?: string;
    hidden?: boolean;
}


export interface PlaceService {
    get(structureId: string): Promise<Place[]>;
    create(placeBody: PlaceRequest): Promise<AxiosResponse>;
    update(placeBody: PlaceRequest): Promise<AxiosResponse>;
    delete(placeId: number): Promise<AxiosResponse>;
}

export const placeService : PlaceService = {
    get: async (structureId: string): Promise<Place[]> => {
        try {
            const {data} = await http.get(`/incidents/places?structureId=${structureId}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    create: async (placeBody: PlaceRequest): Promise<AxiosResponse> => {
        return await http.post(`/incidents/place`, placeBody);
    },

    update: async (placeBody: PlaceRequest): Promise<AxiosResponse> => {
        return await http.put(`/incidents/place`, placeBody);
    },

    delete: async (placeId: number): Promise<AxiosResponse> => {
        return await http.delete(`/incidents/place?id=${placeId}`);
    },
};

export const PlaceService = ng.service('PlaceService', (): PlaceService => placeService);
