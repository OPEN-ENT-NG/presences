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
        if (!Me.preferences['presences.massmailing.filters']) {
            Me.savePreference('presences.massmailing.filters').then(async () => {
                await Me.preference('presences.massmailing.filters');
            });
        }
        Me.preferences['presences.massmailing.filters'][structureId] = filter;
        await Me.savePreference('presences.massmailing.filters');
    }

}