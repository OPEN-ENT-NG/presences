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

    searchStudents(structureId: string, value: string): Promise<User[]>;
}

export const SearchService: SearchService = {
    search: async (structureId: string, value: string) => {
        try {
            const {data} = await http.get(`/presences/search?structureId=${structureId}&q=${value}`);
            data.forEach((item) => {
                if (item.type == "USER") {
                    item.toString = () => item.displayName + ' - ' + item.groupName;
                } else if (item.type == "GROUP") {
                    item.toString = () => item.displayName;
                }
            });
            return data;
        } catch (err) {
            throw err;
        }
    },

    /* Profile = ["Student", "Teacher", Personnel"] */
    searchUser: async (structureId: string, value: string, profile: string) => {
        try {
            const {data} = await http.get(`/presences/search/users?structureId=${structureId}&profile=${profile}&q=${value}&field=firstName&field=lastName`);
            data.forEach((user) => {
                if (user.idClasse && user.idClasse != null) {
                    let idClass = user.idClasse;
                    user.idClasse = idClass.map(id => id.split('$')[1]);
                    user.toString = () => user.displayName + ' - ' + user.idClasse;
                } else user.toString = () => user.displayName;
            });

            return data;
        } catch (err) {
            throw err;
        }
    },

    searchStudents: async (structureId: string, value: string) => {
        try {
            const {data} = await http.get(`/presences/search/students?structureId=${structureId}&q=${value}&field=firstName&field=lastName`);
            data.forEach((user) => {
                if (user.idClasse && user.idClasse != null) {
                    let idClass = user.idClasse;
                    user.idClasse = idClass.map(id => id.split('$')[1]);
                    user.toString = () => user.displayName + ' - ' + user.idClasse;
                } else user.toString = () => user.displayName;
            });

            return data;
        } catch (err) {
            throw err;
        }
    }
};

export const searchService = ng.service('SearchService', (): SearchService => SearchService);