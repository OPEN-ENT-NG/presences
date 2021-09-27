import {ng} from 'entcore';
import {Mix} from 'entcore-toolkit';
import http, {AxiosResponse} from 'axios';
import {Absence, CounsellorAbsence} from '../models'

export interface AbsenceService {
    getAbsence(structure: string, students: string[], start: string, end: string, justified: boolean, regularized: boolean, reasons: number[]): Promise<Absence[]>;
    getCounsellorAbsence(structure: string, students: string[], groups: string[], start: string, end: string, justified: boolean, regularized: boolean, reasons: number[]): Promise<CounsellorAbsence[]>;
    updateFollowed(ids: number[], followed: boolean): Promise<AxiosResponse>;
}

async function retrieve(structure: string, students: string[], groups: string[], start: string, end: string, justified: boolean, regularized: boolean, reasons: number[]): Promise<any[]> {
    try {
        let url: string = `/presences/absences?structure=${structure}&start=${start}&end=${end}`;
        if (justified !== null) url += `&justified=${justified}`;
        if (regularized !== null) url += `&regularized=${regularized}`;
        if (students && students.length > 0) students.forEach(student => url += `&student=${student}`);
        if (groups && groups.length > 0) groups.forEach(group => url += `&classes=${group}`);
        if (reasons && reasons.length > 0) reasons.forEach(reason => url += `&reason=${reason}`);
        const {data}:AxiosResponse = await http.get(url);
        return data;
    } catch (err) {
        throw err;
    }
}

export const absenceService: AbsenceService = {
    async getAbsence(structure: string, students: string[], start: string, end: string, justified: boolean, regularized: boolean, reasons: number[]): Promise<Absence[]> {
        return Mix.castArrayAs(Absence, await retrieve(structure, students, [], start, end, justified, regularized, reasons));
    },

    async getCounsellorAbsence(structure: string, students: string[], groups: string[],
                               start: string, end: string, justified: boolean, regularized: boolean,
                               reasons: number[]): Promise<CounsellorAbsence[]> {
        return Mix.castArrayAs(CounsellorAbsence, await retrieve(structure, students, groups, start, end,
            justified, regularized, reasons)).filter((absence: CounsellorAbsence) => absence.student.name != null);
    },

    async updateFollowed(ids: number[], followed: boolean): Promise<AxiosResponse> {
        return http.put(`/presences/absences/follow`, {absenceIds: ids, followed: followed});
    }
};

export const AbsenceService = ng.service('AbsenceService', (): AbsenceService => absenceService);