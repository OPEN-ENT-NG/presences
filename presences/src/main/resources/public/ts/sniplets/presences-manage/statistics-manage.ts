import {toasts} from 'entcore';
import {Setting, settingService} from '../../services/SettingsService';

declare const window;

enum EVENT_RECOVERY_METHOD {
    HOUR = 'HOUR',
    DAY = 'DAY',
    HALF_DAY = 'HALF_DAY'
}

interface ViewModel {
    setting: Setting

    load(): void

    save(): void
}

const vm: ViewModel = {
    setting: {},
    async load() {
        try {
            let {event_recovery_method} = await settingService.retrieve(window.model.vieScolaire.structure.id);
            vm.setting = {event_recovery_method};
            if (!('event_recovery_method' in vm.setting) || vm.setting.event_recovery_method == null) {
                vm.setting.event_recovery_method = EVENT_RECOVERY_METHOD.HOUR;
            }
            statisticsManage.that.$apply();
        } catch (e) {
            toasts.warning('presences.statistics.manage.event.inventory.method.load.error');
            throw e;
        }
    },
    async save() {
        try {
            await settingService.put(window.model.vieScolaire.structure.id, vm.setting);
            toasts.confirm('presences.statistics.manage.event.inventory.method.save.confirm');
        } catch (e) {
            toasts.confirm('presences.statistics.manage.event.inventory.method.save.error');
            throw e;
        }
    }
};

export const statisticsManage = {
    title: 'presences.statistics.manage.title',
    public: false,
    that: null,
    controller: {
        init: function () {
            this.vm = vm;
            this.setHandler();
            statisticsManage.that = this;
        },
        setHandler: function () {
            this.$watch(() => window.model.vieScolaire.structure, vm.load);
        }
    }
};