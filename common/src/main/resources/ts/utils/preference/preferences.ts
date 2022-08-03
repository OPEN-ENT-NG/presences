import {Me} from "entcore";

export class PreferencesUtils {

    public static readonly PREFERENCE_KEYS = {
        PRESENCE_STRUCTURE: 'presences.structure',
        // presence module's preferences
        PRESENCE_REGISTER: 'presences.register',
        PRESENCE_EVENT_LIST_FILTER: 'presences.eventList.filters',
        PRESENCE_PLANNED_ABSENCES_FILTER: 'presences.plannedAbsences.filters',
        PRESENCE_EVENT_LIST_CALENDAR_FILTER: 'presences.eventListCalendar.filters',
        PRESENCE_EXPORT_EVENTS_DISPLAY_POPUP: 'presences.export.events.displayPopup',

        // Massmailing module's preferences
        MASSMAILING_FILTER: 'presences.massmailing.filters',

        // Incidents module's preferences
        INCIDENTS_EXPORT_INCIDENTS_DISPLAY_POPUP: 'incidents.export.incidents.displayPopup',
        INCIDENTS_EXPORT_PUNISHMENTS_DISPLAY_POPUP: 'incidents.export.punishments.displayPopup'
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

    /**
     * Save boolean preference from key and structureId
     * @param key           preference key we want to save.
     * @param value         boolean value for the preference.
     * @param structureId   structure identifier corresponding to the preference.
     */
    static async savePreferenceBoolean(key: string, value: boolean, structureId: string): Promise<void> {
        if (!Me.preferences[key]) {
            await Me.savePreference(key);
            await Me.preference(key);
        }
        Me.preferences[key][structureId] = value;
        await Me.savePreference(key);
    }
    /**
     * Create and Save preference if it not exists
     *
     * @param key   string, Concerned preference key we want to save.
     */
    static async savePreferenceIfNotExists(key: string): Promise<void> {
        if (!Me.preferences[key]) {
            await Me.savePreference(key);
        }
    }

    /**
     * Reset preference to null value
     *
     * @param key   string, Concerned preference key we want to reset.
     */
    static async resetPreference(key: string): Promise<void> {
        Me.preferences[key] = null;
        await Me.savePreference(key);
    }
}