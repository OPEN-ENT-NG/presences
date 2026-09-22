import {model, ng} from 'entcore'
import { http, HttpResponse } from 'entcore-toolkit';
import {IPunishmentType, IPunishmentTypeBody} from "@incidents/models/PunishmentType";
import rights from "@incidents/rights";

export interface IPunishmentsTypeService {
    get(structure_id: string): Promise<IPunishmentType[]>;

    create(punishmentsTypeBody: IPunishmentTypeBody): Promise<HttpResponse>;

    update(punishmentsTypeBody: IPunishmentTypeBody): Promise<HttpResponse>;

    delete(id_deleted: number): Promise<HttpResponse>;
}

export const punishmentsTypeService: IPunishmentsTypeService = {
    get: async (structure_id: string): Promise<IPunishmentType[]> => {
        try {
            if (!model.me.hasWorkflow(rights.workflow.incidentRead)) return [];
            const {data} = await http.get(`/incidents/punishments/type?structure_id=${structure_id}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    create: async (punishmentsTypeBody: IPunishmentTypeBody): Promise<HttpResponse> => {
        return http.post(`/incidents/punishments/type`, punishmentsTypeBody);
    },

    update: async (punishmentsTypeBody: IPunishmentTypeBody): Promise<HttpResponse> => {
        return http.put(`/incidents/punishments/type`, punishmentsTypeBody);
    },

    delete: async (id_deleted: number): Promise<HttpResponse> => {
        return http.delete(`/incidents/punishments/type?id=${id_deleted}`);
    },
};

export const PunishmentsTypeService = ng.service('PunishmentsTypeService', (): IPunishmentsTypeService => punishmentsTypeService);
