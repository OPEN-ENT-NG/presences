import {Setting, settingService} from '../../services/SettingsService';
import {Controller, idiom as lang, toasts} from "entcore";
import {IScope} from "angular";

declare const window;

interface InputObject {
    label: String,
    settingRef: string,
    threshold: number,
}

interface IViewModel {
    setting: Setting;

    inputThreshold: Array<InputObject>;

    timer: any;

    // interaction
    getThreshold(alertManage: AlertManage): void;

    updateThreshold(input: InputObject, isIncr: Boolean): void;

    manualUpdateThreshold(input: InputObject): void;

    save(): void;
}

class AlertManage implements IViewModel {
    setting: Setting
    inputThreshold: Array<InputObject>
    timer: number

    constructor(private $scope: IScope) {
        this.setting = null;
        this.inputThreshold = [];
        this.timer = 0;
        this.setHandler();
    }

    setHandler() {
        this.$scope.$watch(() => window.model.vieScolaire.structure, () => this.getThreshold());
        this.$scope.$on('reload', () => this.getThreshold());
    }

    getThreshold() {
        let defaultSettings: Setting = {
            alert_absence_threshold: 0,
            alert_lateness_threshold: 0,
            alert_incident_threshold: 0,
            alert_forgotten_notebook_threshold: 0,
            exclude_alert_absence_no_reason: false,
            exclude_alert_lateness_no_reason: false,
            exclude_alert_forgotten_notebook: false
        };
        settingService.retrieve(window.model.vieScolaire.structure.id)
            .then(structureSettings => {
                this.setting = {...defaultSettings, ...structureSettings};

                this.inputThreshold = [
                    {
                        label: lang.translate("presences.navigation.absence"),
                        settingRef: "alert_absence_threshold",
                        threshold: this.setting.alert_absence_threshold
                    },
                    {
                        label: lang.translate("presences.absence.lateness"),
                        settingRef: "alert_lateness_threshold",
                        threshold: this.setting.alert_lateness_threshold
                    },
                    {
                        label: lang.translate("presences.incidents"),
                        settingRef: "alert_incident_threshold",
                        threshold: this.setting.alert_incident_threshold
                    },
                    {
                        label: lang.translate("presences.register.event_type.forgotten.notebooks"),
                        settingRef: "alert_forgotten_notebook_threshold",
                        threshold: this.setting.alert_forgotten_notebook_threshold
                    },
                ];
                this.$scope.$apply();
            })
            .catch(e => {
                toasts.warning('error');
                throw e;
            });
    }

    async updateThreshold(input, isIncr) {
        let threshold = input.threshold;
        this.setting[input.settingRef] = isIncr ? (threshold + 1) : (threshold > 0 ? threshold - 1 : 0);
        await this.save();
    }

    async manualUpdateThreshold(input) {
        let threshold = input.threshold;

        if (this.timer != null) clearTimeout(this.timer);
        this.timer = setTimeout(async () => {
            this.setting[input.settingRef] = threshold < 0 ? 0 : threshold;
            await this.save();
        }, 200);
    }

    async save() {
        try {
            await settingService.put(window.model.vieScolaire.structure.id, this.setting);
            toasts.confirm('presences.alert.manage.event.method.save.confirm');
        } catch (e) {
            toasts.warning('presences.alert.manage.event.method.save.error');
            throw e;
        }
        this.getThreshold();
    }
}

export const presencesAlertManage = {
    vm: null,
    title: 'presences.alert.title',
    public: false,
    controller: {
        init: function (): void {
            console.log('alert');
            this.vm = new AlertManage(this);
        }
    }
};