import {ng} from 'entcore';
import http from 'axios';

export interface Alert {
    absence?: number
    lateness?: number
    incident?: number
    forgotten_notebook?: number
}

export interface AlertService {
    get(structureId: string): Promise<Alert>;
}

export const alertService: AlertService = {
    async get(structureId: string): Promise<Alert> {
        try {
            const {data} = await http.get(`/presences/structures/${structureId}/alerts`);
            return data;
        } catch (e) {
            throw e;
        }
    }
};

export const AlertService = ng.service('SettingService', (): AlertService => alertService);