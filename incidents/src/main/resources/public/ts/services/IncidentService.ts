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
    getIncidentParameterType(structureId: string): Promise<IncidentParameterType>;

    getStudentIncidentsSummary(userId: string, structureId: string, startDate: string, endDate: string);
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
    },

    getStudentIncidentsSummary: async (userId: string, structureId: string, startDate: string, endDate: string) => {
        try {
            let url =
                `/incidents/incidents?structureId=${structureId}` + `&startDate=${startDate}` + `&endDate=${endDate}`;

            if (userId) {
                url += `&userId=${userId}`;
            }

            url += `&page=${this.page}`;

            const {data} = await http.get(url);
            return data;
        } catch (err) {
            throw err;
        }
    }
};

export const IncidentService = ng.service('IncidentService', (): IncidentService => incidentService);

