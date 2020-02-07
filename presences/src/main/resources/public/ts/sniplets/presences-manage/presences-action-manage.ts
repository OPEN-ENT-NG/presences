import {actionService} from "../../services";
import {Action, ActionRequest} from "../../models";
import {PRESENCES_ACTION} from "@common/enum/presences-event";
import {AxiosResponse} from "axios";
import {toasts} from "entcore";

declare const window: any;

interface ViewModel {
    safeApply(fn?: () => void): void;

    actions: Action[];
    form: ActionRequest;
    editActionLightbox: boolean;

    hasActions(): boolean;

    // interaction
    getAction(): void;

    createAction(): void;

    deleteAction(action: Action): void;

    proceedAfterAction(response: AxiosResponse): void;

    openActionLightbox(action: Action): void;
}

function safeApply() {
    let that = presencesActionManage.that;
    return new Promise((resolve, reject) => {
        var phase = that.$root.$$phase;
        if (phase === '$apply' || phase === '$digest') {
            if (resolve && (typeof (resolve) === 'function')) resolve();
        } else {
            if (resolve && (typeof (resolve) === 'function')) that.$apply(resolve);
            else that.$apply();
        }
    });
}

const vm: ViewModel = {
    safeApply: null,
    actions: [],
    form: {} as ActionRequest,
    editActionLightbox: false,

    async getAction(): Promise<void> {
        let response = await actionService.getActions(window.model.vieScolaire.structure.id);
        vm.actions = response;
        vm.safeApply();
    },

    async createAction(): Promise<void> {
        vm.form.structureId = window.model.vieScolaire.structure.id;
        let response = await actionService.create(vm.form);
        vm.proceedAfterAction(response);
        if (response) {
            toasts.confirm('presences.absence.action.setting.method.create.confirm')
        } else {
            toasts.warning('presences.absence.action.setting.method.create.error');
        }
        /* Reset form*/
        vm.form.label = '';
        vm.form.abbreviation = '';
    },

    async deleteAction(action: Action): Promise<void> {
        let response = await actionService.delete(action.id);
        vm.proceedAfterAction(response);
        if (response) {
            toasts.confirm('presences.absence.action.setting.method.delete.confirm')
        } else {
            toasts.warning('presences.absence.action.setting.method.delete.error');
        }
    },

    proceedAfterAction(response: AxiosResponse): void {
        if (response.status === 200 || response.status === 201) {
            vm.getAction();
        }
    },

    openActionLightbox(action: Action): void {
        vm.editActionLightbox = true;
        presencesActionManage.that.$emit(PRESENCES_ACTION.SEND, action);
    },

    hasActions(): boolean {
        return vm.actions && vm.actions.length !== 0;
    },
};


export const presencesActionManage = {
    title: 'presences.action.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            console.log('action');
            this.vm = vm;
            this.setHandler();
            presencesActionManage.that = this;
            vm.safeApply = safeApply;
        },
        setHandler: function () {
            // using vieScolaire.structure to update current structure from viescolaire
            this.$watch(() => window.model.vieScolaire.structure, async () => vm.getAction());
            this.$on(PRESENCES_ACTION.RESPOND, () => vm.getAction());
        }
    }
};
