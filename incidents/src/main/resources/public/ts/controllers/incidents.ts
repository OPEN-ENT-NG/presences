import {_, ng, notify} from 'entcore';
import {Incident, Incidents, Student, Students} from "../models";
import {DateUtils} from '@common/utils'
import {IncidentService} from "../services";
import {Toast} from "@common/utils/toast";
import {Scope} from "./main";
import {INCIDENTS_FORM_EVENTS} from '../sniplets';

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
    filter: { startDate: Date, endDate: Date, students: Student[], student: { search: string } };

    updateDate(): void;

    updateFilter(student?): void;

    sortField(field: string);

    // Students
    students: Students;

    searchStudent(string): void;

    selectStudents(model, option: Student): void;

    removeStudents(audience): void;

    editIncidentLightbox(incident: Incident): void;
}

export const incidentsController = ng.controller('IncidentsController', ['$scope', 'IncidentService', function ($scope: Scope, IncidentService: IncidentService) {
        const vm: ViewModel = this;
        vm.notifications = [];
        vm.filter = {
            startDate: DateUtils.add(new Date(), -30, "d"),
            endDate: DateUtils.add(new Date(), +30, "d"),
            students: [],
            student: {
                search: ''
            }
        };
        vm.students = new Students();
        vm.collapse = false;
        vm.incidents = new Incidents();
        vm.incidentId = null;
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
            if (vm.incidents.pageCount > 50) {
                vm.notifications.push(new Toast('incidents.csv.full', 'warning'));
            } else {
                if (vm.incidents.all.length === 0) {
                    vm.notifications.push(new Toast('incidents.csv.empty', 'info'));
                } else {
                    vm.incidents.export();
                }
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

        vm.sortField = async (field) => {
            switch (field) {
                case 'date':
                    vm.incidents.order = field;
                    break;
                case 'place':
                    vm.incidents.order = field;
                    break;
                case 'type':
                    vm.incidents.order = field;
                    break;
                case 'seriousness':
                    vm.incidents.order = field;
                    break;
                case 'treated':
                    vm.incidents.order = field;
                    break;
                default:
                    vm.incidents.order = 'date';
            }
            vm.incidents.reverse = !vm.incidents.reverse;
            await vm.incidents.syncPagination();
            $scope.safeApply();
        };

        vm.searchStudent = async (searchText: string) => {
            await vm.students.search(window.structure.id, searchText);
            $scope.safeApply();
        };

        vm.selectStudents = async (model: Student, option: Student) => {
            vm.updateFilter(option);
            vm.filter.student.search = '';
        };

        vm.removeStudents = (student): void => {
            vm.filter.students = _.without(vm.filter.students, _.findWhere(vm.filter.students, student));
            vm.updateFilter();
        };

        vm.updateDate = async () => {
            getIncidents();
            $scope.safeApply();
        };

        $scope.$on('form:filter', getIncidents);


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

        vm.editIncidentLightbox = async function (incident: Incident) {
            $scope.$broadcast(INCIDENTS_FORM_EVENTS.EDIT, incident);
            $scope.safeApply()
        };
    }])
;