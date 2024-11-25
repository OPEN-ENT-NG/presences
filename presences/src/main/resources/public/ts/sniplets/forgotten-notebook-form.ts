import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from "@common/model";
import {alertService, forgottenNotebookService, NotebookRequest} from "../services";
import {idiom as lang, moment, toasts} from "entcore";
import {DateUtils} from "@common/utils";
import {AlertType} from "../models";
import {AxiosResponse} from "axios";

console.log("forgottenNotebookFormSniplets");

export enum NOTEBOOK_FORM_EVENTS {
    EDIT = 'notebook-form:edit',
}

declare let window: any;

interface ViewModel {
    openForgottenNotebookLightBox: boolean;
    student: string;
    title: string;
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

    resetForbiddenNotebookCount(studentId: string): Promise<void>;

    getStudentForgottenNoteBookNumberWithThreshold(id?: string): void;

    safeApply(fn?: () => void): void;
}

const vm: ViewModel = {
    safeApply: null,
    title: null,
    openForgottenNotebookLightBox: false,
    student: '',
    form: {} as NotebookRequest,
    count_forgotten_notebook: null,
    threshold_forgotten_notebook: null,
    after_threshold: null,

    openForgottenNotebook(): void {
        vm.openForgottenNotebookLightBox = true;
        forgottenNotebookForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.CREATION);
        vm.safeApply();
    },

    closeForgottenNotebook(): void {
        vm.openForgottenNotebookLightBox = false;
        vm.form = {} as NotebookRequest;
        vm.safeApply();
    },

    setFormParams: ({student, date}) => {
        vm.getStudentForgottenNoteBookNumberWithThreshold(student.id);
        if (vm.form) {
            vm.student = student.displayName ? student.displayName : student.name;
            vm.form.studentId = student.id;
            vm.form.structureId = window.structure.id;
            vm.form.date = date ? moment(date).set({
                second: 0,
                millisecond: 0
            }).toDate() : moment(new Date()).set({second: 0, millisecond: 0}).toDate();
            vm.safeApply();
        }
    },

    async editNotebookForm(obj: { student, notebook }): Promise<void> {
        vm.getStudentForgottenNoteBookNumberWithThreshold(obj.student.id);
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
            this.opened = false;
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

    resetForbiddenNotebookCount: async (studentId: string): Promise<void> => {
        try {
            let response: AxiosResponse = await alertService.resetStudentAlertsCount(window.structure.id, studentId,
                AlertType[AlertType.FORGOTTEN_NOTEBOOK]);

            if (response.status === 200 || response.status === 201) {
                vm.getStudentForgottenNoteBookNumberWithThreshold(studentId);
                toasts.confirm(lang.translate('presences.forgotten.form.reset.succeed'));
            } else {
                toasts.warning(lang.translate('presences.forgotten.form.delete.error'));
            }
            vm.safeApply();
        } catch (e) {
            throw e;
        }
    },


    getStudentForgottenNoteBookNumberWithThreshold: async (id?: string) => {
        // assigning id from parameter as student id, case we did not find it,
        // we use window.item.id fetched from calendar controller
        const studentId: string = id ? id : window.item.id;
        let {count, threshold} = await alertService.getStudentAlerts(window.structure.id, studentId, AlertType[AlertType.FORGOTTEN_NOTEBOOK]);
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
            this.setTitle();
            forgottenNotebookForm.that = this;
            vm.safeApply = this.safeApply;
        },

        setHandler: function () {
            this.$on(NOTEBOOK_FORM_EVENTS.EDIT, (event, args) => vm.editNotebookForm(args));
            this.$on(SNIPLET_FORM_EVENTS.SET_PARAMS, (event, arg) => vm.setFormParams(arg));
        },

        setTitle: function (): void {
            const url: string = window.location.hash;
            const findTerm = (term: string): string => {
                if (url.includes(term)) {
                    return url;
                }
            };
            switch (url) {
                case findTerm('#/registers/'): {
                    vm.title = lang.translate('presences.declare.forgotten.notebook.form.title');
                    break;
                }
                case findTerm('#/calendar/'): {
                    vm.title = lang.translate('presences.forgotten.notebook');
                    break;
                }
            }
        }
    }
};