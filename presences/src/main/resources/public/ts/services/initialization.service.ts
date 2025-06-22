import {ng, idiom} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {INIT_TYPE} from "../core/enum/init-type";
import { HOLIDAYS_ZONE, IInitFormZone, InitForm } from '../models/init-form.model';

export interface IInitStatusResponse {
    initialized: boolean;
}

export interface IInitTeachersResponse {
    teachers: Array<{id: string, displayName: string}>;
    count: number;
}


export interface IInitService {
    getPresencesInitStatus(structureId: string): Promise<boolean>;
    getViescoInitStatus(structureId: string): Promise<IInitStatusResponse>;

    initPresences(structureId: string, initType: INIT_TYPE): Promise<AxiosResponse>;
    initViesco(structureId: string, initType: INIT_TYPE, initForm: InitForm): Promise<AxiosResponse>;

    getTeachersInitializationStatus(structureId: string): Promise<IInitTeachersResponse>;
    getZones(): IInitFormZone[];

}

export const initService: IInitService = {

    getPresencesInitStatus: async (structureId: string): Promise<boolean> => {
        return http.get(`/presences/initialization/structures/${structureId}`)
            .then((res: AxiosResponse) => res.data.initialized);
    },

    getViescoInitStatus: async (structureId: string): Promise<IInitStatusResponse> => {
        return http.get(`/viescolaire/structures/${structureId}/initialization`)
            .then((res: AxiosResponse) => res.data);
    },

    initPresences: async (structureId: string, initType: INIT_TYPE): Promise<AxiosResponse> => {
        return http.post(`/presences/initialization/structures/${structureId}`, {init_type: initType});
    },

    initViesco: async (structureId: string, initType: INIT_TYPE, initForm: InitForm): Promise<AxiosResponse> => {
        initForm.initType = initType;
        return http.post(`/viescolaire/structures/${structureId}/initialize`, initForm.toJSON());
    },

    getTeachersInitializationStatus: async (structureId: string): Promise<IInitTeachersResponse> => {
        return http.get(`/viescolaire/structures/${structureId}/initialization/teachers`)
            .then((res: AxiosResponse) => res.data);
    },

    getZones: (): IInitFormZone[] => {
        const zones: IInitFormZone[] = [];

        zones.push({value: HOLIDAYS_ZONE.ZONE_A, label: idiom.translate("presences.init.1d.form.holidays.detected.zone.A")});
        zones.push({value: HOLIDAYS_ZONE.ZONE_B, label: idiom.translate("presences.init.1d.form.holidays.detected.zone.B")});
        zones.push({value: HOLIDAYS_ZONE.ZONE_C, label: idiom.translate("presences.init.1d.form.holidays.detected.zone.C")});

        zones.push({value: HOLIDAYS_ZONE.CORSE, label: idiom.translate("presences.init.1d.form.holidays.detected.zone.Corse")});
        zones.push({value: HOLIDAYS_ZONE.GUADELOUPE, label: idiom.translate("presences.init.1d.form.holidays.detected.zone.Guadeloupe")});
        zones.push({value: HOLIDAYS_ZONE.GUYANE, label: idiom.translate("presences.init.1d.form.holidays.detected.zone.Guyane")});
        zones.push({value: HOLIDAYS_ZONE.MARTINIQUE, label: idiom.translate("presences.init.1d.form.holidays.detected.zone.Martinique")});
        zones.push({value: HOLIDAYS_ZONE.MAYOTTE, label: idiom.translate("presences.init.1d.form.holidays.detected.zone.Mayotte")});
        zones.push({value: HOLIDAYS_ZONE.NOUVELLE_CALEDONIE, label: idiom.translate("presences.init.1d.form.holidays.detected.zone.NouvelleCaledonie")});
        zones.push({value: HOLIDAYS_ZONE.POLYNESIE, label: idiom.translate("presences.init.1d.form.holidays.detected.zone.Polynesie")});
        zones.push({value: HOLIDAYS_ZONE.REUNION, label: idiom.translate("presences.init.1d.form.holidays.detected.zone.Reunion")});
        zones.push({value: HOLIDAYS_ZONE.SAINT_PIERRE_ET_MIQUELON, label: idiom.translate("presences.init.1d.form.holidays.detected.zone.SaintPierreEtMiquelon")});
        zones.push({value: HOLIDAYS_ZONE.WALLIS_ET_FUTUNA, label: idiom.translate("presences.init.1d.form.holidays.detected.zone.WallisEtFutuna")});

        return zones;
    }
};

export const InitService = ng.service('InitService', (): IInitService => initService);
