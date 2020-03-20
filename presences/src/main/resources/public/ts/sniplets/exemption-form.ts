import {_, idiom as lang, model, moment, toasts} from 'entcore';
import {Exemption, IStructureSlot, Student, Students, Subjects} from '../models';
import rights from "../rights";
import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from '@common/model'
import {DateUtils} from "@common/utils";
import {ViescolaireService} from "@common/services";

console.log("ExemptionForm sniplet");

export enum EXEMPTIONS_FORM_EVENTS {
    EDIT = 'exemptions-form:edit',
}

enum EXEMPTION_TYPE {
    PUNCTUAL = 'punctual',
    RECURSIVE = 'recursive'
}

declare let window: any;

interface ViewModel {
    isCalendar: boolean;
    days: Array<{ label: string, value: string, isChecked: boolean }>;
    createExemptionLightBox: boolean;
    subjects: Subjects;
    studentsFrom: Students;
    formStudentSelected: any[];
    form: Exemption;
    searchValue: string;
    exemptionType: any;
    typeExemptionSelect: Array<{ label: string, type: string }>;
    typeExemptionSelected: { label: string, type: string };
    structureTimeSlot: IStructureSlot;

    createExemption(): void;

    setFormParams(obj: any): void;

    editExemption(obj): void;

    closeCreateExemption(): void;

    selectStudentForm(model: Student, student): void;

    excludeStudentFromForm(student): void;

    searchFormByStudent(searchText: string): void;

    saveExemption(): void;

    updateAfterSaveOrDelete(response: any, message: string): Promise<void>;

    deleteExemption(): void;

    isValidDate(startDate, endDate): boolean;

    safeApply(fn?: () => void): void;

    getButtonLabel(): string;

    setDay(day: { label: string, value: string, isChecked: boolean }): void;

    switchForm(): void;

    selectTimeSlot(): void;
}

const vm: ViewModel = {
    isCalendar: false,
    days: [],
    form: new Exemption(null, true),
    safeApply: null,
    createExemptionLightBox: false,
    searchValue: '',
    formStudentSelected: [],
    subjects: new Subjects(),
    studentsFrom: new Students(),
    exemptionType: EXEMPTION_TYPE,
    typeExemptionSelect: [
        {label: lang.translate('presences.exemptions.punctual'), type: EXEMPTION_TYPE.PUNCTUAL},
        {label: lang.translate('presences.exemptions.recursive'), type: EXEMPTION_TYPE.RECURSIVE}
    ],
    typeExemptionSelected: null,
    structureTimeSlot: {} as IStructureSlot,

    createExemption: (): void => {
        vm.createExemptionLightBox = true;
        vm.form = new Exemption(window.structure.id, true);
        vm.studentsFrom.searchValue = "";
        vm.form.subject = vm.subjects.findEPS();
        if (!vm.form.subject) {
            vm.form.subject = vm.subjects.all[0];
        }
        vm.typeExemptionSelected = vm.typeExemptionSelect[0];
        vm.days.map(day => day.isChecked = false);
        exemptionForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.CREATION);
        vm.safeApply()
    },

    updateAfterSaveOrDelete: async function (response, message: string) {
        if (response.status == 200 || response.status == 201) {
            vm.closeCreateExemption();
            toasts.confirm(message);
        } else {
            toasts.warning(response.data.toString());
        }
        exemptionForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.FILTER);
        vm.safeApply();
    },

    setFormParams: ({student, start_date, end_date}) => {
        if (vm.form) {
            vm.selectStudentForm(null, student);
            vm.form.startDate = moment(start_date).toDate();
            vm.form.endDate = moment(end_date).toDate();
            vm.safeApply();
        }
    },

    isValidDate: (startDate, endDate): boolean => {
        if (startDate || endDate) {
            let start = DateUtils.format(startDate, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
            let end = DateUtils.format(endDate, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
            return start <= end;
        }
        return false;
    },

    editExemption: (obj): void => {
        if (!model.me.hasWorkflow(rights.workflow['manageExemption'])) {
            return;
        }
        vm.createExemptionLightBox = true;
        vm.form = _.clone(obj);
        let studentTmp = new Student(obj.student);
        vm.form.students = [studentTmp];
        vm.formStudentSelected = [studentTmp];
        vm.form.subject = _.chain(vm.subjects.all)
            .filter((item) => {
                return item.id == obj.subjectId;
            })
            .first()
            .value();
        if (!('timeSlot' in vm.form) && !vm.form.timeSlot) {
            vm.typeExemptionSelected = vm.typeExemptionSelect[0];
        }
        vm.safeApply();
    },
    selectStudentForm: (model: Student, student) => {
        if (!_.find(vm.form.students, student)) {
            vm.form.students.push(student);
        }

        vm.studentsFrom.all = [];
        vm.studentsFrom.searchValue = "";
    },
    searchFormByStudent: async (searchText: string) => {
        await vm.studentsFrom.search(window.structure.id, searchText);
        vm.safeApply();
    },
    excludeStudentFromForm: (student) => {
        vm.form.students = _.without(vm.form.students, student);
    },
    deleteExemption: async () => {
        let response = await vm.form.delete();
        vm.updateAfterSaveOrDelete(response, lang.translate('presences.exemptions.delete.succeed'));
    },
    saveExemption: async () => {
        let response = await vm.form.save();
        if (vm.form.id) {
            vm.updateAfterSaveOrDelete(response, lang.translate('presences.exemptions.form.edit.succeed'));
        } else {
            vm.updateAfterSaveOrDelete(response, lang.translate('presences.exemptions.form.create.succeed'));
        }
    },
    closeCreateExemption: () => {
        vm.createExemptionLightBox = false;
    },
    getButtonLabel: () => lang.translate(`presences.exemptions${vm.isCalendar ? '.calendar' : ''}.create`),

    setDay: (day: { label: string, value: string, isChecked: boolean }): void => {
        day.isChecked = !day.isChecked;
        vm.form.dayOfWeek = vm.days.filter(day => day.isChecked).map(day => day.value);
    },

    switchForm: (): void => {
        if (!vm.form.id) {
            if (vm.typeExemptionSelected.type === EXEMPTION_TYPE.RECURSIVE) {
                vm.form.timeSlot = {endHour: "", id: "", name: "", startHour: ""};
            } else {
                vm.form.timeSlot = null
            }
        }
    },

    selectTimeSlot: (): void => {
        vm.form.startDateRecursive = DateUtils.getDateFormat(new Date(vm.form.startDate), DateUtils.getTimeFormatDate(vm.form.timeSlot.startHour));
        vm.form.endDateRecursive = DateUtils.getDateFormat(new Date(vm.form.endDate), DateUtils.getTimeFormatDate(vm.form.timeSlot.endHour));
        console.log("form: ", vm.form);
    }
};

export const exemptionForm = {
    title: 'presences.exemptions.form.sniplet.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.vm.isCalendar = new RegExp('\#\/calendar').test(window.location.hash);
            this.vm.structureTimeSlot = await ViescolaireService.getSlotProfile(window.structure.id);
            this.vm.days = [
                {label: 'presences.monday', value: 'MONDAY', isChecked: false},
                {label: 'presences.tuesday', value: 'TUESDAY', isChecked: false},
                {label: 'presences.wednesday', value: 'WEDNESDAY', isChecked: false},
                {label: 'presences.thursday', value: 'THURSDAY', isChecked: false},
                {label: 'presences.friday', value: 'FRIDAY', isChecked: false},
            ];
            this.setHandler();
            exemptionForm.that = this;
            vm.safeApply = this.safeApply;
        },
        setHandler: function () {
            this.$on(EXEMPTIONS_FORM_EVENTS.EDIT, (event, arg) => vm.editExemption(arg));
            this.$on(SNIPLET_FORM_EVENTS.SET_PARAMS, (event, arg) => vm.setFormParams(arg));
            this.$watch(() => window.structure, async () => {
                await vm.subjects.sync(window.structure.id);
                vm.safeApply();
            });
        }
    }
};