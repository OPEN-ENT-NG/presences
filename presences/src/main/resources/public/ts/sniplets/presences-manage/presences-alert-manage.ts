import {Setting, settingService} from '../../services/SettingsService';
import {idiom as lang, toasts} from "entcore";

declare const window;

interface InputObject {
    label: String,
    settingRef: string,
    threshold: number,
}

interface ViewModel {
    setting: Setting;

    inputThreshold: Array<InputObject>;

    timer: any;

    // interaction
    getThreshold(): void;

    updateThreshold(input: InputObject, isIncr: Boolean): void;

    manualUpdateThreshold(input: InputObject): void;

    save(): void;
}

const vm: ViewModel = {
    setting: {},
    inputThreshold: [],
    timer: null,

    async getThreshold() {
        try {
            let defaultSettings = {
                alert_absence_threshold: 0,
                alert_lateness_threshold: 0,
                alert_incident_threshold: 0,
                alert_forgotten_notebook_threshold: 0
            };
            let structureSettings: any = await settingService.retrieve(window.model.vieScolaire.structure.id);

            vm.setting = {...defaultSettings, ...structureSettings};

            vm.inputThreshold = [
                {
                    label: lang.translate("presences.navigation.absence"),
                    settingRef: "alert_absence_threshold",
                    threshold: vm.setting.alert_absence_threshold
                },
                {
                    label: lang.translate("presences.absence.lateness"),
                    settingRef: "alert_lateness_threshold",
                    threshold: vm.setting.alert_lateness_threshold
                },
                {
                    label: lang.translate("presences.incidents"),
                    settingRef: "alert_incident_threshold",
                    threshold: vm.setting.alert_incident_threshold
                },
                {
                    label: lang.translate("presences.register.event_type.forgotten.notebooks"),
                    settingRef: "alert_forgotten_notebook_threshold",
                    threshold: vm.setting.alert_forgotten_notebook_threshold
                },
            ];
            presencesAlertManage.that.$apply();
        } catch (e) {
            toasts.warning('error');
            throw e;
        }
    },

    updateThreshold: async function (input, isIncr) {
        let threshold = input.threshold;
        vm.setting[input.settingRef] = isIncr ? (threshold + 1) : (threshold > 0 ? threshold - 1 : 0);
        await vm.save();
    },

    manualUpdateThreshold: async function (input) {
        let threshold = input.threshold;

        if (vm.timer != null) clearTimeout(vm.timer);
        vm.timer = setTimeout(async () => {
            vm.setting[input.settingRef] = threshold < 0 ? 0 : threshold;
            await vm.save();
        }, 200);
    },

    async save() {
        try {
            await settingService.put(window.model.vieScolaire.structure.id, vm.setting);
            toasts.confirm('presences.alert.manage.event.method.save.confirm');
        } catch (e) {
            toasts.warning('presences.alert.manage.event.method.save.error');
            throw e;
        }
        await vm.getThreshold();
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
            this.$on('reload', vm.getThreshold);
        }
    }
};