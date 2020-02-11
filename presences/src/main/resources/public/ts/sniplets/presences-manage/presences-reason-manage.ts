import {reasonService} from "../../services/ReasonService";
import {AxiosResponse} from "axios";
import {Reason, ReasonRequest} from "@presences/models/Reason";

declare let window: any;

export enum REASON_MANAGE_FORM_EVENTS {
    EDIT = 'reason-manage-form:edit',
}

interface ViewModel {
    safeApply(fn?: () => void): void;

    reasons: Reason[];
    editReasonLightbox: boolean;
    form: ReasonRequest;
    formEdit: ReasonRequest;

    openReasonLightbox(reason: Reason): void;

    closeReasonLightbox(): void;

    isFormValid(form: ReasonRequest): boolean;

    hasReasons(): boolean;

    // interaction
    getReasons(): void;

    createReason(): void;

    setFormEdit(reason: Reason): void;

    updateReason(): void;

    toggleVisibility(reason: Reason): Promise<void>;

    deleteReason(reason: Reason): void;

    proceedAfterAction(response: AxiosResponse): void;

}

function safeApply() {
    let that = presencesReasonManage.that;
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
    reasons: [],
    editReasonLightbox: false,
    form: {} as ReasonRequest,
    formEdit: {} as ReasonRequest,

    openReasonLightbox(reason: Reason): void {
        vm.editReasonLightbox = true;
        vm.setFormEdit(reason);
    },

    closeReasonLightbox(): void {
        vm.editReasonLightbox = false;
    },

    isFormValid(form: ReasonRequest): boolean {
        return !!(form.label);
    },

    hasReasons(): boolean {
        return vm.reasons && vm.reasons.length !== 0;
    },

    async getReasons(): Promise<void> {
        vm.reasons = await reasonService.getReasons(window.model.vieScolaire.structure.id);
        vm.safeApply();
    },

    async createReason(): Promise<void> {
        if (!vm.form.hasOwnProperty("absenceCompliance")) {
            vm.form.absenceCompliance = true;
        }
        if (!vm.form.hasOwnProperty("proving")) {
            vm.form.proving = true;
        }
        vm.form.structureId = window.model.vieScolaire.structure.id;
        let response = await reasonService.create(vm.form);
        vm.proceedAfterAction(response);

        /* Reset form*/
        vm.form.label = '';
        vm.form.proving = false;
        vm.form.absenceCompliance = true;
    },

    async deleteReason(reason: Reason): Promise<void> {
        let response = await reasonService.delete(reason.id);
        vm.proceedAfterAction(response);
    },

    setFormEdit(reason): void {
        let reasonCopy = JSON.parse(JSON.stringify(reason));
        vm.formEdit.id = reasonCopy.id;
        vm.formEdit.absenceCompliance = reasonCopy.absence_compliance;
        vm.formEdit.hidden = reasonCopy.hidden;
        vm.formEdit.proving = reasonCopy.proving;
        vm.formEdit.label = reasonCopy.label;
    },

    async updateReason(): Promise<void> {
        let response = await reasonService.update(vm.formEdit);
        vm.proceedAfterAction(response);
        vm.closeReasonLightbox();
    },

    async toggleVisibility(reason): Promise<void> {
        reason.hidden = !reason.hidden;
        let form = {} as ReasonRequest;
        form.id = reason.id;
        form.absenceCompliance = reason.absence_compliance;
        form.hidden = reason.hidden;
        form.proving = reason.proving;
        form.label = reason.label;
        await reasonService.update(form);
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
            this.$watch(() => window.model.vieScolaire.structure, async () => vm.getReasons());
        }
    }
};