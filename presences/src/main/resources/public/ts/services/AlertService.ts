import {ng} from 'entcore';
import http from 'axios';

export interface Alert {
    absence?: number
    lateness?: number
    incident?: number
    forgotten_notebook?: number
    alertType?: string
    students: any;
    classes: any;
    userId: string;
}

export interface AlertService {
    getAlerts(structureId: string): Promise<Alert>;

    getStudentsAlerts(structureId: string, type: string[]): Promise<Array<Alert>>;

    reset(alerts: Array<number>): Promise<void>;
}

export const alertService: AlertService = {
    async reset(alerts: Array<number>): Promise<void> {
        try {
            let url = `/presences/alerts?`;
            alerts.forEach(alert => {
                url += `&id=${alert}`;
            });

            const {data} = await http.delete(url);
            return data;
        } catch (e) {
            throw e;
        }
    },

    async getAlerts(structureId: string): Promise<Alert> {
        try {
            const {data} = await http.get(`/presences/structures/${structureId}/alerts/summary`);
            return data;
        } catch (e) {
            throw e;
        }
    },

    async getStudentsAlerts(structureId: string, types: string[]): Promise<Array<Alert>> {
        try {
            let url = `/presences/structures/${structureId}/alerts?`;
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