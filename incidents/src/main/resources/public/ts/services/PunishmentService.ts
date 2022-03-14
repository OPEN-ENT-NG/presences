import {ng} from 'entcore'
import http, {AxiosError, AxiosResponse} from 'axios';
import {
    IPunishment,
    IPunishmentAbsenceRequest,
    IPunishmentBody,
    IPunishmentRequest,
    IPunishmentResponse
} from "@incidents/models";
import {IAbsence} from '@presences/models/Event/Absence';
import {User} from "@common/model/User";
import {PunishmentCategoryType} from "@incidents/models/PunishmentCategory";

export interface IPunishmentService {
    get(punishmentRequest: IPunishmentRequest): Promise<IPunishmentResponse>;

    getFromId(punishmentId: string, structureId: string): Promise<IPunishment>;

    getGroupedPunishmentId(groupedPunishmentId: string, structureId: string): Promise<IPunishmentResponse>;

    create(punishmentBody: IPunishmentBody): Promise<AxiosResponse>;

    update(punishmentBody: IPunishmentBody): Promise<AxiosResponse>;

    delete(punishmentId: string, structureId: string): Promise<AxiosResponse>;

    deleteGroupedPunishment(groupPunishmentId: string, structureId: string): Promise<AxiosResponse>;

    exportCSV(punishmentRequest: IPunishmentRequest): void;

    getStudentsAbsences(students: User[], startAt: string, endAt: string): Promise<Map<string, Array<IAbsence>>>;
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
                    if (processState.isSelected) {
                        stateParams += `&process=${processState.value}`;
                    }
                });
            }
            const structureUrl: string = `?structure_id=${punishmentRequest.structure_id}`;
            const dateUrl: string = `&start_at=${punishmentRequest.start_at}&end_at=${punishmentRequest.end_at}`;
            const urlParams: string = `${typeParams}${studentParams}${groupParams}${stateParams}`;
            const pageUrl: string = `&page=${punishmentRequest.page}`;
            const {data}: AxiosResponse = await http.get(`/incidents/punishments${structureUrl}${dateUrl}${urlParams}${pageUrl}`);
            data.all.filter((punishment:IPunishment) => punishment.type.punishment_category_id == PunishmentCategoryType.DETENTION)
                .forEach((punishment:IPunishment) => {
                    punishment.fields = [punishment.fields];
                    punishment.fields[0].id = punishment.id;
                })
            return data;
        } catch (err) {
            throw err;
        }
    },

    async getFromId(punishmentId: string, structureId: string): Promise<IPunishment> {
        const url: string = `/incidents/punishments?id=${punishmentId}&structure_id=${structureId}`
        const {data}: AxiosResponse = await http.get(url);
        return data;
    },

    async getGroupedPunishmentId(groupedPunishmentId: string, structureId: string): Promise<IPunishmentResponse> {
        const url: string = `/incidents/punishments?grouped_punishment_id=${groupedPunishmentId}&structure_id=${structureId}`
        const {data}: AxiosResponse = await http.get(url);
        return data;
    },

    async create(punishmentBody: IPunishmentBody): Promise<AxiosResponse> {
        return http.post(`/incidents/punishments`, punishmentBody);
    },

    async update(punishmentBody: IPunishmentBody): Promise<AxiosResponse> {
        return http.put(`/incidents/punishments`, punishmentBody);
    },

    async delete(punishmentId: string, structureId: string): Promise<AxiosResponse> {
        return http.delete(`/incidents/punishments?id=${punishmentId}&structureId=${structureId}`);
    },

    async deleteGroupedPunishment(groupedPunishmentId: string, structureId: string): Promise<AxiosResponse> {
        return http.delete(`/incidents/punishments?grouped_punishment_id=${groupedPunishmentId}&structureId=${structureId}`);
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
    },

    getStudentsAbsences(students: User[], startAt: string, endAt: string): Promise<Map<string, Array<IAbsence>>> {
        return http.post(`/incidents/punishments/students/absences`, {
            studentIds: students.map((student: User) => student.id),
            startAt: startAt,
            endAt: endAt
        } as IPunishmentAbsenceRequest)
            .then((res: AxiosResponse) => res.data.all || [])
            .catch((err: AxiosError) => Promise.reject(err))
    }
};

export const PunishmentService = ng.service('PunishmentService', (): IPunishmentService => punishmentService);