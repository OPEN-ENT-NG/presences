import {ng} from 'entcore'
import { http, HttpResponse } from 'entcore-toolkit';
import {Presence, PresenceBody, PresenceRequest} from "../models";

export interface PresenceService {
    get(presenceRequest: PresenceRequest): Promise<Presence[]>;

    create(presenceBody: PresenceBody): Promise<HttpResponse>;

    update(presenceBody: PresenceBody): Promise<HttpResponse>;

    delete(presenceId: number): Promise<HttpResponse>;

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

    let audienceParams: string = '';
    if (presenceRequest.audienceIds) {
        presenceRequest.audienceIds.forEach((audienceId: string) => {
            audienceParams += `&audienceId=${audienceId}`;
        });
    }

    const structureUrl: string = `?structureId=${presenceRequest.structureId}`;
    const dateUrl: string = `&startDate=${presenceRequest.startDate}&endDate=${presenceRequest.endDate}`;
    const urlParams: string = `${studentParams}${ownerParams}${audienceParams}`;
    return `${structureUrl}${dateUrl}${urlParams}`;
}

export const presenceService: PresenceService = {
    get: async (presenceRequest: PresenceRequest): Promise<Presence[]> => {
        try {
            const {data}: HttpResponse = await http.get(`/presences/presences${filterUrl(presenceRequest)}`);
            return data
        } catch (err) {
            throw err;
        }
    },

    create: async (presenceBody: PresenceBody): Promise<HttpResponse> => {
        return http.post(`/presences/presence`, presenceBody);
    },

    update: async (presenceBody: PresenceBody): Promise<HttpResponse> => {
        return http.put(`/presences/presence`, presenceBody);
    },

    delete: async (PresenceId: number): Promise<HttpResponse> => {
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
