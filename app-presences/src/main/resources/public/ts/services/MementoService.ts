import http, {AxiosResponse} from 'axios';
import {IndicatorBody} from "@statistics/model/Indicator";
import {IMonthlyGraph} from "@statistics/model/Monthly";
import {GlobalResponse} from "@statistics/model/Global";

export interface IMementoService {
    /**
     * Retrieve event summary graph based on given data
     *
     * @param structure structure identifier
     * @param student student identifier
     * @param body body identifier
     */
    getStudentEventsSummaryGraph(structure: string, student: string, body: IndicatorBody): Promise<IMonthlyGraph>;

    /**
     * Retrieve event summary based on given data
     *
     * @param structure structure identifier
     * @param student student identifier
     * @param body body identifier
     */
    getStudentEventsSummary(structure: string, student: string, body: IndicatorBody): Promise<GlobalResponse>;
}

export const MementoService: IMementoService = {
    async getStudentEventsSummaryGraph(structure: string, student: string, body: IndicatorBody): Promise<IMonthlyGraph> {
        return http.post(`/presences/statistics/structures/${structure}/student/${student}/graph`, body)
            .then((res: AxiosResponse) => (<IMonthlyGraph>res.data));
    },

    async getStudentEventsSummary(structure: string, student: string, body: IndicatorBody): Promise<GlobalResponse> {
        return http.post(`/presences/statistics/structures/${structure}/student/${student}`, body)
            .then((res: AxiosResponse) => (<GlobalResponse>res.data));
    }
}