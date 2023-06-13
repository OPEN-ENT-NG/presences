import {ng} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {Alert, DeleteAlertRequest, InfiniteScrollAlert, StudentAlert} from "@presences/models/Alert";

export interface AlertService {
    getAlerts(structureId: string): Promise<Alert>;

    getStudentsAlerts(structureId: string, type: string[], students: string[], classes: string[], start_at: string, end_at: string, page?: number): Promise<InfiniteScrollAlert>;

    getStudentAlerts(structureId: string, studentId: string, type: string): Promise<{ count: number, threshold: number }>;

    reset(structureId: string, body: DeleteAlertRequest): Promise<void>;

    resetStudentAlertsCount(structureId: string, studentId: string, type: string): Promise<AxiosResponse>;

    exportCSV(structureId: string, type: string[]): void;
}

export const alertService: AlertService = {
    async reset(structureId: string, body: DeleteAlertRequest): Promise<void> {
        try {
            const url: string = `/presences/structures/${structureId}/alerts`;

            const {data} = await http.delete(url, {data: body});
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

    async getStudentsAlerts(structureId: string, types: string[], students: string[] = null, groups: string[], start_at: string, end_at: string, page?: number): Promise<InfiniteScrollAlert> {
        try {
            let studentsFilter: string = '';
            let groupsFilter: string = '';
            let dateFilter: string = '';
            let pageFilter: string = page != null ? `page=${page}&` : '';
            if (students && students.length > 0) {
                students.map((student) => studentsFilter += `student=${student}&`);
            }

            if (groups && groups.length > 0) {
                groups.map((group) => groupsFilter += `class=${group}&`);
            }

            if (start_at && end_at) {
                dateFilter = `start_at=${start_at}&end_at=${end_at}&`;
            }

            let url: string = `/presences/structures/${structureId}/alerts?${studentsFilter}${groupsFilter}${dateFilter}${pageFilter}`;
            types.forEach(type => {
                url += `&type=${type}`;
            });

            const {data} = await http.get(url);
            return data;
        } catch (e) {
            throw e;
        }
    },

    async getStudentAlerts(structureId: string, studentId: string, type: string): Promise<any> {
        try {
            let url: string = `/presences/structures/${structureId}/students/${studentId}/alerts?type=${type}`;
            const {data} = await http.get(url);
            return data;
        } catch (e) {
            throw e;
        }
    },

    resetStudentAlertsCount(structureId: string, studentId: string, type: string): Promise<AxiosResponse> {
        return http.delete(`/presences/structures/${structureId}/students/${studentId}/alerts/reset?type=${type}`);
    },

    exportCSV(structureId: string, types: string[]): void {
        let url: string = `/presences/structures/${structureId}/alerts/export?`;
        types.forEach(type => {
            url += `&type=${type}`;
        });

        window.open(url);
    }


};

export const AlertService = ng.service('AlertService', (): AlertService => alertService);