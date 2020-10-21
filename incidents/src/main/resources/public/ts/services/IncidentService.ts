import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {IncidentType, Partner, Place, ProtagonistType, Seriousness} from "@incidents/services";
import {IStudentEventRequest} from "@presences/models";
import {IStudentIncidentResponse} from "@incidents/models";

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

    /* fetching PUNISHMENT/INCIDENT */
    getStudentEvents(studentEventRequest: IStudentEventRequest): Promise<IStudentIncidentResponse>;

}

export const incidentService: IncidentService = {
    getIncidentParameterType: async (structureId: string): Promise<IncidentParameterType> => {
        try {
            const {data}: AxiosResponse = await http.get(`/incidents/incidents/parameter/types?structureId=${structureId}`);
            return data;
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

            // url += `&page=${this.page}`;

            const {data} = await http.get(url);
            return data;
        } catch (err) {
            throw err;
        }
    },

    getStudentEvents: async (studentEventRequest: IStudentEventRequest): Promise<IStudentIncidentResponse> => {
        try {
            const structure_id: string = `?structure_id=${studentEventRequest.structure_id}`;
            const start_at: string = `&start_at=${studentEventRequest.start_at}`;
            const end_at: string = `&end_at=${studentEventRequest.end_at}`;

            let types: string = '';
            if (studentEventRequest.type) {
                studentEventRequest.type.forEach((type: string) => {
                    types += `&type=${type}`;
                });
            }

            let limit: string = '';
            if (studentEventRequest.limit) {
                limit = `&limit=${studentEventRequest.limit}`;
            }

            let offset: string = '';
            if (studentEventRequest.offset) {
                offset = `&offset=${studentEventRequest.offset}`;
            }

            const urlParams = `${structure_id}${start_at}${end_at}${types}${limit}${offset}`;
            const {data} = await http.get(`/incidents/students/${studentEventRequest.student_id}/events${urlParams}`);
            return data
        } catch (err) {
            throw err;
        }
    },
};

export const IncidentService = ng.service('IncidentService', (): IncidentService => incidentService);

