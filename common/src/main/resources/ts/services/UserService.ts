import {ng} from 'entcore'
import http from 'axios';
import {Student} from "@common/model/Student";

export interface IUserService {
    getChildrenUser(relativeId: string): Promise<Array<Student>>;
}

export const UserService: IUserService = {
    getChildrenUser: async (relativeId: string): Promise<Array<Student>> => {
        try {
            const {data} = await http.get(`/presences/children?relativeId=${relativeId}`);
            return data;
        } catch (err) {
            throw err;
        }
    },
};

export const userService = ng.service('UserService', (): IUserService => UserService);