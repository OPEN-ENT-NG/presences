import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {Discipline, DisciplineRequest} from "../models";

export interface DisciplineService {
    get(structureId: string): Promise<Discipline[]>;

    create(disciplineBody: DisciplineRequest): Promise<AxiosResponse>;

    update(disciplineBody: DisciplineRequest): Promise<AxiosResponse>;

    delete(disciplineId: number): Promise<AxiosResponse>;
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

    create: async (disciplineBody: DisciplineRequest): Promise<AxiosResponse> => {
        return http.post(`/presences/discipline`, disciplineBody);
    },

    update: async (disciplineBody: DisciplineRequest): Promise<AxiosResponse> => {
        return http.put(`/presences/discipline`, disciplineBody);
    },

    delete: async (disciplineId: number): Promise<AxiosResponse> => {
        return http.delete(`/presences/discipline?id=${disciplineId}`);
    },
};

export const DisciplineService = ng.service('DisciplineService', (): DisciplineService => disciplineService);
