import {_, angular, moment, ng, notify} from 'entcore';
import {Incident, Incidents, ISchoolYearPeriod, Student, Students} from "../models";
import {Group, GroupService, groupService, IncidentService, SearchService} from "../services";
import {Toast} from "@common/utils/toast";
import {Scope} from "./main";
import {INCIDENTS_FORM_EVENTS} from '../sniplets';
import {DateUtils, GroupsSearch, StudentsSearch} from "@common/utils";
import {IViescolaireService, ViescolaireService} from "@common/services/ViescolaireService";

declare let window: any;

interface ViewModel {
    incidents: Incidents;
    notifications: any[];
    studentsSearch: StudentsSearch;
    groupsSearch: GroupsSearch;

    // Collapse
    incidentId: number;
    collapse: boolean;

    toggleCollapse(incident: Incident, event): void;

    isCollapsibleOpen(id): boolean;

    isProcessed(incident: Incident): void;

    // CSV
    exportCsv(): void;

    // Filter
    filter: {
        startDate: Date,
        endDate: Date,
        students: Array<Student>,
        groups: Array<Group>,
        student: { search: string } };

    updateDate(): void;

    updateFilter(student?: Student): void;

    sortField(field: string);

    // Students
    students: Students;

    searchStudent(searchText: string): Promise<void>;

    selectStudents(model: string, option: Student): void;

    removeStudents(student: Student): void;

    searchGroup(searchText: string): Promise<void>;

    selectGroup(valueInput: string, groupItem: Group): void;

    removeSelectedGroups(groupItem: Group): void;

    editIncidentLightbox(incident: Incident): void;
}

export const incidentsController = ng.controller('IncidentsController',
    ['$scope', '$location', 'SearchService', 'IncidentService', 'ViescolaireService', 'GroupService',
        function ($scope: Scope, $location, searchService: SearchService, IncidentService: IncidentService,
                  viescolaireService: IViescolaireService, groupService: GroupService) {
            const vm: ViewModel = this;
            vm.notifications = [];
            vm.filter = {
                startDate: null,
                endDate: new Date(),
                students: [],
                groups: [],
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

            const setStudentAndGroupsToSync = () => {
                vm.incidents.userId = vm.filter.students ? vm.filter.students
                    .map((students: Student) => students.id)
                    .toString() : '';

                vm.incidents.audienceIds = vm.filter.groups ? vm.filter.groups.toString() : '';
            };

            const getIncidents = async (): Promise<void> => {
                if (!window.structure) return;

                vm.incidents.structureId = window.structure.id;
                vm.studentsSearch = new StudentsSearch(window.structure.id, searchService);
                vm.groupsSearch = new GroupsSearch(window.structure.id, searchService, groupService);

                if (vm.filter.startDate === null) {
                    const schoolYears: ISchoolYearPeriod = await viescolaireService.getSchoolYearDates(window.structure.id);
                    vm.filter.startDate = moment(schoolYears.start_date);
                }

                vm.incidents.startDate = moment(vm.filter.startDate).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                vm.incidents.endDate = moment(vm.filter.endDate).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]);

                setDataFromMemento();
                setStudentAndGroupsToSync();

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
                vm.filter.groups = vm.groupsSearch.getSelectedGroups().map(group => group["id"]);

                setStudentAndGroupsToSync();
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

            vm.searchStudent = async (searchText: string): Promise<void> => {
                await vm.studentsSearch.searchStudents(searchText);
                $scope.safeApply();
            };

            vm.selectStudents = (model: string, option: Student): void => {
                vm.updateFilter(option);
                vm.studentsSearch.selectStudents(model, option);
                vm.studentsSearch.student = "";
                vm.filter.student.search = '';
            };

            vm.removeStudents = (student: Student): void => {
                vm.filter.students = _.without(vm.filter.students, _.findWhere(vm.filter.students, student));
                vm.updateFilter();
            };

            /* Search bar groups section */
            vm.searchGroup = async (searchText: string): Promise<void> => {
                await vm.groupsSearch.searchGroups(searchText);
                $scope.safeApply();
            };

            vm.selectGroup = (valueInput: string, groupForm: Group): void => {
                vm.groupsSearch.selectGroups(valueInput, groupForm);
                vm.filter.groups = vm.groupsSearch.getSelectedGroups().map(group => group["id"]);
                vm.groupsSearch.group = "";
                vm.updateFilter();
            };

            vm.removeSelectedGroups = (groupForm: Group): void => {
                vm.groupsSearch.removeSelectedGroups(groupForm);
                vm.filter.groups = vm.groupsSearch.getSelectedGroups().map(group => group["id"]);
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