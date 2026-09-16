import {ng} from "entcore";
import { http, HttpResponse } from 'entcore-toolkit';
import {IStructureSlot} from "@common/model";

interface TimeslotClasseService {
    getAudienceTimeslot(audienceId: string): Promise<IStructureSlot>;

    getAllClassFromTimeslot(timeslotId: string): Promise<string[]>;

    createOrUpdateClassTimeslot(timeslotId: string, classId: string): Promise<HttpResponse>;

    deleteClassTimeslot(classId: string): Promise<HttpResponse>;

    deleteAllAudienceFromTimeslot(timeslotId: string): Promise<HttpResponse>;
}

export const timeslotClasseService: TimeslotClasseService =  {
    async getAudienceTimeslot(audienceId: string): Promise<IStructureSlot> {
        const {data}: HttpResponse = await http.get(`/viescolaire/timeslot/audience/${audienceId}`);
        return data as IStructureSlot;
    },

    async getAllClassFromTimeslot(timeslotId: string): Promise<string[]> {
        const {data}: HttpResponse = await http.get(`/viescolaire/timeslot/${timeslotId}`);
        return data as string[];
    },

    async createOrUpdateClassTimeslot(timeslotId: string, classId: string): Promise<HttpResponse> {
        return http.post(`/viescolaire/timeslot/audience`, {timeslot_id: timeslotId, class_id: classId});
    },

    async deleteAllAudienceFromTimeslot(timeslotId: string): Promise<HttpResponse> {
        return http.delete(`/viescolaire/timeslot/${timeslotId}`);
    },

    async deleteClassTimeslot(classId: string): Promise<HttpResponse> {
        return http.delete(`/viescolaire/timeslot/audience/${classId}`);
    }
}

export const TimeslotClasseService = ng.service('TimeslotClasseService', (): TimeslotClasseService => timeslotClasseService);