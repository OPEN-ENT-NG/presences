import {_, idiom as lang, moment, ng} from 'entcore';
import {
    Group, GroupingService,
    GroupService,
    Registry,
    RegistryDays,
    RegistryEvent,
    RegistryRequest,
    RegistryService,
    SearchService
} from "../services";
import {DateUtils, GroupsSearch} from "@common/utils";
import {EventType} from "../models";
import {EVENT_TYPE} from "@common/core/enum/event-type";

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

interface EventRegistryCard {
    events: RegistryEvent[];
    date: string;
    displayName: string;
    forgottenNotebook: boolean;
}

interface ViewModel {
    params: RegistryRequest;
    registries: Registry[];
    emptyState: string;
    months: { name: string, value: string }[];
    monthLength: number[];
    eventType: string[]; /* [0]:ABSENCE, [1]:LATENESS, [2]:INCIDENT, [3]:DEPARTURE */
    eventCardId: number;
    eventCardData: EventRegistryCard;
    groupsSearch: GroupsSearch;

    removeGroup(group: Group): void;

    updateMonth(): void;

    isFilterActive(typeName: string): boolean;

    toggleFilter(typeName: string): void

    toggleForgottenNotebookFilter(): void;

    selectGroup(model: any, student: any): void;
    searchGroup(value: string): Promise<void>;

    hasEventType(event: RegistryEvent[], eventTypeName: string): boolean;

    hasReason(event: RegistryEvent[]): boolean;

    isAbsenceRegularized(event: RegistryEvent[]): boolean;

    isAbsenceFollowed(event: RegistryEvent[]): boolean;

    openEventCard($event, student: string, day: RegistryDays, events: RegistryEvent[]): void;

    formatDate(date: string): string;
    closeEventCard(): void;

    // CSV
    exportCsv(): void;
}

export const registryController = ng.controller('RegistryController', ['$scope', '$route', '$location',
    'SearchService', 'GroupService', 'GroupingService', 'RegistryService',
    function ($scope, $route, $location, searchService: SearchService, groupService: GroupService, groupingService: GroupingService,
              registryService: RegistryService) {
        const vm: ViewModel = this;

        /* Fetching information from URL Param and cloning new object RegistryRequest */
        vm.params = Object.assign({}, $location ? $location.search() : null);
        vm.eventType = [
            EventType[EventType.ABSENCE],
            EventType[EventType.LATENESS],
            EventType[EventType.INCIDENT],
            EventType[EventType.DEPARTURE]
        ];
        vm.eventCardId = null;
        vm.eventCardData = {} as EventRegistryCard;
        /* Setting url param default if param not fetched or empty */
        if ($location && Object.getOwnPropertyNames(vm.params).length === 0) {
            $location.search({
                month: moment(new Date()).format(DateUtils.FORMAT["YEAR-MONTH"]),
                type: vm.eventType
            });
            vm.params = Object.assign({}, $location.search());
        }

        vm.emptyState = '';
        vm.groupsSearch = new GroupsSearch(window.structure.id, searchService, groupService, groupingService);
        vm.groupsSearch.setSelectedGroups(window.item.groupList);

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

        const createEventDisplay = (): void => {
            vm.registries.forEach(registry => {
                registry.days.forEach(day => {
                    let absences = _.uniq(JSON.parse(JSON.stringify(day.events))
                        .filter(item => item.type === vm.eventType[0]), 'reason');

                    let events = _.uniq(JSON.parse(JSON.stringify(day.events))
                        .filter(item => item.type !== vm.eventType[0]), 'type');
                    day.eventsDisplay = events.concat(absences);
                })
            });
        };

        const offset = (el): { top: number, left: number, x: number, right: number } => {
            let rect = el.getBoundingClientRect();
            let scrollLeft = window.pageXOffset || document.documentElement.scrollLeft;
            let scrollTop = window.pageYOffset || document.documentElement.scrollTop;
            return {top: rect.top + scrollTop, left: rect.left + scrollLeft, x: rect.x, right: rect.right};
        };

        const getRegisterSummary = async (): Promise<void> => {
            vm.registries = await registryService.getRegisterSummary(vm.params);
            let monthLength = [];
            if (vm.registries.length > 1) {
                createEventDisplay();
                vm.registries[0].days.forEach((value, index: number) => {
                    monthLength.push(index);
                });
                vm.monthLength = monthLength;
            } else {
                vm.monthLength = [];
            }
            document.getElementById('event-card').style.display = 'none';
            $scope.safeApply();
        };

        const updateGroup = () => {
            vm.params.group = vm.groupsSearch.getGroups().map((group: Group) => group.id);
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

        vm.removeGroup = async (item: Group): Promise<void> => {
            vm.groupsSearch.removeSelectedGroups(item);
            updateGroup();
            await resetRegistries()
        };

        vm.updateMonth = async (): Promise<void> => {
            await resetRegistries()
        };

        vm.isFilterActive = (typeName: string) => {
            return vm.params.type.some(type => type === typeName);
        };

        vm.toggleFilter = async (typeName: string): Promise<void> => {
            if (vm.params.type.indexOf(typeName) > -1) {
                vm.params.type = vm.params.type.filter(item => item !== typeName)
            } else {
                vm.params.type.push(typeName);
            }
            await resetRegistries()
        };

        vm.toggleForgottenNotebookFilter = async (): Promise<void> => {
            vm.params.forgottenNotebook = !vm.params.forgottenNotebook;
            await resetRegistries()
        };

        const resetRegistries = async (): Promise<void> => {
            $location.search(vm.params);
            if ((vm.params.type.length === 0 && !vm.params.forgottenNotebook) || vm.params.group.length === 0) {
                vm.registries = [];
                vm.monthLength = [];
                vm.emptyState = setEmptyState();
                $scope.safeApply();
                return;
            }
            await getRegisterSummary();
        }

        /* Groups interaction */
        vm.selectGroup = (model, item: Group) => {
            vm.groupsSearch.selectGroups(model, item);
            updateGroup();
            $location.search(vm.params);
            if (vm.params.type.length === 0 || vm.params.group.length === 0) {
                vm.registries = [];
                vm.monthLength = [];
                vm.emptyState = setEmptyState();
                return;
            }
            getRegisterSummary();
            vm.groupsSearch.group = '';
        };

        vm.searchGroup = async (value) => {
            await vm.groupsSearch.searchGroups(value);
        };

        vm.hasEventType = (event: RegistryEvent[], eventTypeName: string): boolean => {
            if (event.length === 0) {
                return false;
            }
            return event.some((event: RegistryEvent) => event.type === eventTypeName);
        };

        vm.hasReason = (event: RegistryEvent[]): boolean => {
            if (event.length === 0) {
                return false;
            }
            return event.some((event: RegistryEvent) => event.hasOwnProperty('reason_id'));
        };

        vm.isAbsenceRegularized = (event: Array<RegistryEvent>): boolean => {
            return (event.length > 0) && !event.some((event: RegistryEvent) => event.type === EVENT_TYPE.ABSENCE &&
                event.counsellor_regularisation === false);
        };

        vm.isAbsenceFollowed = (event: RegistryEvent[]): boolean => {
            if (event.length === 0) {
                return false;
            }
            return !event.some((event: RegistryEvent) => event.followed === false);
        };

        vm.openEventCard = ($event, student: string, day: RegistryDays, events: RegistryEvent[]): void => {
            if (events.length === 0 && !day.forgottenNotebook) {
                return;
            }

            vm.eventCardData.events = events;
            vm.eventCardData.date = DateUtils.format(day.date, DateUtils.FORMAT["DAY-MONTH-YEAR-LETTER"]);
            vm.eventCardData.displayName = student;
            vm.eventCardData.forgottenNotebook = day.forgottenNotebook;

            const hover = document.getElementById('event-card');
            const widthEventCard = hover.querySelector('.registry-event-card-header').clientWidth || 400;
            const windowWidth = document.getElementsByTagName('html')[0].clientWidth;
            const heightEventCard = 90;

            let {top, left, x, right} = offset($event.target.closest('.registry-table-event-period-list'));

            hover.style.top = `${top - heightEventCard}px`;
            if (right + widthEventCard > windowWidth) {
                hover.style.left = `${left - (widthEventCard + 35)}px`;
            } else {
                hover.style.left = `${x + (widthEventCard / 100)}px`;
            }
            hover.style.display = 'flex';
            $scope.safeApply();
        };

        vm.formatDate = (date: string): string => {
            return moment(date).format(DateUtils.FORMAT["HOUR-MINUTES"]).replace(':', 'h');
        };

        vm.closeEventCard = (): void => {
            document.getElementById('event-card').style.display = 'none';
            vm.eventCardData = {} as EventRegistryCard;
        };

        /* CSV  */
        vm.exportCsv = (): void => {
            registryService.exportCSV(vm.params);
        };

        /* on switch (watch) */
        $scope.$watch(() => window.structure, () => {
            vm.params.structureId = window.structure.id;
            $location.search(vm.params);
            getRegisterSummary();
        });
    }]);