import {_, idiom as lang, moment, ng} from 'entcore';
import {
    Group,
    GroupService,
    Registry,
    RegistryEvent,
    RegistryRequest,
    RegistryService,
    SearchService
} from "../services";
import {DateUtils} from "@common/utils";
import {EventType} from "../models";

declare let window: any;

/* Months configuration */
const monthsNumber = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
const currentMonth = new Date().getMonth() + 1;

/*Sorted months depend on your current month */
const sortedMonths = monthsNumber.slice(currentMonth).concat(monthsNumber.slice(0, currentMonth));

const monthsData = [
    {name: "september", value: 9}, {name: "october", value: 10}, {name: "november", value: 11},
    {name: "december", value: 12}, {name: "january", value: 1}, {name: "february", value: 2},
    {name: "march", value: 3}, {name: "april", value: 4}, {name: "may", value: 5},
    {name: "june", value: 6}, {name: "july", value: 7}, {name: "august", value: 8},
];

interface Filter {
    searchGroup: {
        item: string;
        items: Group[];
    }
    groups: Group[];
}

interface ViewModel {
    params: RegistryRequest;
    registries: Registry[];
    filter: Filter;
    emptyState: string;
    months: { name: string, value: string }[];
    monthLength: number[];
    eventType: string[]; /* [0]:ABSENCE, [1]:LATENESS, [2]:INCIDENT, [3]:DEPARTURE */

    removeGroup(group: Group): void;

    updateMonth(): void;

    isFilterActive(typeName: string): boolean;

    toggleFilter(typeName: string): void

    selectGroup(model: any, student: any): void;
    searchGroup(value: string): Promise<void>;

    hasEventType(event: RegistryEvent[], eventTypeName: string): boolean;
}

export const registryController = ng.controller('RegistryController', ['$scope', 'route', '$location',
    'SearchService', 'GroupService', 'RegistryService',
    function ($scope, route, $location, SearchService: SearchService, groupService: GroupService, registryService: RegistryService) {
        const vm: ViewModel = this;

        /* Fetching information from URL Param and cloning new object RegistryRequest */
        vm.params = Object.assign({}, $location.search());
        vm.eventType = [
            EventType[EventType.ABSENCE],
            EventType[EventType.LATENESS],
            EventType[EventType.INCIDENT],
            EventType[EventType.DEPARTURE]
        ];
        /* Setting url param default if param not fetched or empty */
        if (Object.getOwnPropertyNames(vm.params).length === 0) {
            $location.search({
                month: moment(new Date()).format(DateUtils.FORMAT["YEAR-MONTH"]),
                type: vm.eventType
            });
            vm.params = Object.assign({}, $location.search());
        }

        vm.filter = {
            searchGroup: {
                item: null,
                items: null
            },
            groups: [window.item]
        };
        vm.emptyState = '';

        const getDynamicMonths = (): { name: string, value: string }[] => {
            let months = [];
            for (let i = 0; i < sortedMonths.length; i++) {
                for (let j = 0; j < monthsData.length; j++) {
                    if (sortedMonths[i] === monthsData[j].value) {
                        let monthData = {
                            name: lang.translate('presences.months.' + getMonthNumber(monthsData[j])) + ' - '
                                + (isAfterThanJanuary(sortedMonths[i]) ? new Date().getFullYear().toString() :
                                    (new Date().getFullYear() - 1).toString()),

                            value: (isAfterThanJanuary(sortedMonths[i]) ? new Date().getFullYear().toString() :
                                (new Date().getFullYear() - 1).toString()) + '-' + getMonthNumber(monthsData[j])
                        };
                        months.push(monthData);
                    }
                }
            }
            return months;

            function isAfterThanJanuary(item): boolean {
                return sortedMonths.indexOf(1) <= sortedMonths.indexOf(item)
            }

            function getMonthNumber(monthNumber): string {
                return (monthNumber.value.toString().length > 1 ? monthNumber.value.toString() : '0' + monthNumber.value.toString());
            }
        };
        vm.months = getDynamicMonths();

        const squashEvent = (): void => {
            vm.registries.forEach(registry => {
                registry.days.forEach(day => {
                    day.events = _.uniq(day.events, 'type');
                })
            });
        };

        const getRegisterSummary = async (): Promise<void> => {
            vm.registries = await registryService.getRegisterSummary(vm.params);
            let monthLength = [];
            if (vm.registries.length > 1) {
                squashEvent();
                vm.registries[0].days.forEach((value, index: number) => {
                    monthLength.push(index);
                });
                vm.monthLength = monthLength;
            } else {
                vm.monthLength = [];
            }
            $scope.safeApply();
        };

        const updateGroup = () => {
            let groups = [];
            vm.filter.groups.forEach(item => {
                groups.push(item.id);
            });
            vm.params.group = groups;
        };

        const setEmptyState = (): string => {
            let emptyState = '';
            if (vm.params.group.length === 0) {
                emptyState = 'presences.registry.empty.state';
            }
            if (vm.params.type.length === 0) {
                emptyState = 'presences.registry.empty.state.filter';
            }
            if (vm.params.group.length === 0 && vm.params.type.length === 0) {
                emptyState = 'presences.registry.empty.state.both';
            }
            return emptyState;
        };

        vm.removeGroup = (item: Group) => {
            vm.filter.groups = vm.filter.groups.filter(element => element.id !== item.id);
            updateGroup();
            $location.search(vm.params);
            if (vm.params.type.length === 0 || vm.params.group.length === 0) {
                vm.registries = [];
                vm.monthLength = [];
                vm.emptyState = setEmptyState();
                $scope.safeApply();
                return;
            }
            getRegisterSummary();
        };

        vm.updateMonth = () => {
            $location.search(vm.params);
            if (vm.params.type.length === 0 || vm.params.group.length === 0) {
                vm.registries = [];
                vm.monthLength = [];
                vm.emptyState = setEmptyState();
                $scope.safeApply();
                return;
            }
            getRegisterSummary();
        };

        vm.isFilterActive = (typeName: string) => {
            return vm.params.type.some(type => type === typeName);
        };

        vm.toggleFilter = (typeName: string) => {
            if (vm.params.type.indexOf(typeName) > -1) {
                vm.params.type = vm.params.type.filter(item => item !== typeName)
            } else {
                vm.params.type.push(typeName);
            }
            $location.search(vm.params);
            if (vm.params.type.length === 0 || vm.params.group.length === 0) {
                vm.registries = [];
                vm.monthLength = [];
                vm.emptyState = setEmptyState();
                $scope.safeApply();
                return;
            }
            getRegisterSummary();
        };

        /* Groups interaction */
        vm.selectGroup = (model, item: Group) => {
            vm.filter.groups.push(item);
            updateGroup();
            $location.search(vm.params);
            if (vm.params.type.length === 0 || vm.params.group.length === 0) {
                vm.registries = [];
                vm.monthLength = [];
                vm.emptyState = setEmptyState();
                return;
            }
            getRegisterSummary();
            vm.filter.searchGroup.item = '';
        };

        vm.searchGroup = async (value) => {
            const structureId = window.structure.id;
            try {
                vm.filter.searchGroup.items = await groupService.search(structureId, value);
                vm.filter.searchGroup.items.forEach((item) => item.toString = () => item.name);
                $scope.safeApply();
            } catch (err) {
                vm.filter.searchGroup.items = [];
            }
        };

        vm.hasEventType = (event: RegistryEvent[], eventTypeName: string): boolean => {
            if (event.length === 0) {
                return false;
            }
            return event.some(event => event.type === eventTypeName);
        };

        /* on switch (watch) */
        $scope.$watch(() => window.structure, () => {
            getRegisterSummary();
        });
    }]);