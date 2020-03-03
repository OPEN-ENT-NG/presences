import {Me} from "entcore";
import {PreferencesUtils} from "./preferences";
import {IMassmailingFilterPreferences} from "@massmailing/model/Massmailing";

/* ----------------------------
    Massmailing preferences
---------------------------- */

export class MassmailingPreferenceUtils extends PreferencesUtils {

    /**
     * Update Filters of massmailing list
     *
     * @param filter        Object containing each filters in massmailing
     * @param structureId   structure identifier corresponding to the current filter.
     */
    static async updatePresencesMassmailingFilter(filter: IMassmailingFilterPreferences, structureId: string): Promise<void> {
        if (!Me.preferences[this.PREFERENCE_KEYS.MASSMAILING_FILTER]) {
            Me.savePreference(this.PREFERENCE_KEYS.MASSMAILING_FILTER).then(async () => {
                await Me.preference(this.PREFERENCE_KEYS.MASSMAILING_FILTER);
            });
        }
        Me.preferences[this.PREFERENCE_KEYS.MASSMAILING_FILTER][structureId] = filter;
        await Me.savePreference(this.PREFERENCE_KEYS.MASSMAILING_FILTER);
    }

}