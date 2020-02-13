import {ng} from 'entcore'
import http from 'axios';
import {User} from '@common/model/User';

export interface SearchItem {
    id: string;
    displayName: string;
    type: string;
    groupName?: string;
    className?: string;
}

export interface SearchService {
    search(structureId: string, value: string): Promise<SearchItem[]>;

    searchUser(structureId: string, value: string, profile: string): Promise<User[]>;
}

export const SearchService: SearchService = {
    search: async (structureId: string, value: string) => {
        try {
            const {data} = await http.get(`/presences/search?structureId=${structureId}&q=${value}`);
            data.forEach((item) => item.toString = () => item.displayName);
            return data;
        } catch (err) {
            throw err;
        }
    },

    /* Profile = ["Student", "Teacher", Personnel"] */
    searchUser: async (structureId: string, value: string, profile: string) => {
        try {
            const {data} = await http.get(`/presences/search/users?structureId=${structureId}&profile=${profile}&q=${value}&field=firstName&field=lastName`);
            data.forEach((user) => user.toString = () => user.displayName);
            return data;
        } catch (err) {
            throw err;
        }
    }
};

export const searchService = ng.service('SearchService', (): SearchService => SearchService);