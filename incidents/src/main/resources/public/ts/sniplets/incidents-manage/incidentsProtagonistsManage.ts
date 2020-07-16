import {INCIDENTS_PROTAGONIST_TYPE_EVENT} from "@common/core/enum/incidents-event";
import {ProtagonistType, ProtagonistTypeRequest, protagonistTypeService} from "@incidents/services";
import {AxiosResponse} from "axios";

declare let window: any;

interface ViewModel {
    safeApply(fn?: () => void): void;

    form: ProtagonistTypeRequest;
    protagonistsType: ProtagonistType[];

    hasProtagonistTypes(): boolean;

    proceedAfterAction(response: AxiosResponse): void;

    get(): Promise<void>;

    create(): Promise<void>;

    toggleVisibility(protagonistType: ProtagonistType): Promise<void>;

    delete(protagonistType: ProtagonistType): Promise<void>

    openIncidentsManageLightbox(protagonistType: ProtagonistType): void;
}

function safeApply() {
    let that = incidentsProtagonistsManage.that;
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
    protagonistsType: [],
    form: {} as ProtagonistTypeRequest,

    async create(): Promise<void> {
        vm.form.structureId = window.model.vieScolaire.structure.id;
        let response = await protagonistTypeService.create(vm.form);
        vm.proceedAfterAction(response);
        vm.form.label = '';
    },

    async delete(incidentType: ProtagonistType): Promise<void> {
        let response = await protagonistTypeService.delete(incidentType.id);
        vm.proceedAfterAction(response);
    },

    hasProtagonistTypes(): boolean {
        return vm.protagonistsType && vm.protagonistsType.length !== 0;
    },

    async get(): Promise<void> {
        vm.protagonistsType = await protagonistTypeService.get(window.model.vieScolaire.structure.id);
        vm.safeApply();
    },

    async toggleVisibility(protagonistTypes: ProtagonistType): Promise<void> {
        protagonistTypes.hidden = !protagonistTypes.hidden;
        let form = {} as ProtagonistTypeRequest;
        form.id = protagonistTypes.id;
        form.hidden = protagonistTypes.hidden;
        form.label = protagonistTypes.label;
        await protagonistTypeService.update(form);
    },

    proceedAfterAction(response: AxiosResponse): void {
        if (response.status === 200 || response.status === 201) {
            vm.get();
        }
    },

    openIncidentsManageLightbox(protagonistType: ProtagonistType): void {
        incidentsProtagonistsManage.that.$emit(INCIDENTS_PROTAGONIST_TYPE_EVENT.SEND, protagonistType);
    },
};

export const incidentsProtagonistsManage = {
    title: 'incidents.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            incidentsProtagonistsManage.that = this;
            vm.safeApply = safeApply;
        },
        setHandler: function () {
            // using vieScolaire.structure to update current structure from viescolaire
            this.$watch(() => window.model.vieScolaire.structure, async () => vm.get());
            this.$on(INCIDENTS_PROTAGONIST_TYPE_EVENT.RESPOND, () => vm.get());
            this.$on('reload', vm.get);
        }
    }
};