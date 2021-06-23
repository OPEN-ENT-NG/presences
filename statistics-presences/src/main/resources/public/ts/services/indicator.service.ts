import {ng} from 'entcore'
import {GlobalResponse} from "@statistics/model/Global";
import {IMonthly, IMonthlyGraph} from "@statistics/model/Monthly";
import http, {AxiosResponse} from "axios";
import {IndicatorBody} from "../model/Indicator";

export interface IndicatorService {
    fetchIndicator(structureId: string, indicatorName: string, page: number, body: IndicatorBody): Promise<GlobalResponse | IMonthly>;

    fetchGraphIndicator(structureId: string, indicatorName: string, body: IndicatorBody): Promise<IMonthlyGraph>;
}

export const indicatorService: IndicatorService = {
    fetchIndicator: async (structure: string, name: string, page: number, body: IndicatorBody): Promise<GlobalResponse | IMonthly> => {
        return http.post(`/statistics-presences/structures/${structure}/indicators/${name}?page=${page}`, body)
            .then((res: AxiosResponse) => res.data);
    },

    fetchGraphIndicator: async (structureId: string, indicatorName: string, body: IndicatorBody): Promise<IMonthlyGraph> => {
        return http.post(`/statistics-presences/structures/${structureId}/indicators/${indicatorName}/graph`, body)
            .then((res: AxiosResponse) => res.data);
    }

};
export const IndicatorService = ng.service('IndicatorService', (): IndicatorService => indicatorService);