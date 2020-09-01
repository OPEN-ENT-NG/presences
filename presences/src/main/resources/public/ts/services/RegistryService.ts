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
    forgottenNotebook: boolean;
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
    forgottenNotebook: boolean;
}

export interface RegistryService {
    getRegisterSummary(registryParam: RegistryRequest): Promise<Registry[]>;

    exportCSV(registryParam: RegistryRequest) : void;
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
            const forgottenNotebookParam = `&forgottenNotebook=${registryParam.forgottenNotebook}`;
            const {data} = await http.get(`/presences/registry${urlParams}${forgottenNotebookParam}`);

            return data;
        } catch (err) {
            throw err;
        }
    },

    exportCSV: async (registryParam: RegistryRequest) : Promise<void> => {
        try {
            let groupParams : string = '';
            registryParam.group.forEach(groupId => {
                groupParams += `&group=${groupId}`;
            });

            let typeParams : string = '';
            registryParam.type.forEach(typeName => {
                typeParams += `&type=${typeName}`;
            });

            const urlParams : string = `?structureId=${registryParam.structureId}&month=${registryParam.month}${groupParams}${typeParams}`;
            const forgottenNotebookParam : string = `&forgottenNotebook=${registryParam.forgottenNotebook}`;
            window.open(`/presences/registry/export${urlParams}${forgottenNotebookParam}`)
        } catch (err) {
            throw err;
        }
    },
};

export const RegistryService = ng.service('RegistryService', (): RegistryService => registryService);
