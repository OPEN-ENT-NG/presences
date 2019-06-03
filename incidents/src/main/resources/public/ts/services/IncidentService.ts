import {ng} from 'entcore'
import http from 'axios';

export interface IncidentParameterType {
    place: Place[];
    partner: Partner[];
    incidentType: IncidentType[];
    seriousnessLevel: SeriousnessLevel[];
    protagonistType: ProtagonistType[];
}

export interface Place {
    id: number;
    structureId: string;
    label: string;
}

export interface Partner {
    id: number;
    structureId: string;
    label: string;
}

export interface IncidentType {
    id: number;
    structureId: string;
    label: string;
}

export interface ProtagonistType {
    id: number;
    structureId: string;
    label: string;
}

export interface SeriousnessLevel {
    id: number;
    structureId: string;
    label: string;
    level: number;
}

export interface IncidentService {
    getIncidentParameterType(structureId: string): Promise<IncidentParameterType>
}

export const IncidentService = ng.service('IncidentService', (): IncidentService => ({
    getIncidentParameterType: async (structureId: string) => {
        try {
            const {data} = await http.get(`/incidents/incidents/parameter/types?structureId=${structureId}`);
            return data;
        } catch (err) {
            throw err;
        }
    }
}));

