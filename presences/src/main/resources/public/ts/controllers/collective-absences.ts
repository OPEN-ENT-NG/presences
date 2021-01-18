import {ng} from 'entcore';
import {DateUtils} from '@common/utils';
import {SearchService} from '@common/services/SearchService';
import {ICollectiveAbsence, ICollectiveAbsenceBody, ICollectiveAbsencesResponse} from '../models/CollectiveAbsence';
import {CollectiveAbsenceService} from '@presences/services/CollectiveAbsenceService';
import {GroupsSearch} from '@common/utils/autocomplete/groupsSearch';
import {Group, GroupService} from '@common/services';
import {COLLECTIVE_ABSENCE_FORM_EVENTS} from '../core/enum/collective-absences-events';
import {SNIPLET_FORM_EMIT_COLLECTIVE_ABSENCES_EVENTS} from '../core/enum/collective-absences-events';


declare let window: any;

interface IFilter {
    startDate: Date;
    endDate: Date;
    groups: Array<string>;
    page: number;
}

interface IViewModel {
    filter: IFilter;
    groupsSearch: GroupsSearch;
    collectiveAbsences: ICollectiveAbsence[];
    pageCount: number;
    collectivesLoaded: boolean;

    getCollectiveAbsences(): Promise<void>;

    openEditCollectiveAbsence(collectiveId: number): void;

    formatDateDisplay(date: string): string;

    openCreateCollectiveAbsence(): void;

    updateFilter(): Promise<void>;

    changePagination(): Promise<void>;

    exportCsv(): Promise<void>;

    /* search bar methods */
    searchGroup(groupForm: string): Promise<void>;

    selectGroup(valueInput: string, groupItem: Group): void;

    removeSelectedGroups(groupItem: Group): void;
}

export const collectiveAbsencesController = ng.controller('CollectiveAbsencesController',
    ['$scope', 'route', '$location', 'CollectiveAbsenceService', 'SearchService', 'GroupService',
        async function ($scope, route, $location, collectiveAbsenceService: CollectiveAbsenceService,
                        searchService: SearchService, groupService: GroupService) {
            const vm: IViewModel = this;

            vm.filter = {
                startDate: DateUtils.add(new Date(), -30, 'd'),
                endDate: new Date(),
                groups: [],
                page: 0
            };

            vm.collectivesLoaded = false;
            vm.groupsSearch = undefined;
            vm.collectiveAbsences = [];
            vm.pageCount = 0;

            const initData = async (): Promise<void> => {
                vm.groupsSearch = new GroupsSearch(window.structure.id, searchService, groupService);
                await vm.getCollectiveAbsences();
            };

            vm.getCollectiveAbsences = async (): Promise<void> => {

                let startDate: string = DateUtils.format(
                    DateUtils.setFirstTime(vm.filter.startDate), DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC']);
                let endDate: string = DateUtils.format(
                    DateUtils.setLastTime(vm.filter.endDate), DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC']);

                let collectiveParams: ICollectiveAbsenceBody = {
                    startDate: startDate,
                    endDate: endDate,
                    groups: vm.filter.groups,
                    page: vm.filter.page
                };
                await collectiveAbsenceService.getCollectiveAbsences(window.structure.id, collectiveParams)
                    .then((res: ICollectiveAbsencesResponse) => {
                        vm.collectiveAbsences = res.all;
                        vm.pageCount = res.page_count;
                        vm.collectivesLoaded = true;
                        $scope.safeApply();
                });

            };

            vm.openCreateCollectiveAbsence = (): void => {
                $scope.$broadcast(COLLECTIVE_ABSENCE_FORM_EVENTS.CREATE);
            };

            vm.openEditCollectiveAbsence = (collectiveId: number): void => {
                $scope.$broadcast(COLLECTIVE_ABSENCE_FORM_EVENTS.EDIT, {id: collectiveId});
            };

            vm.formatDateDisplay = (date: string): string => {
                return DateUtils.format(date, DateUtils.FORMAT['DAY-MONTH-YEAR']);
            };

            vm.updateFilter = async (): Promise<void> => {
                if (vm.groupsSearch) {
                    vm.filter.groups = vm.groupsSearch.getSelectedGroups().map(group => group['name']);
                }

                vm.filter.page = 0;

                if ('structure' in window) {
                    await vm.getCollectiveAbsences();
                }

                $scope.safeApply();
            };

            vm.changePagination = async (): Promise<void> => {
                await vm.getCollectiveAbsences();
            };

            vm.exportCsv = async (): Promise<void> => {

                let startDate: string = DateUtils.format(
                    DateUtils.setFirstTime(vm.filter.startDate), DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC']);
                let endDate: string = DateUtils.format(
                    DateUtils.setLastTime(vm.filter.endDate), DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC']);

                await collectiveAbsenceService.exportCollectiveAbsences(window.structure.id, startDate, endDate);
            };

            /**
             * ⚠ Autocomplete classes/methods for students
             */
            vm.searchGroup = async (groupForm: string): Promise<void> => {
                await vm.groupsSearch.searchGroups(groupForm);
                $scope.safeApply();
            };

            vm.selectGroup = (valueInput: string, groupForm: Group): void => {
                vm.groupsSearch.selectGroups(valueInput, groupForm);
                vm.filter.groups = vm.groupsSearch.getSelectedGroups().map(group => group['id']);
                vm.groupsSearch.group = '';
                vm.updateFilter();
            };

            vm.removeSelectedGroups = (groupForm: Group): void => {
                vm.groupsSearch.removeSelectedGroups(groupForm);
                vm.filter.groups = vm.groupsSearch.getSelectedGroups().map(group => group['id']);
                vm.updateFilter();
            };

            $scope.$on(SNIPLET_FORM_EMIT_COLLECTIVE_ABSENCES_EVENTS.CREATION, vm.updateFilter);
            $scope.$on(SNIPLET_FORM_EMIT_COLLECTIVE_ABSENCES_EVENTS.EDIT, vm.updateFilter);
            $scope.$on(SNIPLET_FORM_EMIT_COLLECTIVE_ABSENCES_EVENTS.DELETE, vm.updateFilter);
            
            $scope.$watch(() => window.structure, async () => {
                if ('structure' in window) {
                    await initData();
                }
            });
}]);