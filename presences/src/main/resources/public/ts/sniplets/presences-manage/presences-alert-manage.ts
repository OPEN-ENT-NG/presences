import {Setting, settingService} from '../../services/SettingsService';
import {toasts} from "entcore";

declare const window;

interface ViewModel {
    setting: Setting;

    // interaction
    getThreshold(): void;

    incrementThreshold(): void;

    decrementThreshold(): void;

    save(): void;
}

const vm: ViewModel = {
    setting: {},

    async getThreshold() {
        try {
            let defaultSettings = {
                alert_absence_threshold: 0,
                alert_lateness_threshold : 0,
                alert_incident_threshold : 0,
                alert_forgotten_notebook_threshold : 0
            };
            let structureSettings: any = await settingService.retrieve(window.model.vieScolaire.structure.id);

            vm.setting = {...defaultSettings, ...structureSettings};

            presencesAlertManage.that.$apply();
        } catch (e) {
            toasts.warning('error');
            throw e;
        }
    },

    decrementThreshold: function () {
        console.log("-")
    },

    incrementThreshold: function () {
        console.log("+")
    },

    async save() {
        try {
            await settingService.put(window.model.vieScolaire.structure.id, vm.setting);
            toasts.confirm('presences.alert.manage.event.method.save.confirm');
        } catch (e) {
            toasts.confirm('presences.alert.manage.event.method.save.error');
            throw e;
        }
    }
};


export const presencesAlertManage = {
    title: 'presences.alert.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            console.log('alert');
            this.vm = vm;
            this.setHandler();
            presencesAlertManage.that = this;
        },
        setHandler: function () {
            this.$watch(() => window.model.vieScolaire.structure, vm.getThreshold);
        }
    }
};