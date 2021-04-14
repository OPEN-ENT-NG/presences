import {IPunishmentType} from "@incidents/models/PunishmentType";
import {IMassmailingFilterPreferences} from "@massmailing/model";

export class HomeUtils {


    static buildFilteredMassmailingPreference = (filteredMassmailingPreference: any): IMassmailingFilterPreferences => {
        return {...filteredMassmailingPreference} as IMassmailingFilterPreferences;
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