import {ng, moment} from 'entcore'
import {GlobalResponse} from "@statistics/model/Global";
import {IMonthly, IMonthlyGraph} from "@statistics/model/Monthly";
import { http, HttpResponse } from 'entcore-toolkit';
import {IndicatorBody} from "../model/Indicator";
import {IWeekly, IWeeklyResponse, WeeklyStatistics} from "@statistics/model/Weekly";

export interface IndicatorService {
    fetchIndicator(structureId: string, indicatorName: string, page: number, body: IndicatorBody): Promise<GlobalResponse | IMonthly | IWeeklyResponse>;

    fetchGraphIndicator(structureId: string, indicatorName: string, body: IndicatorBody): Promise<IMonthlyGraph>;

    refreshStudentsStats(structureId: string, arrayStudentIds: Array<string>): Promise<HttpResponse>;
}

export const indicatorService: IndicatorService = {
    fetchIndicator: async (structure: string, name: string, page: number, body: IndicatorBody): Promise<GlobalResponse | IMonthly | IWeeklyResponse> => {
        return http.post(`/statistics-presences/structures/${structure}/indicators/${name}?page=${page}`, body)
            .then((res: HttpResponse) => res.data);
    },

    fetchGraphIndicator: async (structureId: string, indicatorName: string, body: IndicatorBody): Promise<IMonthlyGraph> => {
        return http.post(`/statistics-presences/structures/${structureId}/indicators/${indicatorName}/graph`, body)
            .then((res: HttpResponse) => res.data);
    },

    refreshStudentsStats(structureId: string, arrayStudentIds: Array<string>): Promise<HttpResponse> {
        return http.post(`/statistics-presences/structures/${structureId}/process/students/statistics/tasks`, {studentIds: arrayStudentIds})
    }

};
export const IndicatorService = ng.service('IndicatorService', (): IndicatorService => indicatorService);