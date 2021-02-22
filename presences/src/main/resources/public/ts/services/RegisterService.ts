import {ng} from 'entcore';
import http, {AxiosResponse} from 'axios';

export interface RegisterService {
    setStatus(registerId: number, state_id: number): Promise<AxiosResponse>;
}

export const registerService: RegisterService = {

    setStatus: async (registerId: number, state_id: number): Promise<AxiosResponse> => {
        return http.put(`/presences/registers/${registerId}/status`, {state_id});
    }
};

export const RegisterService = ng.service('RegisterService', (): RegisterService => registerService);
