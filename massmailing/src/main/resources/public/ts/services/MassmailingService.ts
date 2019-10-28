import {ng} from 'entcore';
import http from 'axios';
import {MassmailingStatusResponse} from '../model';
import {DateUtils} from "@common/utils";

export interface MassmailingService {
    getStatus(structure: string, massmailed: boolean, reasons: Array<number>, start_at: number, start_date: Date, end_date: Date, groups: Array<string>, students: Array<string>, types: Array<String>): Promise<MassmailingStatusResponse>;
}

export const MassmailingService = ng.service('MassmailingService', (): MassmailingService => {
    return {
        getStatus: async function (structure, massmailed, reasons, start_at = 1, start_date, end_date, groups, students, types): Promise<MassmailingStatusResponse> {
            try {
                const startDate: string = DateUtils.format(start_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                const endDate: string = DateUtils.format(end_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]);

                let url = `/massmailing/massmailings/status?structure=${structure}&massmailed=${massmailed}&start_at=${start_at}&start_date=${startDate}&end_date=${endDate}`;
                const mapFilters = function (objects: Array<any>, parameter: string): string {
                    let filter = '';
                    objects.map((object) => filter += `${parameter}=${object}&`);
                    return filter.substr(0, filter.length - 1);
                };

                if (reasons.length > 0) url += `&${mapFilters(reasons, 'reason')}`;
                if (groups.length > 0) url += `&${mapFilters(groups, 'group')}`;
                if (students.length > 0) url += `&${mapFilters(students, 'student')}`;
                if (types.length > 0) url += `&${mapFilters(types, 'type')}`;

                const {data} = await http.get(url);
                return data;
            } catch (e) {
                throw e;
            }
        }
    }
});
