import {ng, notify} from 'entcore';
import {Mix} from 'entcore-toolkit';
import http, {AxiosResponse} from 'axios';
import {Absence, CounsellorAbsence} from '../models';
import {EXPORT_TYPE, ExportType} from "@common/core/enum/export-type.enum";
import {EventAbsenceSummary} from "@presences/models/Event/EventAbsenceSummary";

export interface AbsenceService {
    getAbsencesPaginated(structure: string, students: Array<string>, groups: Array<string>, start: string, end: string, justified: boolean,
                         regularized: boolean, followed: boolean, reasons: Array<number>, noReason: boolean, halfBoarder: boolean,
                         internal: boolean, page: number): Promise<Array<Absence>>;
    getAbsence(structure: string, students: Array<string>, groups: Array<string>, start: string, end: string, justified: boolean,
               regularized: boolean, reasons: Array<number>): Promise<Array<Absence>>;
    getCounsellorAbsence(structure: string, students: string[], groups: string[], start: string, end: string, justified: boolean, regularized: boolean, reasons: number[]): Promise<CounsellorAbsence[]>;
    updateFollowed(ids: number[], followed: boolean): Promise<AxiosResponse>;
    regularizeAbsences(ids: Array<number>, regularized: boolean): Promise<AxiosResponse>;
    export(exportType: ExportType, structureId: string, startDate: string, endDate: string,
           studentIds: Array<string>, audienceIds: Array<string>, regularized: boolean,
           followed: boolean, reasons: Array<number>, noReason: boolean, halfBoarder: boolean, internal: boolean): void;
    getAbsenceMarkers(structureId: string, startAt: string, endAt: string): Promise<EventAbsenceSummary>;
}

async function retrieve(structure: string, students: Array<string>, groups: Array<string>, start: string, end: string,
                        justified: boolean, regularized: boolean, reasons: Array<number>, noReason? : boolean, followed?: boolean, halfBoarder?: boolean,
                        internal?: boolean, page?: number): Promise<any> {
    try {
        let url: string = `/presences/absences?structure=${structure}&`;
        url += getUriFilterAbsences(students, groups, start, end, justified, regularized, reasons, noReason,
            followed, halfBoarder, internal);
        if (page || page === 0) url += `&page=${page}`;

        const {data}: AxiosResponse = await http.get(url);
        return data;
    } catch (err) {
        throw err;
    }
}

function getUriFilterAbsences(students: Array<string>, groups: Array<string>, start: string, end: string,
                              justified: boolean, regularized: boolean, reasons: Array<number>, noReason? : boolean, followed?: boolean, halfBoarder?: boolean,
                              internal?: boolean): String {
    let uri: string = `start=${start}&end=${end}`;
    if (justified !== null) uri += `&justified=${justified}`;
    if (regularized !== null) uri += `&regularized=${regularized}`;
    if (students && students.length > 0) students.forEach(student => uri += `&student=${student}`);
    if (groups && groups.length > 0) groups.forEach(group => uri += `&classes=${group}`);
    if (reasons && reasons.length > 0) reasons.forEach(reason => uri += `&reason=${reason}`);
    if (noReason !== null) uri += `&noReason=${noReason}`;
    if (followed !== null) uri += `&followed=${followed}`;
    if (halfBoarder) uri += `&halfBoarder=true`;
    if (internal) uri += `&internal=true`;

    return uri;
}

export const absenceService: AbsenceService = {
    async getAbsencesPaginated(structure: string, students: Array<string>, groups: Array<string>, start: string, end: string, justified: boolean,
                               regularized: boolean, followed: boolean, reasons: Array<number>, noReason: boolean, halfBoarder: boolean,
                               internal: boolean, page: number): Promise<Array<Absence>> {
        return retrieve(structure, students, groups, start, end, justified, regularized, reasons, noReason,
            followed, halfBoarder, internal, page)
            .then((res) => { return Mix.castArrayAs(Absence, (<{all: Array<any>}> res).all); });
    },

    async getAbsence(structure: string, students: Array<string>, groups: Array<string>, start: string, end: string, justified: boolean,
                     regularized: boolean, reasons: Array<number>): Promise<Array<Absence>> {
        return Mix.castArrayAs(Absence, await retrieve(structure, students, groups,
            start, end, justified, regularized, reasons));
    },

    async getCounsellorAbsence(structure: string, students: string[], groups: string[],
                               start: string, end: string, justified: boolean, regularized: boolean,
                               reasons: number[]): Promise<CounsellorAbsence[]> {
        return Mix.castArrayAs(CounsellorAbsence, await retrieve(structure, students, groups, start, end,
            justified, regularized, reasons)).filter((absence: CounsellorAbsence) => absence.student.name != null);
    },

    async updateFollowed(ids: number[], followed: boolean): Promise<AxiosResponse> {
        return http.put(`/presences/absences/follow`, {absenceIds: ids, followed: followed});
    },

    async regularizeAbsences(ids: Array<number>, regularized: boolean): Promise<AxiosResponse> {
        if (ids.length === 0) return;
        try {
            await http.put(`/presences/absence/regularized`, {ids: ids, regularized: regularized});
        } catch (err) {
            notify.error('presences.absences.update_regularized.error');
        }
    },


    export(exportType: ExportType, structureId: string, startDate: string, endDate: string,
           studentIds: Array<string>, audienceIds: Array<string>, regularized: boolean,
           followed: boolean, reasons: Array<number>, noReason: boolean, halfBoarder: boolean, internal: boolean): void {
        try {
            let url: string = `/presences/structures/${structureId}/absences/export/`;

            switch (exportType) {
                case EXPORT_TYPE.PDF:
                    url += `pdf`;
                    break;
                case EXPORT_TYPE.CSV:
                    url += `csv`;
                    break;
            }

            url += '?' + getUriFilterAbsences(studentIds, audienceIds, startDate, endDate, null, regularized, reasons, noReason,
                followed, halfBoarder, internal);

            window.open(url);
        } catch (err) {
            throw err;
        }
    },

    /**
     * Get absence markers for students
     * @param structureId   structure identifier
     * @param startAt       start date filter
     * @param endAt         end date filter
     */
    async getAbsenceMarkers(structureId: string, startAt: string, endAt: string): Promise<EventAbsenceSummary> {
        try {
            let url: string = `/presences/structures/${structureId}/absences/markers`;

            if (startAt && endAt) {
                url += `?startAt=${startAt}&endAt=${endAt}`;
            }

            const {data}: AxiosResponse = await http.get(url);
            return data;
        } catch (err) {
            throw err;
        }
    }


};

export const AbsenceService = ng.service('AbsenceService', (): AbsenceService => absenceService);