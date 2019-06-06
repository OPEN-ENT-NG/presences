import {_, ng, notify} from 'entcore';
import {Incident, Incidents, ProtagonistForm, Student, Students} from "../models";
import {DateUtils} from '@common/utils'
import {IncidentParameterType, IncidentService} from "../services";
import {Toast} from "@common/utils/toast";
import {Scope} from "./main";

declare let window: any;

interface ViewModel {
    incidents: Incidents;
    notifications: any[];

    // Collapse
    incidentId: number;
    collapse: boolean;
    toggleCollapse(incident, event): void;

    isCollapsibleOpen(id): boolean;

    isProcessed(incident): void;

    // CSV
    exportCsv(): void;

    // Filter
    filter: { startDate: Date, endDate: Date, students: Student[] };
    updateDate(): void;
    updateFilter(student?): void;

    // Students
    students: Students;
    studentSearchInput: string;

    searchStudent(string): void;

    selectStudents(model, option: Student): void;
    removeStudents(audience): void;

    // incident light box
    lightbox: { createMode: boolean, editMode: boolean };
    incidentForm: Incident;
    incidentStudentsForm: Students;
    incidentParameterType: IncidentParameterType;
    isLightboxActive: boolean;

    createIncidentLightbox(): void;

    editIncidentLightbox(incident: Incident): void;
    createIncident(): void;

    saveIncident(): void;

    deleteIncident(): void;
    searchIncidentStudentForm(string): void;

    selectIncidentStudentForm(model: Student, student: Student): void;
    removeIncidentStudentForm(): void;
    closeIncidentLightbox(): void;
}

export const incidentsController = ng.controller('IncidentsController', ['$scope', 'IncidentService', function ($scope: Scope, IncidentService: IncidentService) {
    const vm: ViewModel = this;
    vm.notifications = [];
    vm.studentSearchInput = "";
    vm.filter = {
        startDate: DateUtils.add(new Date(), -30, "d"),
        endDate: DateUtils.add(new Date(), +30, "d"),
        students: []
    };
    vm.students = new Students();
    vm.collapse = false;
    vm.incidents = new Incidents();
    vm.incidentId = null;

    vm.lightbox = {
        createMode: false,
        editMode: false
    };

    vm.incidents.eventer.on('loading::true', () => $scope.safeApply());
    vm.incidents.eventer.on('loading::false', () => $scope.safeApply());


    const setStudentToSync = () => {
        vm.incidents.userId = vm.filter.students ? vm.filter.students
            .map(students => students.id)
            .filter(function () {
                return true
            })
            .toString() : '';
    };

    const getIncidents = async (): Promise<void> => {
        vm.incidents.structureId = window.structure.id;
        vm.incidents.startDate = vm.filter.startDate.toDateString();
        vm.incidents.endDate = vm.filter.endDate.toDateString();

        setStudentToSync();

        // "page" uses sync() method at the same time it sets 0 (See LoadingCollection)
        vm.incidents.page = 0;
        $scope.safeApply();
    };
    getIncidents();

    /* CSV  */
    vm.exportCsv = (): void => {
        if (vm.incidents.pageCount * 20 !== 0) {
            if (vm.incidents.pageCount * 20 > 1000) {
                vm.notifications.push(new Toast('incidents.csv.full', 'warning'));
            } else {
                vm.incidents.export();
            }
        } else {
            vm.notifications.push(new Toast('incidents.csv.empty', 'info'));
        }
    };

    /* Filter  */
    vm.updateFilter = (student?): void => {
        if (student && !_.find(vm.filter.students, student)) {
            vm.filter.students.push(student);
        }
        setStudentToSync();
        vm.incidents.page = 0;
        $scope.safeApply();
    };

    vm.searchStudent = async (searchText: string) => {
        await vm.students.search(window.structure.id, searchText);
        $scope.safeApply();
    };

    vm.selectStudents = async (model: Student, option: Student) => {
        vm.updateFilter(option);
    };

    vm.removeStudents = (student): void => {
        vm.filter.students = _.without(vm.filter.students, _.findWhere(vm.filter.students, student));
        vm.updateFilter();
    };

    vm.updateDate = async () => {
        getIncidents();
        $scope.safeApply();
    };


    /* Collapse  */
    vm.toggleCollapse = (incident, event): void => {
        if (vm.incidentId == event.currentTarget.getAttribute("data-id")) {
            vm.collapse = !vm.collapse;
            if (vm.collapse) {
                vm.incidentId = event.currentTarget.getAttribute("data-id");
            } else {
                vm.incidentId = null;
            }
        } else {
            vm.collapse = true;
            vm.incidentId = event.currentTarget.getAttribute("data-id");
        }
        $scope.safeApply();
    };

    vm.isCollapsibleOpen = (id): boolean => {
        return id == vm.incidentId;
    };

    vm.isProcessed = async function (incident) {
        try {
            let response = await incident.update();
            if (response.status == 200 || response.status == 201) {
                if (incident.processed) {
                    vm.notifications.push(new Toast('incident.processed.done', 'confirm'));
                } else {
                    vm.notifications.push(new Toast('incident.processed.undone', 'confirm'));
                }
            } else {
                vm.notifications.push(new Toast('incidents.edit.form.err', 'warning'));
            }
            $scope.safeApply();
        } catch (err) {
            notify.error('incidents.edit.form.err');
            $scope.safeApply();
            throw err;
        }
    };

    /* Incident lightbox */
    vm.createIncidentLightbox = async function () {
        vm.isLightboxActive = true;
        vm.lightbox.createMode = true;
        if (!vm.incidentParameterType) {
            vm.incidentParameterType = await IncidentService.getIncidentParameterType(window.structure.id);
        }
        vm.incidentForm = new Incident(window.structure.id);
        vm.incidentStudentsForm = new Students();

        /* Setting incident parameter type select */
        vm.incidentForm.partner = vm.incidentParameterType.partner[vm.incidentParameterType.partner.length - 1];

        $scope.safeApply()
    };

    vm.createIncident = async function () {
        try {
            let response = await vm.incidentForm.create();
            if (response.status == 200 || response.status == 201) {
                vm.closeIncidentLightbox();
                vm.incidents.page = 0;
                vm.notifications.push(new Toast('incidents.form.succeed', 'confirm'));
            } else {
                vm.notifications.push(new Toast('incidents.form.err', 'warning'));
            }
            $scope.safeApply();
        } catch (err) {
            notify.error('incidents.form.err');
            $scope.safeApply();
            throw err;
        }
    };

    vm.editIncidentLightbox = async function (incident: Incident) {
        vm.isLightboxActive = true;
        vm.lightbox.editMode = true;
        if (!vm.incidentParameterType) {
            vm.incidentParameterType = await IncidentService.getIncidentParameterType(window.structure.id);
        }

        vm.incidentStudentsForm = new Students();
        vm.incidentForm = _.clone(incident);
        vm.incidentForm.date = new Date(incident.date);
        vm.incidentForm.protagonists = JSON.parse(JSON.stringify(incident.protagonists));
        $scope.safeApply()
    };

    vm.saveIncident = async function () {
        try {
            let response = await vm.incidentForm.update();
            if (response.status == 200 || response.status == 201) {
                vm.closeIncidentLightbox();

                /* assign new value to the concerned array (find by id) */
                vm.incidents.all[vm.incidents.all
                    .findIndex(incident => incident.id === vm.incidentForm.id)] = vm.incidentForm;

                vm.notifications.push(new Toast('incidents.edit.form.succeed', 'confirm'));
            } else {
                vm.notifications.push(new Toast('incidents.edit.form.err', 'warning'));
            }
            $scope.safeApply();
        } catch (err) {
            notify.error('incidents.edit.form.err');
            $scope.safeApply();
            throw err;
        }
    };

    vm.deleteIncident = async function () {
        try {
            let response = await vm.incidentForm.delete();
            if (response.status == 200 || response.status == 201) {
                vm.closeIncidentLightbox();
                vm.incidents.page = 0;
                vm.notifications.push(new Toast('incidents.delete.succeed', 'confirm'));
            } else {
                vm.notifications.push(new Toast('incidents.delete.err', 'warning'));
            }
        } catch (err) {
            notify.error('incidents.delete.err');
            $scope.safeApply();
            throw err;
        }
    };

    vm.searchIncidentStudentForm = async (searchText: string) => {
        await vm.incidentStudentsForm.search(window.structure.id, searchText);
        $scope.safeApply();
    };

    vm.selectIncidentStudentForm = (model, student: Student): void => {
        if (student && !_.find(vm.incidentForm.protagonists, student)) {
            let protagonist = {} as ProtagonistForm;
            protagonist.label = student.displayName;
            protagonist.userId = student.id;
            protagonist.protagonistType = vm.incidentParameterType.protagonistType[0];
            vm.incidentForm.protagonists.push(protagonist);
        }
    };

    vm.removeIncidentStudentForm = () => {
        vm.incidentForm.protagonists = _.without(vm.incidentForm.protagonists, _.findWhere(vm.incidentForm.protagonists));
        $scope.safeApply();
    };

    vm.closeIncidentLightbox = () => {
        vm.isLightboxActive = false;
        vm.lightbox.createMode = false;
        vm.lightbox.editMode = false;
    };
}]);