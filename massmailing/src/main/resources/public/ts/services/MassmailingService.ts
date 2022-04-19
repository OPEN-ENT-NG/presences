import {ng} from 'entcore';
import http from 'axios';
import {Massmailing, MassmailingAnomaliesResponse, MassmailingStatusResponse} from '../model';
import {DateUtils} from '@common/utils';

export interface MassmailingService {
    getStatus(structure: string, massmailed: boolean, reasons: Array<number>, punishmentTypes: Array<number>,
              sanctionsTypes: Array<number>, start_at: number, start_date: Date, end_date: Date, groups: Array<string>,
              students: Array<string>, types: Array<String>, noReasons: boolean, noLatenessReasons: boolean): Promise<MassmailingStatusResponse>;

    getAnomalies(structure: string, massmailed: boolean, reasons: Array<number>, punishmentTypes: Array<number>,
                 sanctionsTypes: Array<number>, start_at: number, start_date: Date, end_date: Date, groups: Array<string>,
                 students: Array<string>, types: Array<String>, noReasons: boolean, noLatenessReasons: boolean): Promise<MassmailingAnomaliesResponse>;

    prefetch(mailType: string, structure: string, massmailed: boolean, reasons: Array<number>, punishmentTypes: Array<number>,
             sanctionsTypes: Array<number>, start_at: number, start_date: Date, end_date: Date, groups: Array<string>,
             studentList: Array<string>, types: Array<String>, noReasons: boolean, noLatenessReasons: boolean): Promise<Massmailing>;
}

function formatParameters(url: string, structure: string, massmailed: boolean, reasons: Array<number>,
                          punishmentTypes: Array<number>, sanctionsTypes: Array<number>, start_at: number,
                          start_date: Date, end_date: Date, groups: Array<string>, students: Array<string>,
                          types: Array<String>, noReasons: boolean, noLatenessReasons: boolean): string {
    const startDate: string = DateUtils.format(start_date, DateUtils.FORMAT['YEAR-MONTH-DAY']);
    const endDate: string = DateUtils.format(end_date, DateUtils.FORMAT['YEAR-MONTH-DAY']);

    let address: string = `${url}?structure=${structure}&start_at=${start_at}&start_date=${startDate}&end_date=${endDate}&no_reasons=${noReasons}&no_lateness_reasons=${noLatenessReasons}`;
    const mapFilters = (objects: Array<any>, parameter: string): string => {
        let filter: string = '';
        objects.map((object) => filter += `${parameter}=${object}&`);
        return filter.substr(0, filter.length - 1);
    };

    if (reasons.length > 0) address += `&${mapFilters(reasons, 'reason')}`;
    if (punishmentTypes.length > 0) address += `&${mapFilters(punishmentTypes, 'punishmentType')}`;
    if (sanctionsTypes.length > 0) address += `&${mapFilters(sanctionsTypes, 'sanctionType')}`;
    if (groups.length > 0) address += `&${mapFilters(groups, 'group')}`;
    if (students.length > 0) address += `&${mapFilters(students, 'student')}`;
    if (types.length > 0) address += `&${mapFilters(types, 'type')}`;
    if (massmailed !== null) address += `&massmailed=${massmailed}`;

    return address;
}

export const massmailingService: MassmailingService = {
    getStatus: async function (structure: string, massmailed: boolean, reasons: number[], punishmentTypes: Array<number>,
                               sanctionsTypes: Array<number>, start_at: number = 1, start_date: Date, end_date: Date,
                               groups: string[], students: string[], types: Array<String>, noReasons: boolean, noLatenessReasons: boolean): Promise<MassmailingStatusResponse> {
        try {
            const url: string = formatParameters('/massmailing/massmailings/status', structure, massmailed, reasons, punishmentTypes,
                sanctionsTypes, start_at, start_date, end_date, groups, students, types, noReasons, noLatenessReasons);
            const {data} = await http.get(url);
            return data;
        } catch (e) {
            throw e;
        }
    },

    getAnomalies: async function (structure: string, massmailed: boolean, reasons: number[], punishmentTypes: Array<number>,
                                  sanctionsTypes: Array<number>, start_at: number = 1, start_date: Date, end_date: Date,
                                  groups: string[], students: string[], types: Array<String>, noReasons: boolean, noLatenessReasons: boolean): Promise<MassmailingAnomaliesResponse> {
        try {
            const url: string = formatParameters('/massmailing/massmailings/anomalies', structure, massmailed, reasons, punishmentTypes,
                sanctionsTypes, start_at, start_date, end_date, groups, students, types, noReasons, noLatenessReasons);
            const {data} = await http.get(url);
            return data;
        } catch (e) {
            throw e;
        }
    },

    prefetch: async function (mailType: string, structure: string, massmailed: boolean, reasons: number[], punishmentTypes: Array<number>,
                              sanctionsTypes: Array<number>, start_at: number = 1, start_date: Date, end_date: Date, groups: string[],
                              studentList: string[], types: Array<String>, noReasons: boolean, noLatenessReasons: boolean): Promise<Massmailing> {
        try {
            const url: string = formatParameters(`/massmailing/massmailings/prefetch/${mailType}`, structure, massmailed,
                reasons, punishmentTypes, sanctionsTypes, start_at, start_date, end_date, groups, studentList, types, noReasons, noLatenessReasons);
            const {data} = await http.get(url);
            const {type, students, counts} = data;
            return new Massmailing(type, counts, students);
        } catch (e) {
            throw e;
        }
    }
};

export const MassmailingService = ng.service('MassmailingService', (): MassmailingService => massmailingService);