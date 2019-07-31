import {ng} from 'entcore'
import http from 'axios';
import {User} from '@common/model/User';

export interface Group {
    id: string;
    name: string;
}

export interface GroupService {
    search(structureId: string, value: string): Promise<Group[]>;

    getGroupUsers(id: string, profile: string): Promise<Array<User>>;
}

export const GroupService = ng.service('GroupService', (): GroupService => ({
    search: async (structureId: string, value: string) => {
        try {
            const {data} = await http.get(`/presences/search/groups?structureId=${structureId}&q=${value}&field=name`);
            return data;
        } catch (err) {
            throw err;
        }
    },
    getGroupUsers: async (id, profile) => {
        try {
            const {data} = await http.get(`/viescolaire/groupe/enseignement/users/${id}?type=${profile}`);
            data.map((user: User) => user.displayName = `${user.firstName} ${user.lastName}`);
            return data;
        } catch (err) {
            throw err;
        }
    }
}));

