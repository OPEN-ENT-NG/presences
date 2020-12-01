import {ng} from 'entcore'
import http from 'axios';
import {Student} from "@common/model/Student";

export interface IUserService {
    getChildrenUser(relativeId: string): Promise<Array<Student>>;

    getChildUser(childId: string): Promise<Student>;
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
    // @Get("/user/:id/child")
    getChildUser: async (childId: string): Promise<Student> => {
        try {
            const {data} = await http.get(`/presences/user/${childId}/child`);
            return data;
        } catch (err) {
            throw err;
        }
    }
};

export const userService = ng.service('UserService', (): IUserService => UserService);

