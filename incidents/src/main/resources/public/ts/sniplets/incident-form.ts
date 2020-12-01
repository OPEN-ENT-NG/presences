import {Incident, ProtagonistForm, Student, Students} from "../models";
import {IncidentParameterType, incidentService, Partner, SearchService} from "../services";
import {_, idiom as lang, model, moment, notify, toasts} from "entcore";
import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from '@common/model'
import {User} from "@common/model/User";
import {UsersSearch} from "@common/utils";

declare let window: any;

console.log('Sniplet Incident form');

export enum INCIDENTS_FORM_EVENTS {
    EDIT = 'incident-form:edit'
}

interface ViewModel {
    isCalendar: boolean;
    incidentForm: Incident;
    lightbox: { createMode: boolean, editMode: boolean };
    incidentStudentsForm: Students;
    incidentParameterType: IncidentParameterType;
    isLightboxActive: boolean;
    emptyParameter: Partner;
    usersSearch: UsersSearch;
    ownerSearch: string;

    safeApply(fn?: () => void): void;

    createIncident(): void;

    saveIncident(): void;

    editIncidentLightbox(incident: Incident): void;

    deleteIncident(): void;

    createIncidentLightbox(): void;

    checkOptionState(): void;

    removeEmptyOption(): void;

    changeSelect(formType: any, selectName: string): void;

    closeIncidentLightbox(): void;

    selectIncidentStudentForm(model: Student, student: Student): void;

    removeIncidentStudentForm(student): void;

    searchIncidentStudentForm(string): void;

    getButtonLabel(): string;

    searchOwner(value: string): Promise<void>;

    selectOwner(model, owner: User): void;

    getDisplayOwnerName(): string;
}

const vm: ViewModel = {
    isCalendar: false,
    safeApply: null,
    lightbox: {
        createMode: false,
        editMode: false
    },
    incidentStudentsForm: null,
    incidentForm: null,
    incidentParameterType: null,
    isLightboxActive: false,
    emptyParameter: null,
    usersSearch: null,
    ownerSearch: null,

    createIncidentLightbox: async function () {
        vm.usersSearch = new UsersSearch(window.structure.id, SearchService);
        vm.isLightboxActive = true;
        vm.lightbox.createMode = true;
        if (!vm.incidentParameterType) {
            vm.incidentParameterType = await incidentService.getIncidentParameterType(window.structure.id);
            vm.emptyParameter = vm.incidentParameterType.partner.find(item => item.structure_id === '');
            vm.incidentParameterType.partner.pop();
        }
        // check if add empty state
        vm.checkOptionState();
        vm.incidentForm = new Incident(window.structure.id);
        vm.incidentStudentsForm = new Students();
        vm.incidentForm.owner = model.me;

        incidentForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.CREATION);
        vm.safeApply();
    },

    checkOptionState(): void {
        let selectPlace = document.getElementById('selectPlace');
        if (selectPlace['options'][0].value !== '') {
            let option = document.createElement('option');
            option.value = '';
            selectPlace.insertBefore(option, selectPlace.firstChild)
        }

        let incidentType = document.getElementById('selectIncidentType');
        if (incidentType['options'][0].value !== '') {
            let option = document.createElement('option');
            option.value = '';
            incidentType.insertBefore(option, incidentType.firstChild)
        }

        let selectSeriousness = document.getElementById('selectSeriousness');
        if (selectSeriousness['options'][0].value !== '') {
            let option = document.createElement('option');
            option.value = '';
            selectSeriousness.insertBefore(option, selectSeriousness.firstChild)
        }
    },

    removeEmptyOption(): void {
        let selectPlace = document.getElementById('selectPlace');
        if (selectPlace['options'][0].value === '') {
            selectPlace.removeChild(selectPlace['options'][0]);
        }
        let selectIncidentType = document.getElementById('selectIncidentType');
        if (selectIncidentType['options'][0].value === '') {
            selectIncidentType.removeChild(selectIncidentType['options'][0]);
        }
        let selectSeriousness = document.getElementById('selectSeriousness');
        if (selectSeriousness['options'][0].value === '') {
            selectSeriousness.removeChild(selectSeriousness['options'][0]);
        }
    },

    changeSelect(formType: any, selectName: string): void {
        if (vm.lightbox.createMode) {
            if (formType && formType.id) {
                if (selectName !== 'selectPartner') {
                    let select = document.getElementById(selectName);
                    if (select['options'][0].value === '') {
                        select.removeChild(select['options'][0]);
                    }
                }
            }
        }
    },

    createIncident: async function () {
        try {
            if (!vm.incidentForm.partner.hasOwnProperty('id')) {
                vm.incidentForm.partner = vm.emptyParameter;
            }
            let response = await vm.incidentForm.create();
            if (response.status == 200 || response.status == 201) {
                toasts.confirm('incidents.form.succeed');
                incidentForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.FILTER);
                vm.closeIncidentLightbox();
            } else {
                toasts.warning('incidents.form.err');
            }
            vm.safeApply();
        } catch (err) {
            notify.error('incidents.form.err');
            vm.safeApply();
            throw err;
        }
    },

    saveIncident: async function () {
        try {
            if (!vm.incidentForm.partner.hasOwnProperty('id')) {
                vm.incidentForm.partner = vm.emptyParameter;
            }
            let response = await vm.incidentForm.update();
            if (response.status == 200 || response.status == 201) {

                /* assign new value to the concerned array (find by id) */
                vm.incidentForm.date.setHours(vm.incidentForm.dateTime.getHours(), vm.incidentForm.dateTime.getMinutes());

                incidentForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.FILTER);
                toasts.confirm('incidents.edit.form.succeed');
                vm.closeIncidentLightbox();
            } else {
                toasts.warning('incidents.edit.form.err');
            }
            vm.safeApply();
        } catch (err) {
            toasts.warning('incidents.edit.form.err');
            vm.safeApply();
            throw err;
        }
    },

    deleteIncident: async function () {
        try {
            let response = await vm.incidentForm.delete();
            if (response.status == 200 || response.status == 201) {
                incidentForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.FILTER);
                toasts.confirm('incidents.delete.succeed');
                vm.closeIncidentLightbox();
            } else {
                toasts.warning('incidents.delete.err');
                // vm.notifications.push(new Toast('incidents.delete.err', 'warning'));
            }
        } catch (err) {
            // notify.error('incidents.delete.err');
            toasts.warning('incidents.delete.err');
            vm.safeApply();
            throw err;
        }
    },

    closeIncidentLightbox: function () {
        vm.isLightboxActive = false;
        vm.lightbox.createMode = false;
        vm.lightbox.editMode = false;
        delete vm.incidentForm;
    },

    selectIncidentStudentForm: function (model, student: Student) {
        if (student && !_.find(vm.incidentForm.protagonists, student)) {
            let protagonist = {} as ProtagonistForm;
            protagonist.label = student.displayName;
            protagonist.userId = student.id;
            protagonist.protagonistType = vm.incidentParameterType.protagonistType[0];
            vm.incidentForm.protagonists.push(protagonist);
            vm.incidentStudentsForm.searchValue = null;
        }
    },

    removeIncidentStudentForm: function (student) {
        vm.incidentForm.protagonists = _.without(vm.incidentForm.protagonists, student);
        vm.safeApply();
    },

    searchIncidentStudentForm: async function (searchText: string) {
        await vm.incidentStudentsForm.search(window.structure.id, searchText);
        vm.safeApply();
    },

    editIncidentLightbox: async function (incident: Incident) {
        vm.usersSearch = new UsersSearch(window.structure.id, SearchService);
        vm.isLightboxActive = true;
        vm.lightbox.editMode = true;
        if (!vm.incidentParameterType) {
            vm.incidentParameterType = await incidentService.getIncidentParameterType(window.structure.id);
            vm.emptyParameter = vm.incidentParameterType.partner.find(item => item.structure_id === '');
            vm.incidentParameterType.partner.pop();
            vm.safeApply();
        }
        // remove empty state select
        vm.removeEmptyOption();

        vm.incidentStudentsForm = new Students();
        vm.incidentForm = _.clone(incident);
        vm.incidentForm.date = new Date(incident.date);
        vm.incidentForm.dateTime = new Date(incident.date);
        vm.incidentForm.protagonists = JSON.parse(JSON.stringify(incident.protagonists));
        vm.incidentForm.owner = incident.owner;
        vm.safeApply()
    },
    getButtonLabel: () => lang.translate(`incidents${vm.isCalendar ? '.calendar' : ''}.create`),

    getDisplayOwnerName: (): string => {
        return vm.incidentForm.owner.displayName || vm.incidentForm.owner.lastName + " " + vm.incidentForm.owner.firstName;
    },

    searchOwner: async (value: string): Promise<void> => {
        await vm.usersSearch.searchUsers(value);
        vm.safeApply();
    },

    selectOwner: (model, owner: User): void => {
        vm.incidentForm.owner = owner;
        vm.ownerSearch = '';
        vm.safeApply();
    },
};

export const incidentForm = {
    title: 'incidents.incident.form.sniplet.title',
    public: false,
    that: null,
    controller: {
        init: function () {
            this.vm = vm;
            this.vm.isCalendar = new RegExp('\#\/calendar').test(window.location.hash);
            vm.safeApply = this.safeApply;
            incidentForm.that = this;
            this.setHandler();
        },
        setHandler: function () {
            this.$on(INCIDENTS_FORM_EVENTS.EDIT, (event, incident) => vm.editIncidentLightbox(incident));
            this.$on(SNIPLET_FORM_EVENTS.SET_PARAMS, (event, {student, start_date}) => {
                if (vm.incidentForm) {
                    vm.selectIncidentStudentForm(null, student);
                    vm.incidentForm.date = moment(start_date).toDate();
                }
            });
        }
    }
};