import {ng} from 'entcore';
import http from 'axios';

export interface Alert {
    absence?: number
    lateness?: number
    incident?: number
    forgotten_notebook?: number
    alertType?: string
}

export interface AlertService {
    getAlerts(structureId: string): Promise<Alert>;

    getStudentsAlerts(structureId: string, type: string[]): Promise<Alert>;
}

export const alertService: AlertService = {
    async getAlerts(structureId: string): Promise<Alert> {
        try {
            const {data} = await http.get(`/presences/structures/${structureId}/alerts/summary`);
            return data;
        } catch (e) {
            throw e;
        }
    },

    async getStudentsAlerts(structureId: string, types: string[]): Promise<Alert> {
        try {
            let url = `/presences/structures/${structureId}/alerts?`;

            // let selectedTypes = {
            //     ABSENCE: true,
            //     LATENESS: true,
            //     INCIDENT: true,
            //     FORGOTTEN_NOTEBOOK: true
            // };
            //
            // let types = Object.keys(selectedTypes);
            // types.forEach(type => {
            //     if (selectedTypes[type]) {
            //         url += `type=${type}&`
            //     }
            // });

            types.forEach(type => {
                url += `&type=${type}`;
            });

            const {data} = await http.get(url);
            return data;
        } catch (e) {
            throw e;
        }
    }
};

export const AlertService = ng.service('SettingService', (): AlertService => alertService);