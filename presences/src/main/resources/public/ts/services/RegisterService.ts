import {ng} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {Course} from "@presences/models";

export interface RegisterService {
    setStatus(registerId: number, state_id: number): Promise<AxiosResponse>;

    getLastForgottenRegisterCourses(structureId: string, startDate: string, endDate: string, teacherIds?: Array<string>, groupNames?: Array<string>): Promise<Course[]>;

}

export const registerService: RegisterService = {

    setStatus: async (registerId: number, state_id: number): Promise<AxiosResponse> => {
        return http.put(`/presences/registers/${registerId}/status`, {state_id});
    },

    getLastForgottenRegisterCourses : async (structureId: string, startDate: string, endDate: string,
                                             teacherIds?: Array<string>, groupNames?: Array<string>): Promise<Course[]> => {
        let urlParams: string = `?startDate=${startDate}&endDate=${endDate}`;

        if (teacherIds.length > 0) {
            teacherIds.forEach((id:string) => {
                urlParams += `&teacherId=${id}`
            })
        }

        if (groupNames.length > 0) {
            groupNames.forEach((name:string) => {
                urlParams += `&groupName=${name}`
            })
        }

        return http.get(`/presences/structures/${structureId}/registers/forgotten${urlParams}`).then((res: AxiosResponse) => {
            return res.data;
        });
    }
};

export const RegisterService = ng.service('RegisterService', (): RegisterService => registerService);
