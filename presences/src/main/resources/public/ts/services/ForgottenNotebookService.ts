import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';

export interface Notebook {
    id: number;
    date: string;
    student_id: string;
    structure_id: string;
}

export interface NotebookRequest {
    date: string;
    studentId: string;
    structureId: string;
    id?: number;
    startDate?: string;
    endDate?: string;
}

export interface ForgottenNotebookService {
    get(notebook: NotebookRequest): Promise<Notebook[]>;

    create(notebook: NotebookRequest): Promise<AxiosResponse>;

    update(notebookId: number, notebook: NotebookRequest): Promise<AxiosResponse>;

    delete(notebookId: number): Promise<AxiosResponse>;
}

export const forgottenNotebookService: ForgottenNotebookService = {
    get: async (notebook: NotebookRequest): Promise<Notebook[]> => {
        try {
            const urlParams = `?studentId=${notebook.studentId}&startDate=${notebook.startDate}&endDate=${notebook.endDate}`;
            const {data} = await http.get(`/presences/forgotten/notebook${urlParams}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    create: async (notebook: NotebookRequest): Promise<AxiosResponse> => {
        return await http.post(`/presences/forgotten/notebook`, notebook);
    },

    update: async (notebookId: number, notebook: NotebookRequest): Promise<AxiosResponse> => {
        return await http.put(`/presences/forgotten/notebook/${notebookId}`, {date: notebook.date});
    },

    delete: async (notebookId: number): Promise<AxiosResponse> => {
        return await http.delete(`/presences/forgotten/notebook/${notebookId}`);
    }
};

export const ForgottenNotebookService = ng.service('ForgottenNotebookService',
    (): ForgottenNotebookService => forgottenNotebookService);
