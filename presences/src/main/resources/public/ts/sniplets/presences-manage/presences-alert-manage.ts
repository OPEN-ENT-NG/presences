import {Setting, settingService} from '../../services/SettingsService';
import {idiom as lang, toasts} from "entcore";

declare const window;

interface InputObject {
    label: String,
    threshold: number,
}

interface ViewModel {
    setting: Setting;

    inputThreshold: Array<InputObject>;

    // interaction
    getThreshold(): void;

    incrementThreshold(label): void;

    decrementThreshold(label): void;

    save(): void;
}

const vm: ViewModel = {
    setting: {},
    inputThreshold: [],

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

            vm.inputThreshold = [
                {
                    label: lang.translate("presences.navigation.absence"),
                    threshold: vm.setting.alert_absence_threshold
                },
                {
                    label: lang.translate("presences.absence.retards"),
                    threshold: vm.setting.alert_lateness_threshold
                },
                {
                    label: lang.translate("presences.incidents"),
                    threshold: vm.setting.alert_incident_threshold
                },
                {
                    label: lang.translate("presences.navigation.notebook"),
                    threshold: vm.setting.alert_forgotten_notebook_threshold
                },
            ];

            presencesAlertManage.that.$apply();
        } catch (e) {
            toasts.warning('error');
            throw e;
        }
    },

    decrementThreshold: async function (label) {
        if (label == lang.translate("presences.navigation.absence")) {
            if(vm.setting.alert_absence_threshold > 0) {
                vm.setting.alert_absence_threshold--;
            }
            vm.save();
        }
        if (label == lang.translate("presences.absence.retards")) {
            if(vm.setting.alert_lateness_threshold > 0) {
                vm.setting.alert_lateness_threshold--;
            }
            vm.save();
        }
        if (label == lang.translate("presences.incidents")) {
            if(vm.setting.alert_incident_threshold > 0) {
                vm.setting.alert_incident_threshold--;
            }
            vm.save();
        }
        if (label == lang.translate("presences.register.event_type.forgotten.notebooks")) {
            if(vm.setting.alert_forgotten_notebook_threshold > 0) {
                vm.setting.alert_forgotten_notebook_threshold--;
            }
            vm.save();
        }
    },

    incrementThreshold: async function (label) {
        if(label == lang.translate("presences.navigation.absence")) {
            if(!vm.setting.alert_absence_threshold) {
                vm.setting.alert_absence_threshold = 1;
            }
            else {
                vm.setting.alert_absence_threshold ++;
            }
            vm.save();
            await vm.getThreshold();
        }
        if(label == lang.translate("presences.absence.retards")) {
            if(!vm.setting.alert_lateness_threshold) {
                vm.setting.alert_lateness_threshold = 1;
            }
            else {
                vm.setting.alert_lateness_threshold ++;
            }
            vm.save();
            await vm.getThreshold();
        }
        if(label == lang.translate("presences.incidents")) {
            if(!vm.setting.alert_incident_threshold) {
                vm.setting.alert_incident_threshold = 1;
            }
            else {
                vm.setting.alert_incident_threshold ++;
            }
            vm.save();
            await vm.getThreshold();
        }
        if(label == lang.translate("presences.register.event_type.forgotten.notebooks")) {
            if(!vm.setting.alert_forgotten_notebook_threshold) {
                vm.setting.alert_forgotten_notebook_threshold = 1;
            }
            else {
                vm.setting.alert_forgotten_notebook_threshold ++;
            }
            vm.save();
            await vm.getThreshold();
        }
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