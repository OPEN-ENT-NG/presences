import {ng} from 'entcore'
import { http, HttpResponse } from 'entcore-toolkit';
import {Action, ActionRequest} from "../models/Action";

export interface ActionService {
    getActions(structureId: string): Promise<Action[]>;

    // Settings
    create(actionBody: ActionRequest): Promise<HttpResponse>;

    update(actionBody: ActionRequest): Promise<HttpResponse>;

    delete(actionId: number): Promise<HttpResponse>;
}

export const actionService: ActionService = {
    getActions: async (structureId: string): Promise<Action[]> => {
        try {
            const {data} = await http.get(`/presences/actions?structureId=${structureId}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    // Settings
    create: (actionBody: ActionRequest): Promise<HttpResponse> => {
        return http.post(`/presences/action`, actionBody);
    },

    update: (actionBody: ActionRequest): Promise<HttpResponse> => {
        return http.put(`/presences/action`, actionBody);
    },

    delete: (actionId: number): Promise<HttpResponse> => {
        return http.delete(`/presences/action?id=${actionId}`);
    }
};
export const ActionService = ng.service('ActionService', (): ActionService => actionService);