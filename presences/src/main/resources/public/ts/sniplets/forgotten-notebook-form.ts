import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from "@common/model";
import {forgottenNotebookService, NotebookRequest} from "../services";
import {idiom as lang, moment, toasts} from "entcore";
import {DateUtils} from "@common/utils";

console.log("forgottenNotebookFormSnipplets");

declare let window: any;

interface ViewModel {
    openForgottenNotebookLightBox: boolean;
    student: string;
    form: NotebookRequest;

    openForgottenNotebook(): void;

    closeForgottenNotebook(): void;

    setFormParams(obj): void;

    createForbiddenNotebook(): Promise<void>;

    updateForbiddenNotebook(): Promise<void>;

    deleteForbiddenNotebook(): Promise<void>;

    safeApply(fn?: () => void): void;
}

const vm: ViewModel = {
    safeApply: null,
    openForgottenNotebookLightBox: false,
    student: '',
    form: {} as NotebookRequest,

    openForgottenNotebook(): void {
        vm.openForgottenNotebookLightBox = true;
        forgottenNotebookForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.CREATION);
        vm.safeApply();
    },
    closeForgottenNotebook(): void {
        vm.openForgottenNotebookLightBox = false;
    },

    setFormParams: ({student}) => {
        if (vm.form) {
            vm.student = student.displayName;
            vm.form.studentId = student.id;
            vm.form.structureId = window.structure.id;
            vm.form.date = moment(new Date()).set({second: 0, millisecond: 0}).toDate();
            vm.safeApply();
        }
    },

    async createForbiddenNotebook(): Promise<void> {
        vm.form.date = moment(vm.form.date).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
        let response = await forgottenNotebookService.create(vm.form);
        if (response.status == 200 || response.status == 201) {
            vm.closeForgottenNotebook();
            toasts.confirm(lang.translate('presences.forgotten.form.create.succeed'));
        } else {
            toasts.warning(response.data.toString());
        }
        forgottenNotebookForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.FILTER);
        vm.safeApply();
    },

    async updateForbiddenNotebook(): Promise<void> {
        let response = await forgottenNotebookService.update(window.structure.id, vm.form);
        if (response.status == 200 || response.status == 201) {
            vm.closeForgottenNotebook();
            toasts.confirm(lang.translate('presences.forgotten.form.edit.succeed'));
        } else {
            toasts.warning(response.data.toString());
        }
        forgottenNotebookForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.EDIT);
        vm.safeApply();
    },

    async deleteForbiddenNotebook(): Promise<void> {
        let response = await forgottenNotebookService.delete(window.structure.id);
        if (response.status == 200 || response.status == 201) {
            vm.closeForgottenNotebook();
            toasts.confirm(lang.translate('presences.forgotten.form.delete.succeed'));
        } else {
            toasts.warning(response.data.toString());
        }
        forgottenNotebookForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.DELETE);
        vm.safeApply();
    }
};

export const forgottenNotebookForm = {
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            forgottenNotebookForm.that = this;
            vm.safeApply = this.safeApply;
        },
        setHandler: function () {
            this.$on(SNIPLET_FORM_EVENTS.SET_PARAMS, (event, arg) => vm.setFormParams(arg));
        }
    }
};