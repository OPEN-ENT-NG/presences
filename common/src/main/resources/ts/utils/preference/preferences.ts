import {Me} from "entcore";

export class PreferencesUtils {

    static async initPreference(): Promise<void> {
        /**
         * Fetching all existing preferences
         */
        await Promise.all([
            // structure
            Me.preference('presences.structure'),
            // presences module
            Me.preference('presences.register'),
            Me.preference('presences.eventList.filters'),
            // massmailing module
            Me.preference('presences.massmailing.filters')
        ]);

        /* STRUCTURE  */
        if (Object.keys(PreferencesUtils.getPreference('presences.structure')).length === 0 &&
            Me.preferences['presences.structure'].constructor === Object) {
            await Me.savePreference('presences.structure');
        }

        /* ----------------------------
            Presences preferences
        ---------------------------- */

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

        /* ----------------------------
            Massmailing preferences
        ---------------------------- */

        /* MASSMAILING FILTER  */
        if (Object.keys(PreferencesUtils.getPreference('presences.massmailing.filters')).length === 0 &&
            Me.preferences['presences.massmailing.filters'].constructor === Object) {
            await Me.savePreference('presences.massmailing.filters');
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