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
        Me.preferences['presences.register'] = {multipleSlot: isActive};
        await Me.savePreference('presences.register');
    }

    /**
     * Update Filters of event list
     *
     * @param filter        Object containing each filters
     * @param structureId   structure identifier corresponding to the current filter.
     */
    static async updatePresencesEventListFilter(filter: EventsFormFilter, structureId: string): Promise<void> {
        if (!Me.preferences['presences.eventList.filters']) {
            await Me.savePreference('presences.eventList.filters');
            await Me.preference('presences.eventList.filters')
        }
        Me.preferences['presences.eventList.filters'][structureId] = filter;
        await Me.savePreference('presences.eventList.filters');
    }

}