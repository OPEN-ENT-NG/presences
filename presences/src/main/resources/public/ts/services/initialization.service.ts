import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {INIT_TYPE} from "../core/enum/init-type";
import {InitForm} from "@presences/models/init-form.model";
export interface IInitStatusResponse {
    initialized: boolean;
}
export interface IInitService {
    getPresencesInitStatus(structureId: string): Promise<IInitStatusResponse>;
    getViescoInitStatus(structureId: string): Promise<IInitStatusResponse>;

    initPresences(structureId: string, initType: INIT_TYPE): Promise<AxiosResponse>;
    initViesco(structureId: string, initType: INIT_TYPE, initForm: InitForm): Promise<AxiosResponse>;

}

export const initService: IInitService = {

    getPresencesInitStatus: async (structureId: string): Promise<IInitStatusResponse> => {
        return http.get(`/presences/initialization/structures/${structureId}`)
            .then((res: AxiosResponse) => res.data);
    },

    getViescoInitStatus: async (structureId: string): Promise<IInitStatusResponse> => {
        return http.get(`/viescolaire/structures/${structureId}/initialization`)
            .then((res: AxiosResponse) => res.data);
    },

    initPresences: async (structureId: string, initType: INIT_TYPE): Promise<AxiosResponse> => {
        return http.post(`/presences/initialization/structures/${structureId}`, {init_type: initType});
    },

    initViesco: async (structureId: string, initType: INIT_TYPE, initForm: InitForm): Promise<AxiosResponse> => {
        initForm.initType = initType;
        return http.post(`/viescolaire/structures/${structureId}/initialize`, initForm.toJSON());
    }

};

export const InitService = ng.service('InitService', (): IInitService => initService);