import {Me} from "entcore";
import {EventsFormFilter} from "@presences/utilities/events";

export class PreferencesUtils {

    static async initPreference(): Promise<void> {
        /* REGISTER */
        await Me.preference('presences.register');
        if (Object.keys(Me.preferences['presences.register']).length === 0 &&
            Me.preferences['presences.register'].constructor === Object) {
            Me.preferences['presences.register'] = {multipleSlot: true};
            await Me.savePreference('presences.register');
        }


        /* EVENT LIST FILTER  */
        await Me.preference('presences.eventList.filters');
        if (Object.keys(Me.preferences['presences.eventList.filters']).length === 0 &&
            Me.preferences['presences.eventList.filters'].constructor === Object) {
            await Me.savePreference('presences.eventList.filters');
        }
    };

    static async updatePresencesRegisterPreference(isActive: boolean): Promise<void> {
        Me.preferences['presences.register'] = {multipleSlot: isActive};
        await Me.savePreference('presences.register');
    }

    static async updatePresencesEventListFilter(filter: EventsFormFilter, structure_id: string): Promise<void> {
        Me.preferences['presences.eventList.filters'][structure_id] = filter;
        await Me.savePreference('presences.eventList.filters');
    }
}