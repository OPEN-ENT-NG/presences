import {idiom as lang, ng} from 'entcore'
import http, {AxiosResponse, AxiosError} from 'axios';
import {Reason} from "@presences/models/Reason";

export interface IPresenceService {
    getReasons(structureId: string): Promise<Reason[]>;
}

export const presenceService: IPresenceService = {
    getReasons: async (structureId: string): Promise<Reason[]> => {
        return http.get(`/incidents/structures/${structureId}/reasons`)
            .then((res: AxiosResponse) => res.data || [])
            .catch((err: AxiosError) => Promise.reject(err));
    },
};

export const PresenceService = ng.service('PresenceService', (): IPresenceService => presenceService);
