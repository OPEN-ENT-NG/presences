import {ng} from 'entcore'
import http from 'axios';
import {IncidentType, Partner, Place, ProtagonistType, Seriousness} from "@incidents/services";

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
        function builderIncidentParameterType(data) {
            let dataModel = data;
            for (let type in dataModel) {
                if (dataModel.hasOwnProperty(type)) {
                    dataModel[type].forEach(item => {
                        item.structureId = item.structure_id;
                        delete item.structure_id;
                    });
                }
            }
            return dataModel;
        }

        try {
            const {data} = await http.get(`/incidents/incidents/parameter/types?structureId=${structureId}`);
            return builderIncidentParameterType(data);
        } catch (err) {
            throw err;
        }
    }
};

export const IncidentService = ng.service('IncidentService', (): IncidentService => incidentService);

