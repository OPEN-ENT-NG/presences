import {Reason, ReasonRequest, reasonService} from "../../services/ReasonService";
import {AxiosResponse} from "axios";

declare let window: any;

export enum REASON_MANAGE_FORM_EVENTS {
    EDIT = 'reason-manage-form:edit',
}

interface ViewModel {
    safeApply(fn?: () => void): void;
    reasons: Reason[];
    editReasonLightbox: boolean;
    form: ReasonRequest;
    openReasonLightbox(reason: Reason): void;
    closeReasonLightbox(): void;
    isFormValid(): boolean;
    hasReasons(): boolean;

    // interaction
    getReasons(): void;
    createReason(): void;
    setFormEdit(reason: Reason): void;
    updateReason(reasonId: number): void;
    hideReason(reason: Reason): void;
    deleteReason(reason: Reason): void;
    proceedAfterAction(response: AxiosResponse): void;

}

function safeApply() {
    let that = presencesReasonManage.that;
    return new Promise((resolve, reject) => {
        var phase = that.$root.$$phase;
        if(phase === '$apply' || phase === '$digest') {
            if(resolve && (typeof(resolve) === 'function')) resolve();
        } else {
            if (resolve && (typeof(resolve) === 'function')) that.$apply(resolve);
            else that.$apply();
        }
    });
}

const vm: ViewModel = {
    safeApply: null,
    reasons: [],
    editReasonLightbox: false,
    form: {} as ReasonRequest,

    openReasonLightbox(reason: Reason): void {
        vm.editReasonLightbox = true;
        vm.setFormEdit(reason);
    },

    closeReasonLightbox(): void {
        vm.editReasonLightbox = false;
    },

    isFormValid(): boolean {
        return !!(vm.form.label);
    },

    hasReasons(): boolean {
        return vm.reasons && vm.reasons.length !== 0;
    },

    async getReasons(): Promise<void> {
        vm.reasons = await reasonService.getReasons(window.model.vieScolaire.structure.id);
        vm.safeApply();
    },

    async createReason(): Promise<void> {
        if (!vm.form.hasOwnProperty("absenceCompliance") ) {
            vm.form.absenceCompliance = true;
        }
        vm.form.structureId = window.model.vieScolaire.structure.id;
        let response = await reasonService.create(vm.form);
        vm.proceedAfterAction(response);
    },

    async deleteReason(reason: Reason): Promise<void> {
        let response = await reasonService.delete(reason.id);
        vm.proceedAfterAction(response);
    },

    async hideReason(reason: Reason): Promise<void> {

    },

    setFormEdit(reason): void {
        vm.form.id = reason.id;
        vm.form.absenceCompliance = reason.absence_compliance;
        vm.form.regularisable = reason.regularisable;
        vm.form.label = reason.label;
    },

    async updateReason(reason): Promise<void> {
        console.log("reason:", reason);
        vm.form = {} as ReasonRequest;
    },


    proceedAfterAction(response: AxiosResponse): void {
        if (response.status === 200 || response.status === 201) {
            vm.getReasons();
        }
    }
};

export const presencesReasonManage = {
    title: 'presences.absence.reason.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            presencesReasonManage.that = this;
            vm.safeApply = safeApply;
        },
        setHandler: function () {
            // using vieScolaire.structure to update current structure from viescolaire
            this.$watch(() =>  window.model.vieScolaire.structure, async () => vm.getReasons());
        }
    }
};