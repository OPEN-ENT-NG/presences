import {ng} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {Register} from '@presences/models';


export interface RegisterService {
    setStatus(register: Register, state_id: number): Promise<AxiosResponse>;
}

export const registerService: RegisterService = {

    setStatus: async (register: Register, state_id: number): Promise<AxiosResponse> => {
        return http.put(`/presences/registers/${register.id}/status`, {state_id});
    }
};

export const RegisterService = ng.service('RegisterService', (): RegisterService => registerService);
