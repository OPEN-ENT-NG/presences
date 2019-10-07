import {ng} from 'entcore'
import http from 'axios';
import {Place, Partner, IncidentType, ProtagonistType, Seriousness} from "@incidents/services";

export interface IncidentParameterType {
    place: Place[];
    partner: Partner[];
    incidentType: IncidentType[];
    seriousnessLevel: Seriousness[];
    protagonistType: ProtagonistType[];
}

export interface IncidentService {
    getIncidentParameterType(structureId: string): Promise<IncidentParameterType>
}

export const incidentService: IncidentService = {
    getIncidentParameterType: async (structureId: string) => {
        try {
            const {data} = await http.get(`/incidents/incidents/parameter/types?structureId=${structureId}`);
            return data;
        } catch (err) {
            throw err;
        }
    }
};

export const IncidentService = ng.service('IncidentService', (): IncidentService => incidentService);

