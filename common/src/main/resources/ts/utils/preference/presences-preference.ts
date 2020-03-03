import {Me} from "entcore";
import {EventsFormFilter} from "@presences/utilities/events";
import {PreferencesUtils} from "./preferences";

/* ----------------------------
   Presences preferences
---------------------------- */

export class PresencesPreferenceUtils extends PreferencesUtils {
    /**
     * Update register activation preference
     *
     * @param isActive Register is active or not
     */
    static async updatePresencesRegisterPreference(isActive: boolean): Promise<void> {
        Me.preferences[this.PREFERENCE_KEYS.PRESENCE_REGISTER] = {multipleSlot: isActive};
        await Me.savePreference(this.PREFERENCE_KEYS.PRESENCE_REGISTER);
    }

    /**
     * Update Filters of event list
     *
     * @param filter        Object containing each filters
     * @param structureId   structure identifier corresponding to the current filter.
     */
    static async updatePresencesEventListFilter(filter: EventsFormFilter, structureId: string): Promise<void> {
        if (!Me.preferences[this.PREFERENCE_KEYS.PRESENCE_EVENT_LIST_FILTER]) {
            await Me.savePreference(this.PREFERENCE_KEYS.PRESENCE_EVENT_LIST_FILTER);
            await Me.preference(this.PREFERENCE_KEYS.PRESENCE_EVENT_LIST_FILTER)
        }
        Me.preferences[this.PREFERENCE_KEYS.PRESENCE_EVENT_LIST_FILTER][structureId] = filter;
        await Me.savePreference(this.PREFERENCE_KEYS.PRESENCE_EVENT_LIST_FILTER);
    }

}