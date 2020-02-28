import {incidentsTypeService, IncidentType, IncidentTypeRequest} from "@incidents/services";
import {AxiosResponse} from "axios";
import {INCIDENTS_TYPE_EVENT} from "@common/enum/incidents-event";

declare let window: any;

interface ViewModel {
    safeApply(fn?: () => void): void;

    form: IncidentTypeRequest;
    incidentsType: IncidentType[];

    isFormValid(form: IncidentTypeRequest): boolean;

    hasIncidentsType(): boolean;

    proceedAfterAction(response: AxiosResponse): void;

    getIncidentsType(): Promise<void>;

    createIncidentType(): Promise<void>;

    toggleVisibility(incidentsType: IncidentType): Promise<void>;

    deleteIncidentType(incidentType: IncidentType): Promise<void>

    openIncidentsManageLightbox(incidentsType: IncidentType): void;
}

function safeApply() {
    let that = incidentsTypeManage.that;
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
    incidentsType: [],
    form: {} as IncidentTypeRequest,

    isFormValid(form: IncidentTypeRequest): boolean {
        return !!(form.label);
    },

    async createIncidentType(): Promise<void> {
        vm.form.structureId = window.model.vieScolaire.structure.id;
        let response = await incidentsTypeService.create(vm.form);
        vm.proceedAfterAction(response);
        vm.form.label = '';
    },

    async deleteIncidentType(incidentType: IncidentType): Promise<void> {
        let response = await incidentsTypeService.delete(incidentType.id);
        vm.proceedAfterAction(response);
    },

    hasIncidentsType(): boolean {
        return vm.incidentsType && vm.incidentsType.length !== 0;
    },

    async getIncidentsType(): Promise<void> {
        vm.incidentsType = await incidentsTypeService.get(window.model.vieScolaire.structure.id);
        vm.safeApply();
    },

    async toggleVisibility(incidentsType: IncidentType): Promise<void> {
        incidentsType.hidden = !incidentsType.hidden;
        let form = {} as IncidentTypeRequest;
        form.id = incidentsType.id;
        form.hidden = incidentsType.hidden;
        form.label = incidentsType.label;
        await incidentsTypeService.update(form);
    },

    proceedAfterAction(response: AxiosResponse): void {
        if (response.status === 200 || response.status === 201) {
            vm.getIncidentsType();
        }
    },

    openIncidentsManageLightbox(incidentsType: IncidentType): void {
        incidentsTypeManage.that.$emit(INCIDENTS_TYPE_EVENT.SEND, incidentsType);
    },
};

export const incidentsTypeManage = {
    title: 'incidents.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            incidentsTypeManage.that = this;
            vm.safeApply = safeApply;
        },
        setHandler: function () {
            // using vieScolaire.structure to update current structure from viescolaire
            this.$watch(() => window.model.vieScolaire.structure, async () => vm.getIncidentsType());
            this.$on(INCIDENTS_TYPE_EVENT.RESPOND, () => vm.getIncidentsType());
            this.$on('reload', vm.getIncidentsType);
        }
    }
};