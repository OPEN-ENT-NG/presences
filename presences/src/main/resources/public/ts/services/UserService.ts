import {ng} from 'entcore'
import http from 'axios';

export interface UserService {
    search(structureId: string, value: string, profile: string)
}

export const UserService = ng.service('UserService', (): UserService => ({
    search: async (structureId: string, value: string, profile: string) => {
        try {
            const {data} = await http.get(`/presences/search/users?structureId=${structureId}&profile=${profile}&q=${value}&field=displayName`);
            return data;
        } catch (err) {
            throw err;
        }
    }
}));

