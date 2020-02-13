import {IAngularEvent} from "angular";
import {disciplineService, presenceService, SearchService} from "../services";
import {Discipline, MarkedStudent, MarkedStudentRequest, Presence, PresenceBody, User} from "../models";
import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from "@common/model";
import {_, idiom as lang, model, moment, toasts} from "entcore";
import {DateUtils} from "@common/utils";
import {StudentsSearch} from "../utilities";

console.log("presenceFormSnipplets");

declare let window: any;

interface ViewModel {
    createPresenceLightBox: boolean;
    presence: Presence;
    form: PresenceBody;
    studentsSearch: StudentsSearch;
    date: { date: string, startTime: Date, endTime: Date };
    disciplines: Array<Discipline>;
    isButtonAllowed: boolean;

    openPresenceLightbox(): void;

    initPresence(): void;

    initPresenceEdit(presence: Presence): void;

    preparePresenceForm(): void;

    isFormValid(): void;

    openEditPresence(presence: Presence): void;

    createPresence(): Promise<void>;

    updatePresence(): Promise<void>;

    deletePresence(): Promise<void>;

    closePresenceLightbox(): void;

    searchStudent(studentForm: string): Promise<void>;

    selectStudent(valueInput, studentItem): void;

    removeMarkedStudent(markedStudent: MarkedStudent): void;

    safeApply(fn?: () => void): void;
}

const vm: ViewModel = {
    safeApply: null,
    createPresenceLightBox: false,
    disciplines: [],
    date: {
        date: moment(),
        startTime: moment().set({second: 0, millisecond: 0}).toDate(),
        endTime: moment().add(1, 'h').set({second: 0, millisecond: 0}).toDate(),
    },
    presence: {owner: {} as User, discipline: {} as Discipline, markedStudents: []} as Presence,
    form: {} as PresenceBody,
    studentsSearch: null,
    isButtonAllowed: true,

    openPresenceLightbox(): void {
        vm.studentsSearch = new StudentsSearch(window.structure.id, SearchService);
        vm.createPresenceLightBox = true;
        vm.initPresence();
        vm.safeApply();
    },

    async createPresence(): Promise<void> {
        vm.preparePresenceForm();
        let response = await presenceService.create(vm.form);
        if (response.status == 200 || response.status == 201) {
            vm.closePresenceLightbox();
            toasts.confirm(lang.translate('presences.presence.form.create.succeed'));
        } else {
            toasts.warning(response.data.toString());
        }
        presenceForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.FILTER);
        vm.safeApply();
    },

    async updatePresence(): Promise<void> {
        vm.preparePresenceForm();
        let response = await presenceService.update(vm.form);
        if (response.status == 200 || response.status == 201) {
            vm.closePresenceLightbox();
            toasts.confirm(lang.translate('presences.presence.form.edit.succeed'));
        } else {
            toasts.warning(response.data.toString());
        }
        presenceForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.EDIT);
        vm.safeApply();
    },

    async deletePresence(): Promise<void> {
        vm.preparePresenceForm();
        let response = await presenceService.delete(vm.form.id);
        if (response.status == 200 || response.status == 201) {
            vm.closePresenceLightbox();
            toasts.confirm(lang.translate('presences.presence.form.delete.succeed'));
        } else {
            toasts.warning(response.data.toString());
        }
        presenceForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.DELETE);
        vm.safeApply();
    },


    openEditPresence: (presence: Presence): void => {
        vm.studentsSearch = new StudentsSearch(presence.structureId, SearchService);
        vm.createPresenceLightBox = true;
        vm.initPresenceEdit(presence);
    },

    initPresenceEdit: (presence: Presence): void => {
        /* when click on card to edit presence */
        vm.presence.id = presence.id;
        vm.date.date = moment(presence.startDate);
        vm.date.startTime = moment(presence.startDate).set({second: 0, millisecond: 0}).toDate();
        vm.date.endTime = moment(presence.endDate).set({second: 0, millisecond: 0}).toDate();
        vm.presence.owner.displayName = presence.owner.displayName;
        vm.presence.owner.id = presence.owner.id;
        vm.presence.structureId = presence.structureId;
        vm.presence.discipline = presence.discipline;
        vm.presence.markedStudents = presence.markedStudents;
    },

    initPresence: (): void => {
        /* when click on button create presence */
        vm.presence.id = null;
        vm.date.date = moment();
        vm.date.startTime = moment().set({second: 0, millisecond: 0}).toDate();
        vm.date.endTime = moment().add(1, 'h').set({second: 0, millisecond: 0}).toDate();
        vm.presence.owner.displayName = model.me.username;
        vm.presence.owner.id = model.me.userId;
        vm.presence.structureId = window.structure.id;
        vm.presence.discipline = {} as Discipline;
        vm.presence.markedStudents = [];
    },

    preparePresenceForm: (): void => {
        vm.form.id = vm.presence.id;
        vm.form.startDate = DateUtils.getDateFormat(moment(vm.date.date), vm.date.startTime);
        vm.form.endDate = DateUtils.getDateFormat(moment(vm.date.date), vm.date.endTime);
        vm.form.structureId = vm.presence.structureId;
        vm.form.disciplineId = vm.presence.discipline.id;
        vm.form.markedStudents = [];
        vm.presence.markedStudents.forEach((markedStudent: MarkedStudent) => {
            let markedStudentBody: MarkedStudentRequest = {} as MarkedStudentRequest;
            markedStudentBody.comment = markedStudent.comment;
            markedStudentBody.studentId = markedStudent.student.id;
            vm.form.markedStudents.push(markedStudentBody);
        });
    },

    isFormValid(): boolean {
        return vm.presence.discipline.id != null && vm.presence.markedStudents.length > 0 &&
            DateUtils.getDateFormat(moment(vm.date.date), vm.date.startTime) <=
            DateUtils.getDateFormat(moment(vm.date.date), vm.date.endTime);
    },


    /* search bar interaction */

    async searchStudent(studentForm: string): Promise<void> {
        await vm.studentsSearch.searchStudentsOrGroups(studentForm);
        vm.safeApply();
    },

    selectStudent(valueInput, studentItem): void {
        console.log("valueInput: ", valueInput);
        console.log("studentItem: ", studentItem);
    },

    removeMarkedStudent(markedStudent: MarkedStudent): void {
        vm.presence.markedStudents = _.without(vm.presence.markedStudents, markedStudent);
        vm.safeApply();
    },

    closePresenceLightbox(): void {
        vm.createPresenceLightBox = false;
    },
};

export const presenceForm = {
    title: 'presences.presences.form',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            presenceForm.that = this;
            vm.disciplines = await disciplineService.get(window.structure.id);
            this.setButton();
            vm.safeApply = this.safeApply;
        },
        setHandler: function () {
            this.$on(SNIPLET_FORM_EVENTS.SET_PARAMS, (event: IAngularEvent, presence: Presence) => vm.openEditPresence(presence));
        },
        setButton: function () {
            switch (window.location.hash) {
                case '#/dashboard': {
                    vm.isButtonAllowed = false;
                    break;
                }
            }
        }
    }
};