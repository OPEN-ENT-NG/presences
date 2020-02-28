import {Me} from "entcore";
import {EventsFormFilter} from "@presences/utilities/events";

export class PreferencesUtils {

    static async initPreference(): Promise<void> {
        /**
         * Preferences to get
         */
        await Promise.all([
            Me.preference('presences.register'),
            Me.preference('presences.eventList.filters'),
            Me.preference('presences.structure')
        ]);


        /* REGISTER */
        if (Object.keys(PreferencesUtils.getPreference('presences.register')).length === 0 &&
            Me.preferences['presences.register'].constructor === Object) {
            Me.preferences['presences.register'] = {multipleSlot: true};
            await Me.savePreference('presences.register');
        }


        /* EVENT LIST FILTER  */
        if (Object.keys(PreferencesUtils.getPreference('presences.eventList.filters')).length === 0 &&
            Me.preferences['presences.eventList.filters'].constructor === Object) {
            await Me.savePreference('presences.eventList.filters');
        }

        /* STRUCTURE  */
        if (Object.keys(PreferencesUtils.getPreference('presences.structure')).length === 0 &&
            Me.preferences['presences.structure'].constructor === Object) {
            await Me.savePreference('presences.structure');
        }
    };

    /**
     * Get preference of corresponding key
     *
     * @param key index of the wanting preference.
     */
    static getPreference(key: string): Object {
        return Me.preferences[key] ? Me.preferences[key] : {};
    }

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
     * @param structureId   Id conrresponding to the current filter.
     */
    static async updatePresencesEventListFilter(filter: EventsFormFilter, structureId: string): Promise<void> {
        if (!Me.preferences['presences.eventList.filters']) {
            await Me.savePreference('presences.eventList.filters');
            await Me.preference('presences.eventList.filters')
        }
        Me.preferences['presences.eventList.filters'][structureId] = filter;
        await Me.savePreference('presences.eventList.filters');
    }

    /**
     * Updated default structure selected
     *
     * @param structure selected.
     */
    static async updateStructure(structure): Promise<void> {
        if (!Me.preferences['presences.structure']) {
            await Me.savePreference('presences.structure');
            await Me.preference('presences.structure')
        }
        Me.preferences['presences.structure'] = structure;
        await Me.savePreference('presences.structure');
    }
}