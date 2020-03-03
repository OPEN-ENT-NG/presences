import {IMassmailingFilterPreferences} from "../model";

export class HomeUtils {

    static buildFilteredMassmailingPreference = (filteredMassmailingPreference: any): IMassmailingFilterPreferences => {
        return {
            start_at: filteredMassmailingPreference.start_at,
            allReasons: filteredMassmailingPreference.allReasons,
            anomalies: filteredMassmailingPreference.anomalies,
            massmailing_status: filteredMassmailingPreference.massmailing_status,
            noReasons: filteredMassmailingPreference.noReasons,
            reasons: filteredMassmailingPreference.reasons,
            status: filteredMassmailingPreference.status
        } as IMassmailingFilterPreferences;
    };
}