import {Incident, ProtagonistForm, Student, Students} from "../models";
import {IncidentParameterType} from "../services";
import {incidentService} from "../services/IncidentService";
import {_, idiom as lang, moment, notify, toasts} from "entcore";
import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from '@common/model'

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

    safeApply(fn?: () => void): void;

    createIncident(): void;

    saveIncident(): void;

    editIncidentLightbox(incident: Incident): void;

    deleteIncident(): void;

    createIncidentLightbox(): void;

    closeIncidentLightbox(): void;

    selectIncidentStudentForm(model: Student, student: Student): void;

    removeIncidentStudentForm(): void;

    searchIncidentStudentForm(string): void;

    getButtonLabel(): string;
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
    createIncidentLightbox: async function () {
        vm.isLightboxActive = true;
        vm.lightbox.createMode = true;
        if (!vm.incidentParameterType) {
            vm.incidentParameterType = await incidentService.getIncidentParameterType(window.structure.id);
        }
        vm.incidentForm = new Incident(window.structure.id);
        vm.incidentStudentsForm = new Students();

        /* Setting incident parameter type select */
        vm.incidentForm.partner = vm.incidentParameterType.partner[vm.incidentParameterType.partner.length - 1];
        incidentForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.CREATION);
        vm.safeApply()
    },
    createIncident: async function () {
        try {
            let response = await vm.incidentForm.create();
            if (response.status == 200 || response.status == 201) {
                vm.closeIncidentLightbox();
                // vm.incidents.page = 0;
                toasts.confirm('incidents.form.succeed');
                incidentForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.FILTER);
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
            let response = await vm.incidentForm.update();
            if (response.status == 200 || response.status == 201) {
                vm.closeIncidentLightbox();

                /* assign new value to the concerned array (find by id) */
                vm.incidentForm.date.setHours(vm.incidentForm.dateTime.getHours(), vm.incidentForm.dateTime.getMinutes());
                // vm.incidents.all[vm.incidents.all
                //     .findIndex(incident => incident.id === vm.incidentForm.id)] = vm.incidentForm;

                incidentForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.FILTER);
                toasts.confirm('incidents.edit.form.succeed');
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
                vm.closeIncidentLightbox();
                toasts.confirm('incidents.delete.succeed');
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
    removeIncidentStudentForm: function () {
        vm.incidentForm.protagonists = _.without(vm.incidentForm.protagonists, _.findWhere(vm.incidentForm.protagonists));
        vm.safeApply();
    },
    searchIncidentStudentForm: async function (searchText: string) {
        await vm.incidentStudentsForm.search(window.structure.id, searchText);
        vm.safeApply();
    },
    editIncidentLightbox: async function (incident: Incident) {
        vm.isLightboxActive = true;
        vm.lightbox.editMode = true;
        if (!vm.incidentParameterType) {
            vm.incidentParameterType = await incidentService.getIncidentParameterType(window.structure.id);
        }

        vm.incidentStudentsForm = new Students();
        vm.incidentForm = _.clone(incident);
        vm.incidentForm.date = new Date(incident.date);
        vm.incidentForm.dateTime = new Date(incident.date);
        vm.incidentForm.protagonists = JSON.parse(JSON.stringify(incident.protagonists));
        vm.safeApply()
    },
    getButtonLabel: () => lang.translate(`incidents${vm.isCalendar ? '.calendar' : ''}.create`)
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