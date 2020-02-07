import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from "@common/model";
import {alertService, forgottenNotebookService, NotebookRequest} from "../services";
import {idiom as lang, moment, toasts} from "entcore";
import {DateUtils} from "@common/utils";
import {AlertType} from "../models";

console.log("forgottenNotebookFormSnipplets");

export enum NOTEBOOK_FORM_EVENTS {
    EDIT = 'notebook-form:edit',
}

declare let window: any;

interface ViewModel {
    openForgottenNotebookLightBox: boolean;
    student: string;
    form: NotebookRequest;
    count_forgotten_notebook: number;
    threshold_forgotten_notebook: number;
    after_threshold: number;

    openForgottenNotebook(): void;

    editNotebookForm({student, notebook}): void;

    closeForgottenNotebook(): void;

    setFormParams(obj): void;

    createForbiddenNotebook(): Promise<void>;

    updateForbiddenNotebook(): Promise<void>;

    deleteForbiddenNotebook(): Promise<void>;

    getStudentForgottenNoteBookNumberWithThreshold(): void

    safeApply(fn?: () => void): void;
}

const vm: ViewModel = {
    safeApply: null,
    openForgottenNotebookLightBox: false,
    student: '',
    form: {} as NotebookRequest,
    count_forgotten_notebook: null,
    threshold_forgotten_notebook: null,
    after_threshold: null,

    openForgottenNotebook(): void {
        vm.openForgottenNotebookLightBox = true;
        forgottenNotebookForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.CREATION);
        this.getStudentForgottenNoteBookNumberWithThreshold();
        vm.safeApply();
    },

    closeForgottenNotebook(): void {
        vm.openForgottenNotebookLightBox = false;
        vm.form = {} as NotebookRequest;
        vm.safeApply();
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

    async editNotebookForm(obj: { student, notebook }): Promise<void> {
        this.getStudentForgottenNoteBookNumberWithThreshold();
        vm.openForgottenNotebookLightBox = true;
        vm.student = obj.student.displayName;
        vm.form.id = obj.notebook.id;
        vm.form.studentId = obj.notebook.student_id;
        vm.form.structureId = obj.notebook.structure_id;
        vm.form.date = obj.notebook.date;
        vm.safeApply();
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
        vm.form.date = moment(vm.form.date).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
        let response = await forgottenNotebookService.update(vm.form.id, vm.form);
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
        let response = await forgottenNotebookService.delete(vm.form.id);
        if (response.status == 200 || response.status == 201) {
            vm.closeForgottenNotebook();
            toasts.confirm(lang.translate('presences.forgotten.form.delete.succeed'));
        } else {
            toasts.warning(response.data.toString());
        }
        forgottenNotebookForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.DELETE);
        vm.safeApply();
    },

    getStudentForgottenNoteBookNumberWithThreshold: async () => {
        let {count, threshold} = await alertService.getStudentAlerts(window.structure.id, window.item.id, AlertType[AlertType.FORGOTTEN_NOTEBOOK]);
        vm.count_forgotten_notebook = count ? count : 0;
        vm.threshold_forgotten_notebook = threshold ? threshold : 0;
        vm.after_threshold = count - threshold > 0 ? count - threshold : 0;
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
            this.$on(NOTEBOOK_FORM_EVENTS.EDIT, (event, args) => vm.editNotebookForm(args));
            this.$on(SNIPLET_FORM_EVENTS.SET_PARAMS, (event, arg) => vm.setFormParams(arg));
        },
    }
};