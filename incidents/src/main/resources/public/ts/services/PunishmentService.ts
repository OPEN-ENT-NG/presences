import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {IPunishmentBody, IPunishmentRequest, IPunishmentResponse,} from "@incidents/models";

export interface IPunishmentService {
    get(punishmentRequest: IPunishmentRequest): Promise<IPunishmentResponse>;

    create(punishmentBody: IPunishmentBody): Promise<AxiosResponse>;

    update(punishmentBody: IPunishmentBody): Promise<AxiosResponse>;

    delete(punishmentId: string): Promise<AxiosResponse>;

    exportCSV(punishmentRequest: IPunishmentRequest): void;
}

export const punishmentService: IPunishmentService = {
    async get(punishmentRequest: IPunishmentRequest): Promise<IPunishmentResponse> {
        try {

            let typeParams: string = '';
            if (punishmentRequest.type_ids) {
                punishmentRequest.type_ids.forEach((type_id: number) => {
                    typeParams += `&type_id=${type_id}`;
                });
            }

            let studentParams: string = '';
            if (punishmentRequest.students_ids) {
                punishmentRequest.students_ids.forEach((studentId: string) => {
                    studentParams += `&student_id=${studentId}`;
                });
            }

            let groupParams: string = '';
            if (punishmentRequest.groups_ids) {
                punishmentRequest.groups_ids.forEach((groupId: string) => {
                    groupParams += `&group_id=${groupId}`;
                });
            }

            let stateParams: string = '';
            if (punishmentRequest.process_state) {
                punishmentRequest.process_state.forEach((processState: { label: string, value: string, isSelected: boolean }) => {
                    if(processState.isSelected) {
                        stateParams += `&process=${processState.value}`;
                    }
                });
            }
            const structureUrl: string = `?structure_id=${punishmentRequest.structure_id}`;
            const dateUrl: string = `&start_at=${punishmentRequest.start_at}&end_at=${punishmentRequest.end_at}`;
            const urlParams: string = `${typeParams}${studentParams}${groupParams}${stateParams}`;
            const pageUrl: string = `&page=${punishmentRequest.page}`;
            const {data}: AxiosResponse = await http.get(`/incidents/punishments${structureUrl}${dateUrl}${urlParams}${pageUrl}`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    async create(punishmentBody: IPunishmentBody): Promise<AxiosResponse> {
        return http.post(`/incidents/punishments`, punishmentBody);
    },

    async update(punishmentBody: IPunishmentBody): Promise<AxiosResponse> {
        return http.put(`/incidents/punishments`, punishmentBody);
    },

    async delete(punishmentId: string): Promise<AxiosResponse> {
        return http.delete(`/incidents/punishments?id=${punishmentId}`);
    },


    /**
     * Export the punishments list as CSV format.
     * @param punishmentRequest the punishments get request.
     */
    exportCSV(punishmentRequest: IPunishmentRequest): void {
        let filterParams: string = '';
        if (punishmentRequest.type_ids) {
            punishmentRequest.type_ids.forEach((type_id: number) => {
                filterParams += `&type_id=${type_id}`;
            });
        }

        if (punishmentRequest.students_ids) {
            punishmentRequest.students_ids.forEach((studentId: string) => {
                filterParams += `&student_id=${studentId}`;
            });
        }

        if (punishmentRequest.groups_ids) {
            punishmentRequest.groups_ids.forEach((groupId: string) => {
                filterParams += `&group_id=${groupId}`;
            });
        }

        let url: string = `/incidents/punishments/export?structure_id=${punishmentRequest.structure_id}` +
            `&start_at=${punishmentRequest.start_at}` +
            `&end_at=${punishmentRequest.end_at}` + filterParams;

        window.open(url);
    }
};

export const PunishmentService = ng.service('PunishmentService', (): IPunishmentService => punishmentService);