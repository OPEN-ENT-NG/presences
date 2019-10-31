import {moment, ng} from 'entcore';
import http from 'axios';
import {MassmailingAnomaliesResponse, MassmailingStatusResponse} from '../model';
import {DateUtils} from "@common/utils";

export interface MassmailingService {
    getStatus(structure: string, massmailed: boolean, reasons: Array<number>, start_at: number, start_date: Date, end_date: Date, groups: Array<string>, students: Array<string>, types: Array<String>, noReasons: boolean): Promise<MassmailingStatusResponse>;

    getAnomalies(structure: string, massmailed: boolean, reasons: Array<number>, start_at: number, start_date: Date, end_date: Date, groups: Array<string>, students: Array<string>, types: Array<String>, noReasons: boolean): Promise<MassmailingAnomaliesResponse>;
}

function formatParameters(url: string, structure: string, massmailed: boolean, reasons: Array<number>, start_at: number,
                          start_date: Date, end_date: Date, groups: Array<string>, students: Array<string>, types: Array<String>, noReasons: boolean): string {
    const startDate: string = DateUtils.format(start_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
    const endDate: string = DateUtils.format(moment(end_date).add(1, 'd'), DateUtils.FORMAT["YEAR-MONTH-DAY"]);

    let address = `${url}?structure=${structure}&start_at=${start_at}&start_date=${startDate}&end_date=${endDate}&no_reasons=${noReasons}`;
    const mapFilters = function (objects: Array<any>, parameter: string): string {
        let filter = '';
        objects.map((object) => filter += `${parameter}=${object}&`);
        return filter.substr(0, filter.length - 1);
    };

    if (reasons.length > 0) address += `&${mapFilters(reasons, 'reason')}`;
    if (groups.length > 0) address += `&${mapFilters(groups, 'group')}`;
    if (students.length > 0) address += `&${mapFilters(students, 'student')}`;
    if (types.length > 0) address += `&${mapFilters(types, 'type')}`;
    if (massmailed !== null) address += `&massmailed=${massmailed}`;

    return address;
}

export const MassmailingService = ng.service('MassmailingService', (): MassmailingService => {
    return {
        getStatus: async function (structure, massmailed, reasons, start_at = 1, start_date, end_date, groups, students, types, noReasons): Promise<MassmailingStatusResponse> {
            try {
                const url = formatParameters('/massmailing/massmailings/status', structure, massmailed, reasons, start_at, start_date, end_date, groups, students, types, noReasons);
                const {data} = await http.get(url);
                return data;
            } catch (e) {
                throw e;
            }
        },
        getAnomalies: async function (structure, massmailed, reasons, start_at = 1, start_date, end_date, groups, students, types, noReasons): Promise<MassmailingAnomaliesResponse> {
            try {
                const url = formatParameters('/massmailing/massmailings/anomalies', structure, massmailed, reasons, start_at, start_date, end_date, groups, students, types, noReasons);
                const {data} = await http.get(url);
                return data;
            } catch (e) {
                throw e;
            }
        }
    }
});
