import {ng} from 'entcore';
import http, {AxiosResponse} from 'axios';

export interface Setting {
    alert_absence_threshold?: number
    alert_lateness_threshold?: number
    alert_incident_threshold?: number
    alert_forgotten_notebook_threshold?: number
    event_recovery_method?: 'HOUR' | 'HALF_DAY' | 'DAY',
    allow_multiple_slots?: boolean
}

export interface SettingsService {
    retrieve(structureId: string): Promise<Setting>;

    retrieveMultipleSlotSetting(structureId: string): Promise<boolean>;

    put(structureId: string, setting: Setting): Promise<Setting>;
}

export const settingService: SettingsService = {
    async put(structureId: string, setting: Setting): Promise<Setting> {
        try {
            const {data} = await http.put(`/presences/structures/${structureId}/settings`, setting);
            return data;
        } catch (e) {
            throw e;
        }
    },
    async retrieve(structureId: string): Promise<Setting> {
        try {
            const {data} = await http.get(`/presences/structures/${structureId}/settings`);
            return data;
        } catch (e) {
            throw e;
        }
    },
    async retrieveMultipleSlotSetting(structureId: string): Promise<boolean> {
        return http.get(`/presences/structures/${structureId}/settings/multiple-slots`)
                .then((res: AxiosResponse) => res.data.allow_multiple_slots);
    }
};

export const SettingService = ng.service('SettingService', (): SettingsService => settingService);