import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {Presence, PresenceBody, PresenceRequest} from "../models";

export interface PresenceService {
    get(presenceRequest: PresenceRequest): Promise<Presence[]>;

    create(presenceBody: PresenceBody): Promise<AxiosResponse>;

    update(presenceBody: PresenceBody): Promise<AxiosResponse>;

    delete(presenceId: number): Promise<AxiosResponse>;
}

export const presenceService: PresenceService = {
    get: async (presenceRequest: PresenceRequest): Promise<Presence[]> => {
        try {

            let studentParams = '';
            if (presenceRequest.studentIds) {
                presenceRequest.studentIds.forEach(studentId => {
                    studentParams += `&studentId=${studentId}`;
                });
            }

            let ownerParams = '';
            if (presenceRequest.ownerIds) {
                presenceRequest.ownerIds.forEach(ownerId => {
                    ownerParams += `&ownerId=${ownerId}`;
                });
            }

            const structureUrl = `?structureId=${presenceRequest.structureId}`;
            const dateUrl = `&startDate=${presenceRequest.startDate}&endDate=${presenceRequest.endDate}`;
            const urlParams = `${studentParams}${ownerParams}`;
            const {data} = await http.get(`/presences/presences${structureUrl}${dateUrl}${urlParams}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    create: async (presenceBody: PresenceBody): Promise<AxiosResponse> => {
        return http.post(`/presences/presence`, presenceBody);
    },

    update: async (presenceBody: PresenceBody): Promise<AxiosResponse> => {
        return http.put(`/presences/presence`, presenceBody);
    },

    delete: async (PresenceId: number): Promise<AxiosResponse> => {
        return http.delete(`/presences/presence?id=${PresenceId}`);
    },
};

export const PresenceService = ng.service('PresenceService', (): PresenceService => presenceService);
