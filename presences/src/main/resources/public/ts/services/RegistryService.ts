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
    eventsDisplay?: RegistryEvent[];
    exclude: boolean;
}

export interface RegistryEvent {
    type: string;
    startDate: string;
    endDate: string;
    student_id: string;

    /* incidents properties */
    incident_type?: string;
    place?: string;
    protagonist_type?: string;

    /* absence properties */
    reason_id?: number;
    reason?: string;
}

export interface RegistryRequest {
    structureId: string;
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

            const urlParams = `?structureId=${registryParam.structureId}&month=${registryParam.month}${groupParams}${typeParams}`;
            const {data} = await http.get(`/presences/registry${urlParams}`);
            return data;
        } catch (err) {
            throw err;
        }
    },
};

export const RegistryService = ng.service('RegistryService', (): RegistryService => registryService);
