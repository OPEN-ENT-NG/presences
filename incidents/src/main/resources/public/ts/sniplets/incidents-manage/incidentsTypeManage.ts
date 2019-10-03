import {IncidentType} from "@incidents/services";

declare let window: any;

interface ViewModel {
    safeApply(fn?: () => void): void;
    form: any;
    incidentsType: IncidentType[];
    isFormValid(form: any): boolean;
    createIncidentType(): Promise<void>;
    hasIncidentsType(): boolean;

}

const vm: ViewModel = {
    safeApply: null,
    incidentsType: [],
    form: {},

    isFormValid(form: any): boolean {
        return !!(form.label);
    },

    async createIncidentType(): Promise<void> {
        // if (!vm.form.hasOwnProperty("absenceCompliance") ) {
        //     vm.form.absenceCompliance = true;
        // }
        vm.form.structureId = window.model.vieScolaire.structure.id;
        // let response = await reasonService.create(vm.form);
        // vm.proceedAfterAction(response);
        // vm.form.label = '';
    },

    hasIncidentsType(): boolean {
        return vm.incidentsType && vm.incidentsType.length !== 0;
    },


};

export const incidentsTypeManage = {
    title: 'incidents.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            incidentsTypeManage.that = this;
            vm.safeApply = this.safeApply;
        }
    }
};