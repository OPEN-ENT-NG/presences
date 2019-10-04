import {incidentsTypeService, IncidentTypeRequest} from "@incidents/services";

export enum INCIDENT_FORM_EVENTS {
    EDIT_TYPE = 'type-incident-form:edit',
    UPDATE_EDIT_TYPE = 'type-incident-form:update',
}

interface ViewModel {
    safeApply(fn?: () => void): void;
    form: IncidentTypeRequest;
    editIncidentsManageLightbox: boolean;
    openIncidentsManageLightbox(event, args): void;
    closeIncidentsManageLightbox(): void;
    updateIncidentsManageLightbox(): Promise<void>;
}

const vm: ViewModel = {
    safeApply: null,
    editIncidentsManageLightbox: null,
    form: {} as IncidentTypeRequest,

    openIncidentsManageLightbox(event, args): void {
        vm.editIncidentsManageLightbox = true;

        /* Assign form to current data */
        vm.form.id = args.id;
        vm.form.label = args.label;
        vm.form.hidden = args.hidden;
    },

    closeIncidentsManageLightbox(): void {
        vm.editIncidentsManageLightbox = false;
    },

    async updateIncidentsManageLightbox(): Promise<void> {
        let response = await incidentsTypeService.update(vm.form);
        if (response.status === 200 || response.status === 201) {
            incidentsManageLightbox.that.$emit(INCIDENT_FORM_EVENTS.UPDATE_EDIT_TYPE);
            vm.editIncidentsManageLightbox = false;
        }
    },
};

export const incidentsManageLightbox = {
    title: 'incidents.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            incidentsManageLightbox.that = this;
            vm.safeApply = this.safeApply;
        },
        setHandler: function () {
            this.$on(INCIDENT_FORM_EVENTS.EDIT_TYPE, (event, args) => vm.openIncidentsManageLightbox(event, args));
        }
    }
};