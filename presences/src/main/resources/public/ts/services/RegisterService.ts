import {moment, ng} from 'entcore'
import http from 'axios';
import {DateUtils} from "@common/utils";
import {Course} from "@presences/models";

export interface IRegisterService {
    get(registerRequest: IRegisterRequest): Promise<Course[]>;
}

export interface IRegisterRequest {
    start?: string;
    end?: string;
    structure?: string;
    teachers?: Array<string>;
    groups?: Array<string>;
    forgottenRegister?: boolean;
    multipleSlot?: boolean;
    limit?: number;
    offset?: number;
}

export const registerService: IRegisterService = {
    get: async (registerRequest: IRegisterRequest): Promise<Course[]> => {
        const start: string = `&start=${registerRequest.start}`;
        const end: string = `&end=${registerRequest.end}`;
        const time: string = `&_t=${moment().format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])}`;
        const structure: string = `&structure=${registerRequest.structure}`;
        let teacherFilter: string = '';
        let groupFilter: string = '';
        if (registerRequest.teachers && registerRequest.teachers.length > 0) {
            registerRequest.teachers.forEach((teacher: string) => teacherFilter += `teacher=${teacher}&`);
        }
        if (registerRequest.groups && registerRequest.groups.length > 0) {
            registerRequest.groups.forEach((group: string) => groupFilter += `group=${group}&`);
        }
        const forgottenRegisterParam: string = `&forgotten_registers=${registerRequest.forgottenRegister}`;
        const multipleSlotParam: string = `&multiple_slot=${registerRequest.multipleSlot}`;
        const limitParam: string = registerRequest.limit || registerRequest.limit === 0 ? `&limit=${registerRequest.limit}` : '';
        const offsetParam: string = registerRequest.offset || registerRequest.offset === 0
            ? `&offset=${registerRequest.limit * registerRequest.offset}` : '';
        const orderParam = `&descendingDate=true`;
        const urlParam: string = `${forgottenRegisterParam}${multipleSlotParam}${time}${limitParam}${offsetParam}${orderParam}`;

        const {data} = await http.get(`/presences/courses?${teacherFilter}${groupFilter}${structure}${start}${end}${urlParam}`);
        return data;
    },
};

export const RegisterService = ng.service('RegisterService',
    (): IRegisterService => registerService);
