import {IMassmailingFilterPreferences} from "../model";
import {IPunishmentType} from "@incidents/models/PunishmentType";

export class HomeUtils {

    static buildFilteredMassmailingPreference = (filteredMassmailingPreference: any): IMassmailingFilterPreferences => {
        return {
            start_at: filteredMassmailingPreference.start_at,
            allReasons: filteredMassmailingPreference.allReasons,
            anomalies: filteredMassmailingPreference.anomalies,
            massmailing_status: filteredMassmailingPreference.massmailing_status,
            noReasons: filteredMassmailingPreference.noReasons,
            reasons: filteredMassmailingPreference.reasons,
            punishments: filteredMassmailingPreference.punishments,
            punishmentTypes: filteredMassmailingPreference.punishmentTypes,
            status: filteredMassmailingPreference.status
        } as IMassmailingFilterPreferences;
    };

    static getPunishmentTypePreferenceMap = (punishmentsTypes: Array<IPunishmentType>): Map<number, IPunishmentType> => {
        let punishmentTypeMap = new Map<number, IPunishmentType>();

        for (let i = 0; i < punishmentsTypes.length; i++) {
            const punishmentType: IPunishmentType = punishmentsTypes[i];
            if (!punishmentTypeMap.has(punishmentType.id)) {
                punishmentTypeMap.set(punishmentType.id, punishmentType);
            }
        }
        return punishmentTypeMap;
    };
}