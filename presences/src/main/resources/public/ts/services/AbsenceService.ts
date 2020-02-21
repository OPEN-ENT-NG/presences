import {ng} from 'entcore';
import {Mix} from 'entcore-toolkit';
import http from 'axios';
import {CounsellorAbsence} from '../models'

export interface AbsenceService {
    retrieve(structure: string, students: string[], start: string, end: string, justified: boolean, regularized: boolean, reasons: number[]): Promise<CounsellorAbsence[]>;
}

export const absenceService: AbsenceService = {
    async retrieve(structure: string, students: string[], start: string, end: string, justified: boolean, regularized: boolean, reasons: number[]): Promise<CounsellorAbsence[]> {
        try {
            let url = `/presences/absences?structure=${structure}&start=${start}&end=${end}`;
            if (justified !== null) url += `&justified=${justified}`;
            if (regularized !== null) url += `&regularized=${regularized}`;
            if (students && students.length > 0) students.forEach(student => url += `&student=${student}`);
            if (reasons && reasons.length > 0) reasons.forEach(reason => url += `&reason=${reason}`);
            const {data} = await http.get(url);
            return Mix.castArrayAs(CounsellorAbsence, data);
        } catch (err) {
            throw err;
        }
    }
};

export const AbsenceService = ng.service('AbsenceService', (): AbsenceService => absenceService);