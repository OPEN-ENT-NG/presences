import {ng} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {
    ICollectiveAbsence, ICollectiveAbsenceBody,
    ICollectiveAbsencesResponse,
    ICollectiveAbsenceStudent
} from '../models';

export interface CollectiveAbsenceService {

    getCollectiveAbsence(structureId: string, collectiveId: number): Promise<ICollectiveAbsence>;

    getCollectiveAbsences(structureId: string, params: ICollectiveAbsenceBody): Promise<ICollectiveAbsencesResponse>;

    createCollectiveAbsence(structureId: string, collectiveAbsence: ICollectiveAbsence): Promise<AxiosResponse>;

    updateCollectiveAbsence(structureId: string, collectiveAbsence: ICollectiveAbsence): Promise<AxiosResponse>;

    removeAbsenceFromCollectiveAbsence(structureId: string, collectiveId: number, studentIds: Array<string>): Promise<AxiosResponse>;

    deleteCollectiveAbsence(structureId: string, collectiveId: number): Promise<AxiosResponse>;

    exportCollectiveAbsences(structureId: string, startDate: string, endDate: string): void;

    getStudentsAbsencesStatus(structureId: string, params: ICollectiveAbsenceBody): Promise<Array<ICollectiveAbsenceStudent>>;
}

export const collectiveAbsenceService: CollectiveAbsenceService = {

    /**
     * Get collective absence from id.
     * @param structureId       structure identifier
     * @param collectiveId      collective absence identifier
     */
    getCollectiveAbsence : async (structureId: string, collectiveId: number): Promise<ICollectiveAbsence> => {

        return http.get(`/presences/structures/${structureId}/absences/collectives/${collectiveId}`)
            .then((res: AxiosResponse) => { return res.data; });
    },

    /**
     * Get list of collective absences.
     * @param structureId  structure identifier
     * @param params       collective absences filter parameters
     */
    getCollectiveAbsences : async (structureId: string, params: ICollectiveAbsenceBody): Promise<ICollectiveAbsencesResponse> => {
        let urlParams: string = `?startDate=${params.startDate}&endDate=${params.endDate}`;

        if (params.groups.length > 0) {
           for (let i = 0; i < params.groups.length; i++) {
               urlParams += `&audienceName=${params.groups[i]}`;
           }
        }

        urlParams += (params.page !== undefined && params.page !== null)  ? `&page=${params.page}` : '';

        return http.get(`/presences/structures/${structureId}/absences/collectives${urlParams}`)
            .then((res: AxiosResponse) => {
                return res.data;
            });
    },

    /**
     * Create collective absence.
     * @param structureId           structure identifier
     * @param collectiveAbsence     collective absence to create
     */
    createCollectiveAbsence : async (structureId: string, collectiveAbsence: ICollectiveAbsence): Promise<AxiosResponse> => {
        return http.post(`/presences/structures/${structureId}/absences/collectives`, collectiveAbsence);
    },

    /**
     * Update collective absence.
     * @param structureId           structure identifier
     * @param collectiveAbsence     collective absence to update
     */
    updateCollectiveAbsence : async (structureId: string, collectiveAbsence: ICollectiveAbsence): Promise<AxiosResponse> => {
        return http.put(`/presences/structures/${structureId}/absences/collectives/${collectiveAbsence.id}`, collectiveAbsence);
    },

    /**
     * Remove an absence from the collective absence.
     * @param structureId       structure identifier
     * @param collectiveId      collective absence identifier
     * @param studentIds        list of student identifiers
     */
    removeAbsenceFromCollectiveAbsence : async (structureId: string, collectiveId: number, studentIds: Array<string>): Promise<AxiosResponse> => {
        const students: {studentIds: Array<string>} = {
            studentIds: studentIds
        };

        return http.put(`/presences/structures/${structureId}/absences/collectives/${collectiveId}/students`, students);
    },

    /**
     * Delete collective absence.
     * @param structureId       structure identifier
     * @param collectiveId      collective absence identifier
     */
    deleteCollectiveAbsence : async (structureId: string, collectiveId: number): Promise<AxiosResponse> => {
        return http.delete(`/presences/structures/${structureId}/absences/collectives/${collectiveId}`);
    },

    /**
     * Export collective absences in CSV file.
     * @param structureId       structure identifier
     * @param startDate         start date range
     * @param endDate           end date range
     */
    exportCollectiveAbsences : (structureId: string, startDate: string, endDate: string): void => {
        window.open(`/presences/structures/${structureId}/absences/collectives/export?startDate=${startDate}&endDate=${endDate}`);
    },

    /**
     * Get list of student absences status
     * @param structureId       structure identifier
     * @param body              collective absences parameters
     */
    getStudentsAbsencesStatus : async (structureId: string, body: ICollectiveAbsenceBody): Promise<Array<ICollectiveAbsenceStudent>> => {
        return http.post(`/presences/structures/${structureId}/absences/isAbsent`, body)
            .then((res: AxiosResponse) => { return res.data.all; });
    }
};

export const CollectiveAbsenceService = ng.service('CollectiveAbsenceService',
    (): CollectiveAbsenceService => collectiveAbsenceService);