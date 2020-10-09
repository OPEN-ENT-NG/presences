import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {
    IStatementAbsenceBody,
    IStatementsAbsences,
    IStatementsAbsencesRequest,
    IStatementsAbsencesResponse
} from "../models";

export interface IStatementsAbsencesService {
    baseUrl: string;

    get(statementsAbsencesRequest: IStatementsAbsencesRequest): Promise<IStatementsAbsencesResponse>;

    download(statementsAbsencesBody: IStatementAbsenceBody): void;

    create(statementsAbsencesBody: IStatementAbsenceBody): Promise<AxiosResponse>;

    update(statementsAbsencesId: number, statementsAbsencesBody: IStatementAbsenceBody): Promise<AxiosResponse>;

    validate(statementsAbsencesId: number, statementsAbsencesBody: IStatementAbsenceBody): Promise<AxiosResponse>;

    delete(statementsAbsencesId: number): Promise<AxiosResponse>;

    export(statementsAbsences: IStatementsAbsencesRequest): void;

    getUrl(statementsAbsences: IStatementsAbsencesRequest, page: number, limit: number, offset: number): String;
}

export const statementsAbsencesService: IStatementsAbsencesService = {
    baseUrl: '/presences/statements/absences',

    get: async (statementsAbsences: IStatementsAbsencesRequest): Promise<IStatementsAbsencesResponse> => {
        try {
            const urlParams: String = statementsAbsencesService.getUrl(statementsAbsences, statementsAbsences.page, statementsAbsences.limit, statementsAbsences.offset);
            const {data}: AxiosResponse = await http.get(`${statementsAbsencesService.baseUrl}${urlParams}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    download: async (statementsAbsences: IStatementsAbsences): Promise<void> => {
        const basicUrl: string = `/presences/statements/absences/`;
        const urlFetchingData: string = `${statementsAbsences.id}/attachment/${statementsAbsences.attachment_id}`;
        const structure_id: string = `?structure_id=${statementsAbsences.structure_id}`;
        window.open(`${basicUrl}${urlFetchingData}${structure_id}`);
    },

    create: async (statementsAbsences: IStatementAbsenceBody): Promise<AxiosResponse> => {
        const formData: FormData = new FormData();
        const headers = {'headers': {'Content-type': 'multipart/form-data'}};

        formData.append('structure_id', statementsAbsences.structure_id);
        formData.append('student_id', statementsAbsences.student_id);
        formData.append('start_at', statementsAbsences.start_at);
        formData.append('end_at', statementsAbsences.end_at);
        formData.append('description', statementsAbsences.description);
        formData.append('file', statementsAbsences.file);

        return http.post(`/presences/statements/absences${statementsAbsences.file ? '/attachment' : ''}`, formData, headers);
    },

    validate: async (statementsAbsencesId: number, {isTreated}: IStatementAbsenceBody): Promise<AxiosResponse> => {
        return http.put(`${statementsAbsencesService.baseUrl}/${statementsAbsencesId}/validate`, {is_treated: isTreated});
    },

    update: async (statementsAbsencesId: number, statementsAbsences: IStatementAbsenceBody): Promise<AxiosResponse> => {
        return http.put(`${statementsAbsencesService.baseUrl}/${statementsAbsencesId}`, statementsAbsences);
    },

    delete: async (statementsAbsencesId: number): Promise<AxiosResponse> => {
        return http.delete(`${statementsAbsencesService.baseUrl}/${statementsAbsencesId}`);
    },

    export: async (statementsAbsences: IStatementsAbsencesRequest): Promise<void> => {
        const urlParams: String = statementsAbsencesService.getUrl(statementsAbsences, statementsAbsences.page, statementsAbsences.limit, statementsAbsences.offset);
        window.open(`${statementsAbsencesService.baseUrl}/export${urlParams}`);
    },

    getUrl: (statementsAbsences: IStatementsAbsencesRequest, pageNumber: number, limitNumber: number, offsetNumber: number): String => {
        const structure_id: string = `?structure_id=${statementsAbsences.structure_id}`;
        const start_at: string = `&start_at=${statementsAbsences.start_at}`;
        const end_at: string = `&end_at=${statementsAbsences.end_at}`;
        const isTreated: string = statementsAbsences.isTreated ? '' : `&is_treated=${statementsAbsences.isTreated}`;

        let student_ids: string = '';
        if (statementsAbsences.student_ids) {
            statementsAbsences.student_ids.forEach((student_id: string) => {
                student_ids += `&student_id=${student_id}`;
            });
        }
        const page: string = (pageNumber || pageNumber === 0) ?
            `&page=${pageNumber}` : ``;

        const limit: string = (limitNumber || limitNumber === 0) ?
            `&limit=${limitNumber}` : ``;

        const offset: string = (offsetNumber || offsetNumber === 0) ?
            `&offset=${offsetNumber}` : ``;

        return `${structure_id}${start_at}${end_at}${student_ids}${isTreated}${page}${limit}${offset}`;
    }
};

export const StatementsAbsencesService = ng.service('StatementsAbsencesService',
    (): IStatementsAbsencesService => statementsAbsencesService);
