import {Me} from "entcore";

export class PreferencesUtils {

    public static readonly PREFERENCE_KEYS = {
        PRESENCE_STRUCTURE: 'presences.structure',
        // presence module's preferences
        PRESENCE_REGISTER: 'presences.register',
        PRESENCE_EVENT_LIST_FILTER: 'presences.eventList.filters',
        // Massmailing module's preferences
        MASSMAILING_FILTER: 'presences.massmailing.filters'
    };

    /**
     * Updated default structure selected
     *
     * @param structure selected.
     */
    static async updateStructure(structure): Promise<void> {
        if (!Me.preferences[this.PREFERENCE_KEYS.PRESENCE_STRUCTURE]) {
            await Me.savePreference(this.PREFERENCE_KEYS.PRESENCE_STRUCTURE);
            await Me.preference(this.PREFERENCE_KEYS.PRESENCE_STRUCTURE)
        }
        Me.preferences[this.PREFERENCE_KEYS.PRESENCE_STRUCTURE] = structure;
        await Me.savePreference(this.PREFERENCE_KEYS.PRESENCE_STRUCTURE);
    }
}