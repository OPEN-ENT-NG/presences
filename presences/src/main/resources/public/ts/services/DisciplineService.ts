import {ng} from 'entcore'
import { http, HttpResponse } from 'entcore-toolkit';
import {Discipline, DisciplineRequest} from "../models";

export interface DisciplineService {
    get(structureId: string): Promise<Discipline[]>;

    create(disciplineBody: DisciplineRequest): Promise<HttpResponse>;

    update(disciplineBody: DisciplineRequest): Promise<HttpResponse>;

    delete(disciplineId: number): Promise<HttpResponse>;
}

export const disciplineService: DisciplineService = {
    get: async (structureId: string): Promise<Discipline[]> => {
        try {
            const {data} = await http.get(`/presences/disciplines?structureId=${structureId}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    create: async (disciplineBody: DisciplineRequest): Promise<HttpResponse> => {
        return http.post(`/presences/discipline`, disciplineBody);
    },

    update: async (disciplineBody: DisciplineRequest): Promise<HttpResponse> => {
        return http.put(`/presences/discipline`, disciplineBody);
    },

    delete: async (disciplineId: number): Promise<HttpResponse> => {
        return http.delete(`/presences/discipline?id=${disciplineId}`);
    },
};

export const DisciplineService = ng.service('DisciplineService', (): DisciplineService => disciplineService);
