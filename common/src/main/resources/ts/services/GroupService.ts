import {ng} from 'entcore'
import http from 'axios';

export interface Group {
    id: string;
    name: string;
}

export interface GroupService {
    search(structureId: string, value: string): Promise<Group[]>
}

export const GroupService = ng.service('GroupService', (): GroupService => ({
    search: async (structureId: string, value: string) => {
        try {
            const {data} = await http.get(`/presences/search/groups?structureId=${structureId}&q=${value}&field=name`);
            return data;
        } catch (err) {
            throw err;
        }
    }
}));

