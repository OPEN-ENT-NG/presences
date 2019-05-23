import {ng} from 'entcore'
import http from 'axios';

export interface User {
    displayName: string;
    firstName: string;
    lastName: string;
    id: string;
    idClasse: any;
}

export interface UserService {
    search(structureId: string, value: string, profile: string): Promise<User[]>
}

export const UserService = ng.service('UserService', (): UserService => ({
    search: async (structureId: string, value: string, profile: string) => {
        try {
            const {data} = await http.get(`/presences/search/users?structureId=${structureId}&profile=${profile}&q=${value}&field=firstName&field=lastName`);
            return data;
        } catch (err) {
            throw err;
        }
    }
}));

