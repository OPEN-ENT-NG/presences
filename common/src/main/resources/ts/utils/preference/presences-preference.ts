import {Me} from "entcore";
import {EventsFormFilter} from "@presences/utilities/events";
import {EventListCalendarFilter} from "@presences/models";
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

    /**
     * Update Filters of planned absences list
     *
     * @param filter        Object containing each filters
     * @param structureId   structure identifier corresponding to the current filter.
     */
    static async updatePresencesPlannedAbsencesFilter(filter: EventsFormFilter, structureId: string): Promise<void> {
        if (!Me.preferences[this.PREFERENCE_KEYS.PRESENCE_PLANNED_ABSENCES_FILTER]) {
            await Me.savePreference(this.PREFERENCE_KEYS.PRESENCE_PLANNED_ABSENCES_FILTER);
            await Me.preference(this.PREFERENCE_KEYS.PRESENCE_PLANNED_ABSENCES_FILTER);
        }
        Me.preferences[this.PREFERENCE_KEYS.PRESENCE_PLANNED_ABSENCES_FILTER][structureId] = filter;
        await Me.savePreference(this.PREFERENCE_KEYS.PRESENCE_PLANNED_ABSENCES_FILTER);
    }

    /**
     * Update EventList Filter (dates / students) when we move to Calendar view
     *
     * @param filter    EventListCalendarFilter, containing dates and students
     */
    static async updatePresencesEventListCalendarFilter(filter: EventListCalendarFilter): Promise<void> {
        const key: string = this.PREFERENCE_KEYS.PRESENCE_EVENT_LIST_CALENDAR_FILTER
        Me.preferences[key] = filter;
        await Me.savePreference(key);
    }
}