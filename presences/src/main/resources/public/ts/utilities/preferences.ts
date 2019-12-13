import {Me} from "entcore";

export class PreferencesUtils {

    static async initPreference(): Promise<void> {
        await Me.preference('presences.register');
        if (Object.keys(Me.preferences['presences.register']).length === 0 &&
            Me.preferences['presences.register'].constructor === Object) {
            Me.preferences['presences.register'] = {multipleSlot: true};
            await Me.savePreference('presences.register');
        }

    };

    static async updatePresencesRegisterPreference(isActive: boolean): Promise<void> {
        Me.preferences['presences.register'] = {multipleSlot: isActive};
        await Me.savePreference('presences.register');
    }
}