import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from "@common/model";
import {Absence} from "../models";
import {idiom as lang, model, moment, toasts} from "entcore";
import {eventService} from "../services";

declare let window: any;

interface ViewModel {
    createAbsenceLightBox: boolean;

    form: any;
    reasonsType: any;
    openAbsenceLightbox(): void;

    setFormParams(obj): void;

    editAbsenceForm(obj): void;

    isFormValid(): void;
    createAbsence(): Promise<void>;

    closeAbsenceLightbox(): void;

    safeApply(fn?: () => void): void;
}

function getDateFormat(date: Date, dateTime: Date): string {
    return moment(moment(date)
        .format('YYYY-MM-DD') + ' ' + moment(dateTime)
        .format('HH:mm'), 'YYYY-MM-DD HH:mm')
        .format('YYYY-MM-DD HH:mm');
}

const vm: ViewModel = {
    safeApply: null,
    createAbsenceLightBox: false,
    reasonsType: null,
    form: null,

    openAbsenceLightbox(): void {
        vm.createAbsenceLightBox = true;
        vm.form = new Absence(null, null, null, null);
        absenceForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.CREATION);
        vm.safeApply();
    },

    setFormParams: ({student, start_date, end_date}) => {
        if (vm.form) {

            vm.form.student_id = student.id;
            vm.form.startDate = moment(start_date).toDate();
            vm.form.startDateTime = moment(new Date()).set({second: 0, millisecond: 0}).toDate();
            vm.form.endDate = moment(end_date).toDate();
            vm.form.endDateTime = moment(new Date().setHours(17)).set({minute: 0, second: 0, millisecond: 0}).toDate();

            vm.form.start_date = getDateFormat(vm.form.startDate, vm.form.startDateTime);
            vm.form.end_date = getDateFormat(vm.form.startDate, vm.form.endDateTime);

            vm.safeApply();
        }
    },

    editAbsenceForm(obj): void {
        vm.createAbsenceLightBox = true;
        vm.safeApply();
    },

    isFormValid(): boolean {
        if (vm.form) {
            return vm.form.startDateTime < vm.form.endDateTime;
        }
        return false;
    },

    async createAbsence(): Promise<void> {
        vm.form.start_date = getDateFormat(vm.form.startDate, vm.form.startDateTime);
        vm.form.end_date = getDateFormat(vm.form.startDate, vm.form.endDateTime);
        let response = await vm.form.createAbsence(window.structure.id, vm.form.reason_id, model.me.userId);
        if (response.status == 200 || response.status == 201) {
            vm.closeAbsenceLightbox();
            toasts.confirm(lang.translate('presences.absence.form.create.succeed'));
        } else {
            toasts.warning(response.data.toString());
        }
        absenceForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.FILTER);
        vm.safeApply();
    },

    closeAbsenceLightbox(): void {
        vm.createAbsenceLightBox = false;
    }
};

export const absenceForm = {
    title: 'presences.register.event_type.absences',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            absenceForm.that = this;
            vm.reasonsType = await eventService.getReasonsType(window.structure.id);
            vm.safeApply = this.safeApply;
        },
        setHandler: function () {
            this.$on(SNIPLET_FORM_EVENTS.SET_PARAMS, (event, arg) => vm.setFormParams(arg));
        }
    }
};