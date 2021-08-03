import {toasts} from 'entcore';
import {Setting, settingService} from '../../services/SettingsService';

declare const window;

interface ViewModel {
    settings: Setting

    loadMultipleSlotsSetting(): Promise<void>

    switchAllowMultipleSlots(): Promise<void>
}

const vm: ViewModel = {
    settings: {},
    async loadMultipleSlotsSetting(): Promise<void> {
        try {
            let {allow_multiple_slots} = await settingService.retrieve(window.model.vieScolaire.structure.id);
            vm.settings = {allow_multiple_slots};
            presencesMultipleSlotsManage.that.$apply();
        } catch (e) {
            toasts.warning('presences.multiple.slots.manage.load.error');
            throw e;
        }
    },
    async switchAllowMultipleSlots(): Promise<void> {
        try {
            await settingService.put(window.model.vieScolaire.structure.id, vm.settings);
            toasts.confirm('presences.multiple.slots.manage.save.confirm');
        } catch (e) {
            toasts.confirm('presences.multiple.slots.manage.save.error');
            throw e;
        }
    }
};

export const presencesMultipleSlotsManage = {
    title: 'presences.multiple.slots.manage.title',
    public: false,
    that: null,
    controller: {
        init: function () {
            this.vm = vm;
            this.setHandler();
            presencesMultipleSlotsManage.that = this;
        },
        setHandler: function () {
            this.$watch(() => window.model.vieScolaire.structure, vm.loadMultipleSlotsSetting);
            this.$on('reload', vm.loadMultipleSlotsSetting);
        }
    }
};
