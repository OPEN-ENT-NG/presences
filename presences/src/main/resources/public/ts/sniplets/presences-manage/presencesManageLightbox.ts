import {AxiosResponse} from "axios";
import {PRESENCES_ACTION, PRESENCES_DISCIPLINE} from "@common/enum/presences-event";
import {IAngularEvent} from "angular";
import {actionService} from "../../services";
import {ActionRequest} from "@presences/models";
import {toasts} from "entcore";
import {disciplineService} from "../../services";

interface ViewModel {
    safeApply(fn?: () => void): void;

    header: string;
    description: string;
    event: any;
    form: any

    editPresencesManageLightbox: boolean;

    openPresencesManageLightbox(event, args): void;

    closePresencesManageLightbox(): void;

    updatePresencesManageLightbox(): Promise<void>;

    proceedAfterAction(response: AxiosResponse): void;
}

const vm: ViewModel = {
    safeApply: null,
    header: '',
    description: '',
    event: null,
    editPresencesManageLightbox: null,
    form: {} as ActionRequest,

    openPresencesManageLightbox(event, args): void {
        vm.editPresencesManageLightbox = true;
        vm.form = {};
        switch (event.name) {
            case PRESENCES_DISCIPLINE.TRANSMIT:
                vm.header = 'presences.discipline.edit.title';
                vm.description = 'presences.discipline.edit.warning';
                break;
            case PRESENCES_ACTION.TRANSMIT:
                vm.header = 'presence.absence.actions.update';
                vm.description = 'presences.absence.actions.warning.update';
                break;
        }
        /* Assign form to current data */
        vm.form.id = ('id' in args) ? args.id : null;
        vm.form.label = ('label' in args) ? args.label : null;
        vm.form.abbreviation = ('abbreviation' in args) ? args.abbreviation : null;

        vm.form.hidden = ('hidden' in args) ? args.hidden : null;
        vm.event = event;
    },

    closePresencesManageLightbox(): void {
        vm.editPresencesManageLightbox = false;
    },

    async updatePresencesManageLightbox(): Promise<void> {
        switch (vm.event.name) {
            case PRESENCES_DISCIPLINE.TRANSMIT: {
                let response = await disciplineService.update(vm.form);
                vm.proceedAfterAction(response);
                presencesManageLightbox.that.$emit(PRESENCES_DISCIPLINE.SEND_BACK);
                break;
            }
            case PRESENCES_ACTION.TRANSMIT: {
                let response = await actionService.update(vm.form);
                vm.proceedAfterAction(response);
                presencesManageLightbox.that.$emit(PRESENCES_ACTION.SEND_BACK);
                break;
            }
        }

    },

    proceedAfterAction(response: AxiosResponse): void {
        if (response.status === 200 || response.status === 201) {
            vm.editPresencesManageLightbox = false;
        }
        switch (vm.event.name) {
            case PRESENCES_ACTION.TRANSMIT: {
                if (response.status === 200) {
                    toasts.confirm('presences.absence.action.setting.method.update.confirm')
                } else {
                    toasts.warning('presences.absence.action.setting.method.update.error');
                }
            }
        }
    },
};

export const presencesManageLightbox = {
    title: 'presences.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            presencesManageLightbox.that = this;
            vm.safeApply = this.safeApply;
        },
        setHandler: function () {
            this.$on(PRESENCES_DISCIPLINE.TRANSMIT, (event: IAngularEvent, args) => vm.openPresencesManageLightbox(event, args));
            this.$on(PRESENCES_ACTION.TRANSMIT, (event: IAngularEvent, args) => vm.openPresencesManageLightbox(event, args));
        }
    }
};