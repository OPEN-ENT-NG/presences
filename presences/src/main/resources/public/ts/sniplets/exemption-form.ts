import {_, idiom as lang, model, moment, toasts} from 'entcore';
import {Exemption, IStructureSlot, ITimeSlot, Student, Students, Subjects, TimeSlotHourPeriod} from '../models';
import rights from "../rights";
import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from '@common/model'
import {DateUtils} from "@common/utils";
import {ViescolaireService} from "@common/services";
import {IAngularEvent} from "angular";

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
    isEditMode: boolean;
    subjects: Subjects;
    studentsFrom: Students;
    formStudentSelected: any[];
    form: Exemption;
    searchValue: string;
    exemptionType: any;
    typeExemptionSelect: Array<{ label: string, type: string }>;
    typeExemptionSelected: { label: string, type: string };
    structureTimeSlot: IStructureSlot;
    timeSlotHourPeriod: any;

    createExemption(): void;

    setFormParams(obj: any): void;

    editExemption(obj): void;

    editFormRecursive(): void;

    setRecursiveTimeSlot(): void;

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

    selectTimeSlot(hourPeriod: TimeSlotHourPeriod): void;
}

const vm: ViewModel = {
    isCalendar: false,
    days: [],
    form: new Exemption(null, true),
    safeApply: null,
    createExemptionLightBox: false,
    isEditMode: false,
    searchValue: '',
    formStudentSelected: [],
    subjects: new Subjects(),
    studentsFrom: new Students(),
    exemptionType: EXEMPTION_TYPE,
    typeExemptionSelect: [
        {label: lang.translate('presences.exemptions.punctual'), type: EXEMPTION_TYPE.PUNCTUAL},
        {label: lang.translate('presences.exemptions.recursive'), type: EXEMPTION_TYPE.RECURSIVE}
    ],
    timeSlotHourPeriod: TimeSlotHourPeriod,
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
        vm.isEditMode = true;
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
        if (vm.form.exemption_id) {
            vm.typeExemptionSelected = vm.typeExemptionSelect[0];
            vm.form.isRecursiveMode = false;
        } else {
            vm.editFormRecursive();
            vm.form.isRecursiveMode = true;
        }
        vm.safeApply();
    },

    editFormRecursive: () => {
        vm.form.subject = {id: ''};
        vm.typeExemptionSelected = vm.typeExemptionSelect[1];
        vm.days.forEach(day => {
            vm.form.day_of_week.forEach((dayOfWeek: string) => {
                if (day.value === dayOfWeek) {
                    day.isChecked = true;
                }
            });
        });
        vm.setRecursiveTimeSlot();
        vm.form.startDateRecursive = DateUtils.getDateFormat(new Date(vm.form.startDate),
            DateUtils.getTimeFormatDate(vm.form.timeSlotTimePeriod.start.startHour));
        vm.form.endDateRecursive = DateUtils.getDateFormat(new Date(vm.form.endDate),
            DateUtils.getTimeFormatDate(vm.form.timeSlotTimePeriod.end.endHour));
    },

    setRecursiveTimeSlot: () => {
        let start = DateUtils.format(vm.form.startDate, DateUtils.FORMAT["HOUR-MINUTES"]);
        let end = DateUtils.format(vm.form.endDate, DateUtils.FORMAT["HOUR-MINUTES"]);
        vm.form.timeSlotTimePeriod = {start, end: {endHour: "", id: "", name: "", startHour: ""}};
        vm.structureTimeSlot.slots.forEach((slot: ITimeSlot) => {
            if (slot.startHour === start) {
                vm.form.timeSlotTimePeriod.start = slot;
            }
            if (slot.endHour === end) {
                vm.form.timeSlotTimePeriod.end = slot;
            }
            if (vm.form.timeSlotTimePeriod.start && vm.form.timeSlotTimePeriod.end) return;
        });
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
        if (vm.typeExemptionSelected.type === EXEMPTION_TYPE.RECURSIVE) {
            vm.form.startDate = DateUtils.getDateFormat(new Date(vm.form.startDate),
                DateUtils.getTimeFormatDate(vm.form.timeSlotTimePeriod.start.startHour));

            vm.form.endDate = DateUtils.getDateFormat(new Date(vm.form.endDate),
                DateUtils.getTimeFormatDate(vm.form.timeSlotTimePeriod.end.endHour));

            vm.form.startDateRecursive = vm.form.startDate;
            vm.form.endDateRecursive = vm.form.endDate;
        }
        let response = await vm.form.save();
        if (vm.form.id) {
            vm.updateAfterSaveOrDelete(response, lang.translate('presences.exemptions.form.edit.succeed'));
        } else {
            vm.updateAfterSaveOrDelete(response, lang.translate('presences.exemptions.form.create.succeed'));
        }
    },

    closeCreateExemption: () => {
        vm.createExemptionLightBox = false;
        setTimeout(() => {
            vm.isEditMode = false;
            vm.days.map(day => day.isChecked = false);
        });
    },

    getButtonLabel: () => lang.translate(`presences.exemptions${vm.isCalendar ? '.calendar' : ''}.create`),

    setDay: (day: { label: string, value: string, isChecked: boolean }): void => {
        day.isChecked = !day.isChecked;
        vm.form.day_of_week = vm.days.filter(day => day.isChecked).map(day => day.value);
    },

    switchForm: (): void => {
        if (!vm.form.id) {
            vm.form.timeSlotTimePeriod = null;
            vm.form.isRecursiveMode = (vm.typeExemptionSelected.type === EXEMPTION_TYPE.RECURSIVE);
        }
    },

    selectTimeSlot: (hourPeriod: TimeSlotHourPeriod): void => {
        switch (hourPeriod) {
            case TimeSlotHourPeriod.START_HOUR:
                let start = vm.form.timeSlotTimePeriod.start != null ? DateUtils.getDateFormat(new Date(vm.form.startDate),
                    DateUtils.getTimeFormatDate(vm.form.timeSlotTimePeriod.start.startHour)) : null;
                vm.form.startDateRecursive = start;
                break;
            case TimeSlotHourPeriod.END_HOUR:
                let end = vm.form.timeSlotTimePeriod.end != null ? DateUtils.getDateFormat(new Date(vm.form.endDate),
                    DateUtils.getTimeFormatDate(vm.form.timeSlotTimePeriod.end.endHour)) : null;
                vm.form.endDateRecursive = end;
                break;
            default:
                return;
        }
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
            this.$on(EXEMPTIONS_FORM_EVENTS.EDIT, (event: IAngularEvent, arg) => vm.editExemption(arg));
            this.$on(SNIPLET_FORM_EVENTS.SET_PARAMS, (event: IAngularEvent, arg) => vm.setFormParams(arg));
            this.$watch(() => window.structure, async () => {
                await vm.subjects.sync(window.structure.id);
                this.vm.structureTimeSlot = await ViescolaireService.getSlotProfile(window.structure.id);
                vm.safeApply();
            });
        }
    }
};