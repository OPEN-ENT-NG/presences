import {ng} from 'entcore'
import http from 'axios';

export interface Registry {
    id: string;
    displayName: string;
    className: string;
    days: RegistryDays[];
}

export interface RegistryDays {
    date: string;
    events: RegistryEvent[];
}

export interface RegistryEvent {
    type: string;
    startDate: string;
    endDate: string;
    comment: string;
}

export interface RegistryRequest {
    month: string;
    group: string[];
    type: string[];
}

export interface RegistryService {
    getRegisterSummary(registryParam: RegistryRequest): Promise<Registry[]>;
}

export const registryService: RegistryService = {
    getRegisterSummary: async (registryParam: RegistryRequest): Promise<Registry[]> => {
        try {

            let groupParams = '';
            registryParam.group.forEach(groupId => {
                groupParams += `&group=${groupId}`;
            });

            let typeParams = '';
            registryParam.type.forEach(typeName => {
                typeParams += `&type=${typeName}`;
            });

            const urlParams = `?month=${registryParam.month}${groupParams}${typeParams}`;
            const {data} = await http.get(`/presences/registry${urlParams}`);
            return data;
        } catch (err) {
            throw err;
        }
    },
};

export const RegistryService = ng.service('RegistryService', (): RegistryService => registryService);
