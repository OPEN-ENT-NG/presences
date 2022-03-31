import {model, ng} from 'entcore'
import {ISchoolYearPeriod, IStructure, IStructureSlot} from "../model";
import http from "axios";
import {Student} from "@common/model/Student";

declare let window: any;

export interface IViescolaireService {
    getSchoolYearDates(structureId): Promise<ISchoolYearPeriod>;

    getSlotProfile(structureId: string): Promise<IStructureSlot>;

    getStudent(structureId:string, studentId: string): Promise<Array<Student>>

    getBuildOwnStructure(): Array<IStructure>;
}

export const ViescolaireService: IViescolaireService = {
    getSchoolYearDates: async (structureId: string): Promise<ISchoolYearPeriod> => {
        let {data} = await http.get(`viescolaire/settings/periode/schoolyear?structureId=` + structureId);
        return data;
    },

    getSlotProfile: async (structureId: string): Promise<IStructureSlot> => {
        try {
            const {data} = await http.get(`/viescolaire/structures/${structureId}/time-slot`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    getStudent: async (structureId: string, studentId: string): Promise<Array<Student>> => {
        let {data} = await http.get(`viescolaire/eleves?idUser=${studentId}&idStructure=${structureId}`);
        return data;
    },

    getBuildOwnStructure: (): Array<IStructure> => {
        const {structures, structureNames} = model.me;
        const values: Array<IStructure> = [];
        for (let i = 0; i < structures.length; i++) {
            values.push({id: structures[i], name: structureNames[i]});
        }
        return values;
    }
};

export const viescolaireService = ng.service('ViescolaireService', (): IViescolaireService => ViescolaireService);