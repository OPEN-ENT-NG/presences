import {ng} from 'entcore'
import http from 'axios';
import {User} from '@common/model/User';
import {SearchItem} from "@common/services/SearchService";

export interface Group {
    id: string;
    name: string;
}

export interface GroupService {
    search(structureId: string, value: string): Promise<Group[]>;

    getGroupUsers(id: string, profile: string): Promise<Array<User>>;

    getStudentsFromGroupId(id: string, type: string): Promise<Array<SearchItem>>;
}

export const GroupService: GroupService = {
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
    },
    
    getStudentsFromGroupId: async (groupId: string, type: string): Promise<Array<SearchItem>> => {
        try {
            const {data} = await http.get(`/presences/users?groupId=${groupId}&type=${type}`);
            data.map((item) => item.displayName = `${item.firstName} ${item.lastName}`);
            return data;
        } catch (err) {
            throw err;
        }
    },
};

export const groupService = ng.service('GroupService', (): GroupService => GroupService);