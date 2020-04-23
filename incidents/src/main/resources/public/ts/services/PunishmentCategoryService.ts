import {ng} from 'entcore'
import http from 'axios';
import {IPunishmentCategory} from "@incidents/models/PunishmentCategory";

export interface IPunishmentsCategoryService {
    get(): Promise<IPunishmentCategory[]>;
}

export const punishmentsCategoryService: IPunishmentsCategoryService = {
    get: async (): Promise<IPunishmentCategory[]> => {
        try {
            const {data} = await http.get(`/incidents/punishments/category`);
            return data;
        } catch (err) {
            throw err;
        }
    }
};

export const PunishmentsCategoryService = ng.service('PunishmentsTypeService', (): IPunishmentsCategoryService => punishmentsCategoryService);