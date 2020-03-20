import {ng} from 'entcore'
import {ISchoolYearPeriod, IStructureSlot} from "../model";
import http from "axios";

export interface IViescolaireService {
    getSchoolYearDates(structureId): Promise<ISchoolYearPeriod>;

    getSlotProfile(structureId: string): Promise<IStructureSlot>
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
    }
};

export const viescolaireService = ng.service('ViescolaireService', (): IViescolaireService => ViescolaireService);