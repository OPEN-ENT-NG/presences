import {_, angular, moment, ng, notify} from 'entcore';
import {Incident, Incidents, ISchoolYearPeriod, Student, Students} from "../models";
import {IncidentService} from "../services";
import {Toast} from "@common/utils/toast";
import {Scope} from "./main";
import {INCIDENTS_FORM_EVENTS} from '../sniplets';
import {DateUtils} from "@common/utils";
import {IViescolaireService, ViescolaireService} from "@common/services/ViescolaireService";

declare let window: any;

interface ViewModel {
    incidents: Incidents;
    notifications: any[];

    // Collapse
    incidentId: number;
    collapse: boolean;

    toggleCollapse(incident: Incident, event): void;

    isCollapsibleOpen(id): boolean;

    isProcessed(incident: Incident): void;

    // CSV
    exportCsv(): void;

    // Filter
    filter: { startDate: Date, endDate: Date, students: Student[], student: { search: string } };

    updateDate(): void;

    updateFilter(student?: Student): void;

    sortField(field: string);

    // Students
    students: Students;

    searchStudent(string): void;

    selectStudents(model, option: Student): void;

    removeStudents(audience): void;

    editIncidentLightbox(incident: Incident): void;
}

export const incidentsController = ng.controller('IncidentsController',
    ['$scope', '$location', 'IncidentService', 'ViescolaireService',
        function ($scope: Scope, $location, IncidentService: IncidentService, viescolaireService: IViescolaireService) {
            const vm: ViewModel = this;
            vm.notifications = [];
            vm.filter = {
                startDate: null,
                endDate: new Date(),
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
                if (!window.structure) return;

                vm.incidents.structureId = window.structure.id;

                if (vm.filter.startDate === null) {
                    const schoolYears: ISchoolYearPeriod = await viescolaireService.getSchoolYearDates(window.structure.id);
                    vm.filter.startDate = moment(schoolYears.start_date);
                }

                vm.incidents.startDate = moment(vm.filter.startDate).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                vm.incidents.endDate = moment(vm.filter.endDate).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);

                setDataFromMemento();
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
            vm.updateFilter = (student?: Student): void => {
                if (student && !_.find(vm.filter.students, student)) {
                    vm.filter.students.push(student);
                }
                setStudentToSync();
                vm.incidents.page = 0;
                $scope.safeApply();
            };

            const setDataFromMemento = (): void => {
                const fetchedParam: { mementoStudentId: string, mementoStudentName: string } = $location.search();
                // if there is nothing to fetch since this is only from memento
                if (!fetchedParam.mementoStudentId && !fetchedParam.mementoStudentName) {
                    return;
                }
                let student: any = {
                    id: fetchedParam.mementoStudentId,
                    displayName: fetchedParam.mementoStudentName,
                };
                vm.filter.students.push(student);
                $location.search({});
            };

            vm.sortField = async (field: string) => {
                switch (field) {
                    case 'date':
                        vm.incidents.order = field;
                        break;
                    case 'time':
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

            vm.selectStudents = (model: Student, option: Student): void => {
                vm.updateFilter(option);
                vm.filter.student.search = '';
            };

            vm.removeStudents = (student: Student): void => {
                vm.filter.students = _.without(vm.filter.students, _.findWhere(vm.filter.students, student));
                vm.updateFilter();
            };

            vm.updateDate = (): void => {
                getIncidents();
                $scope.safeApply();
            };

            $scope.$on('form:filter', getIncidents);


            /* Collapse  */
            vm.toggleCollapse = (incident: Incident, event): void => {
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

            vm.isProcessed = async (incident: Incident): Promise<void> => {
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
        $scope.safeApply();
    };

    /* on switch (watch) */
    $scope.$watch(() => window.structure, () => {
        getIncidents();
    });

    /* Destroy directive and scope */
    $scope.$on('$destroy', function () {
        angular.element(document.querySelectorAll(".datepicker")).remove();
    })
}]);