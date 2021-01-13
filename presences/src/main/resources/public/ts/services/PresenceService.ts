import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {Presence, PresenceBody, PresenceRequest} from "../models";
import {Mix} from "entcore-toolkit";

export interface PresenceService {
    get(presenceRequest: PresenceRequest): Promise<Presence[]>;

    create(presenceBody: PresenceBody): Promise<AxiosResponse>;

    update(presenceBody: PresenceBody): Promise<AxiosResponse>;

    delete(presenceId: number): Promise<AxiosResponse>;

    exportCSV(punishmentRequest: PresenceRequest): Promise<void>;

}

function filterUrl(presenceRequest: PresenceRequest): string {
    let studentParams: string = '';
    if (presenceRequest.studentIds) {
        presenceRequest.studentIds.forEach(studentId => {
            studentParams += `&studentId=${studentId}`;
        });
    }

    let ownerParams: string = '';
    if (presenceRequest.ownerIds) {
        presenceRequest.ownerIds.forEach(ownerId => {
            ownerParams += `&ownerId=${ownerId}`;
        });
    }

    const structureUrl: string = `?structureId=${presenceRequest.structureId}`;
    const dateUrl: string = `&startDate=${presenceRequest.startDate}&endDate=${presenceRequest.endDate}`;
    const urlParams: string = `${studentParams}${ownerParams}`;
    return `${structureUrl}${dateUrl}${urlParams}`;
}

export const presenceService: PresenceService = {
    get: async (presenceRequest: PresenceRequest): Promise<Presence[]> => {
        try {
            const {data}: AxiosResponse = await http.get(`/presences/presences${filterUrl(presenceRequest)}`);
            return data
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

    /**
     * Export the punishments list as CSV format.
     * @param presenceRequest the presences get request.
     */
    exportCSV: async (presenceRequest: PresenceRequest): Promise<void> => {
        try {
            window.open(`/presences/presences/export${filterUrl(presenceRequest)}`);
        } catch (err) {
            throw err;
        }
    }
};

export const PresenceService = ng.service('PresenceService', (): PresenceService => presenceService);
