import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {IStatementAbsenceBody, IStatementsAbsencesRequest, IStatementsAbsencesResponse} from "../models";

export interface IStatementsAbsencesService {
    get(statementsAbsencesRequest: IStatementsAbsencesRequest): Promise<IStatementsAbsencesResponse>;

    create(statementsAbsencesBody: IStatementAbsenceBody): Promise<AxiosResponse>;

    update(statementsAbsencesId: number, statementsAbsencesBody: IStatementAbsenceBody): Promise<AxiosResponse>;

    delete(statementsAbsencesId: number): Promise<AxiosResponse>;
}

export const statementsAbsencesService: IStatementsAbsencesService = {
    get: async (statementsAbsences: IStatementsAbsencesRequest): Promise<IStatementsAbsencesResponse> => {
        try {
            const urlParams = `?studentId=${statementsAbsences.student_ids}
            &start_at=${statementsAbsences.start_at}&end_at=${statementsAbsences.end_at}`;
            const {data} = await http.get(`/presences/statements/absences${urlParams}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    create: async (statementsAbsences: IStatementAbsenceBody): Promise<AxiosResponse> => {
        const formData: FormData = new FormData();
        const headers = {'headers': {'Content-type': 'multipart/form-data'}};

        formData.append('structure_id', statementsAbsences.structure_id);
        formData.append('student_id', statementsAbsences.student_id);
        formData.append('start_at', statementsAbsences.start_at);
        formData.append('end_at', statementsAbsences.end_at);
        formData.append('description', statementsAbsences.description);
        formData.append('file', statementsAbsences.file, statementsAbsences.file.name);

        return http.post(`/presences/statements/absences`, formData, headers);
    },

    update: async (statementsAbsencesId: number, statementsAbsences: IStatementAbsenceBody): Promise<AxiosResponse> => {
        return http.put(`/presences/statements/absences/${statementsAbsencesId}`, statementsAbsences);
    },

    delete: async (statementsAbsencesId: number): Promise<AxiosResponse> => {
        return http.delete(`/presences/statements/absences/${statementsAbsencesId}`);
    }
};

export const StatementsAbsencesService = ng.service('StatementsAbsencesService',
    (): IStatementsAbsencesService => statementsAbsencesService);
