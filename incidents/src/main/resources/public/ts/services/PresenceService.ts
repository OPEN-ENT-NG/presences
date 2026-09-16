import {idiom as lang, ng} from 'entcore'
import { http, HttpResponse, HttpError } from 'entcore-toolkit';
import {Reason} from "@presences/models/Reason";

export interface IPresenceService {
    getReasons(structureId: string): Promise<Reason[]>;
}

export const presenceService: IPresenceService = {
    getReasons: async (structureId: string): Promise<Reason[]> => {
        return http.get(`/incidents/structures/${structureId}/reasons`)
            .then((res: HttpResponse) => res.data || [])
            .catch((err: HttpError) => Promise.reject(err));
    },
};

export const PresenceService = ng.service('PresenceService', (): IPresenceService => presenceService);
