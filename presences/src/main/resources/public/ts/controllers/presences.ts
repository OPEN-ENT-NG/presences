import {model, moment, ng, toasts} from 'entcore';
import {Presence, PresenceRequest, Presences} from "../models";
import {DateUtils} from "@common/utils";
import {StudentsSearch, UsersSearch} from "../utilities";
import {Group, GroupingService, GroupService, PresenceService, SearchService} from "../services";
import {SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from "@common/model";
import {GroupsSearch} from "@common/utils/autocomplete/groupsSearch";

declare let window: any;

interface PresencesFilter {
    mine: boolean;
}

interface Filter {
    startDate: string;
    endDate: string;
    personalIds: Array<String>;
    studentsIds: Array<String>;
    audiencesIds: Array<String>;
    presencesFilter: PresencesFilter;
}

interface ViewModel {
    filter: Filter;
    presences: Presences;

    presencesRequest: PresenceRequest;

    presencesFilter: Array<String>;
    studentsSearch: StudentsSearch;
    usersSearch: UsersSearch;
    groupsSearch: GroupsSearch;

    updateFilter(): Promise<void>;

    formatHour(date: string): string;

    formatDayDate(date: string): string;

    togglePresencesFilter(presenceFilterKey: string): Promise<void>;

    openPresence(presence: Presence): void;

    /* search bar method */
    searchUser(studentForm: string): Promise<void>;

    selectUser(valueInput, userItem): void;

    removeSelectedUsers(userItem): void;

    searchStudent(studentForm: string): Promise<void>;

    selectStudent(valueInput, studentItem): void;

    removeSelectedStudents(studentItem): void;

    searchGroup(groupForm: string): Promise<void>;

    selectGroup(valueInput: string, groupItem: Group): void;

    removeSelectedGroups(groupItem: Group): void;

    // CSV
    exportCsv(): void;
}

export const presencesController = ng.controller('PresencesController',
    ['$scope', '$route', '$location', 'SearchService', 'PresenceService', 'GroupService', 'GroupingService',
        function ($scope, $route, $location, searchService: SearchService, presenceService: PresenceService,
                  groupService: GroupService, groupingService: GroupingService) {
            const vm: ViewModel = this;

            /* Init filter */
            vm.filter = {
                startDate: moment().add(-1, 'M').startOf('day'),
                endDate: moment().endOf('day'),
                personalIds: [],
                studentsIds: [],
                audiencesIds: [],
                presencesFilter: {
                    mine: false
                }
            };

            vm.presences = undefined;
            vm.presencesRequest = {} as PresenceRequest;

            /* Init search bar */
            vm.studentsSearch = undefined;
            vm.usersSearch = undefined;
            vm.groupsSearch = undefined;

            /* presenceFilter keys dynamically : [mine] */
            vm.presencesFilter = Object.keys(vm.filter.presencesFilter);

            const startAction = () => {
                vm.presences = new Presences(window.structure.id);
                vm.studentsSearch = new StudentsSearch(window.structure.id, searchService);
                vm.usersSearch = new UsersSearch(window.structure.id, searchService);
                vm.groupsSearch = new GroupsSearch(window.structure.id, searchService, groupService, groupingService);

                /* event */
                vm.presences.eventer.on('loading::true', () => $scope.safeApply());
                vm.presences.eventer.on('loading::false', () => $scope.safeApply());
                switch ($route.current.action) {
                    case 'dashboard': {
                        actions.dayPresencesWidget();
                        break;
                    }
                    case 'presences': {
                        actions.getPresencesWidget();
                        break;
                    }
                    default:
                        return;
                }
            };

            const actions = {
                dayPresencesWidget: () => getDayPresences(moment(new Date()).format(DateUtils.FORMAT["YEAR-MONTH-DAY"])),
                getPresencesWidget: () => getPresences()
            };

            const getDayPresences = async (date: string): Promise<void> => {
                vm.presences = new Presences(window.structure.id);
                let presenceRequest: PresenceRequest = {
                    structureId: window.structure.id,
                    startDate: date,
                    endDate: date
                };
                await vm.presences.build(await presenceService.get(presenceRequest));
                $scope.safeApply();
            };

            const getPresences = async (): Promise<void> => {
                vm.presences.loading = true;
                prepareRequest();
                await vm.presences.build(await presenceService.get(vm.presencesRequest));
                vm.presences.loading = false;
                $scope.safeApply();
            };

            const prepareRequest = (): void => {
                vm.presencesRequest.structureId = vm.presences.structureId;
                vm.presencesRequest.startDate = DateUtils.format(vm.filter.startDate, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                vm.presencesRequest.endDate = DateUtils.format(vm.filter.endDate, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                vm.presencesRequest.studentIds = vm.filter.studentsIds;
                vm.presencesRequest.ownerIds = vm.filter.personalIds;
                vm.presencesRequest.audienceIds = vm.filter.audiencesIds;
                if (vm.filter.presencesFilter.mine) {
                    if (vm.presencesRequest.ownerIds.indexOf(model.me.userId) === -1) {
                        vm.presencesRequest.ownerIds.push(model.me.userId);
                    }
                }
            };

            vm.updateFilter = async (): Promise<void> => {
                /* get our search bar info */
                vm.filter.studentsIds = vm.studentsSearch.getSelectedStudents().map(student => student['id']);
                vm.filter.personalIds = vm.usersSearch.getSelectedUsers().map(user => user['id']);
                vm.filter.audiencesIds = vm.groupsSearch.getSelectedGroups().map(group => group['id']);

                await getPresences();
            };

            vm.togglePresencesFilter = async (presenceFilterKey: string): Promise<void> => {
                vm.filter.presencesFilter[presenceFilterKey] = !vm.filter.presencesFilter[presenceFilterKey];
                await vm.updateFilter();
            };

            vm.openPresence = (presence: Presence): void => {
                $scope.$broadcast(SNIPLET_FORM_EVENTS.SET_PARAMS, JSON.parse(JSON.stringify(presence)));
            };

            vm.formatHour = (date: string): string => DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"]);
            vm.formatDayDate = (date: string): string => DateUtils.format(parseInt(date), DateUtils.FORMAT["DAY-DATE"]);

            /* Search bar users section */
            vm.searchUser = async (userForm: string): Promise<void> => {
                await vm.usersSearch.searchUsers(userForm);
                $scope.safeApply();
            };

            vm.selectUser = (valueInput, userItem): void => {
                vm.usersSearch.selectUsers(valueInput, userItem);
                vm.usersSearch.user = "";
                vm.updateFilter();
            };

            vm.removeSelectedUsers = (userItem): void => {
                vm.usersSearch.removeSelectedUsers(userItem);
                vm.updateFilter();
            };

            /* Search bar student section */
            vm.searchStudent = async (studentForm: string): Promise<void> => {
                await vm.studentsSearch.searchStudents(studentForm);
                $scope.safeApply();
            };

            vm.selectStudent = (valueInput, studentItem): void => {
                vm.studentsSearch.selectStudents(valueInput, studentItem);
                vm.studentsSearch.student = "";
                vm.updateFilter();
            };

            vm.removeSelectedStudents = (studentItem): void => {
                vm.studentsSearch.removeSelectedStudents(studentItem);
                vm.updateFilter();
            };

            /* Search bar groups section */
            vm.searchGroup = async (groupForm: string): Promise<void> => {
                await vm.groupsSearch.searchGroups(groupForm);
                $scope.safeApply();
            };

            vm.selectGroup = (valueInput: string, groupItem: Group): void => {
                vm.groupsSearch.selectGroups(valueInput, groupItem);
                vm.filter.audiencesIds = vm.groupsSearch.getSelectedGroups().map(group => group['id']);
                vm.groupsSearch.group = '';
                vm.updateFilter();
            };

            vm.removeSelectedGroups = (groupItem: Group): void => {
                vm.groupsSearch.removeSelectedGroups(groupItem);
                vm.filter.audiencesIds = vm.groupsSearch.getSelectedGroups().map(group => group['id']);
                vm.updateFilter();
            };

            /* CSV  */
            vm.exportCsv = (): void => {
                if (vm.presences.all.length > 1000) {
                    toasts.warning('presences.presences.csv.full');
                } else {
                    if (vm.presences.all.length === 0) {
                        toasts.info('presences.presences.csv.empty');
                    } else {
                        presenceService.exportCSV(vm.presencesRequest);
                    }
                }
            };

            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.CREATION, startAction);
            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.EDIT, startAction);
            $scope.$on(SNIPLET_FORM_EMIT_EVENTS.DELETE, startAction);

            /* on  (watch) */
            $scope.$watch(() => window.structure, () => {
                if ('structure' in window) {
                    startAction();
                }
            });

        }]);