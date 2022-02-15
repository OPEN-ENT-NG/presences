import {_, idiom, model, ng, template} from 'entcore';
import {Reason} from "@presences/models";
import {
    IPunishmentsTypeService,
    IViescolaireService,
    ReasonService,
    SearchService,
    SettingsService,
    ViescolaireService,
    Setting
} from '../services';
import {GroupService} from '@common/services/GroupService';
import {IPunishmentType} from '@incidents/models/PunishmentType';
import {Indicator, IndicatorFactory} from '../indicator';
import {INFINITE_SCROLL_EVENTER} from '@common/core/enum/infinite-scroll-eventer';
import {FILTER_TYPE, FilterType} from '../filter';
import {DateUtils} from '@common/utils';
import {INDICATOR_TYPE} from "../core/constants/IndicatorType";
import {DISPLAY_TYPE} from "../core/constants/DisplayMode";
import {IMonthly, MonthlyStatistics} from "../model/Monthly";
import {EXPORT_TYPE} from "../core/enums/export-type.enum";
import {Weekly} from "@statistics/indicator/Weekly";

declare let window: any;

interface Search {
    student: {
        value: any;
        list: Array<any>;
    },
    audience: {
        value: any;
        list: Array<any>;
    }
}

export interface Filter {
    show: boolean;
    showCSV: boolean;
    from: Date;
    to: Date;
    students: Array<any>;
    audiences: Array<any>;
    filterTypes: FilterType[];
    exportType: string;
}

interface ViewModel {
    $onInit();

    $onDestroy();

    search: Search;
    filter: Filter;
    reasons: Array<Reason>;
    punishmentTypes: Array<IPunishmentType>;
    setting: Setting;
    indicator: Indicator;
    indicators: Array<Indicator>;
    indicatorType: typeof INDICATOR_TYPE;
    exportType: typeof EXPORT_TYPE;
    loading: boolean;
    displayType: typeof DISPLAY_TYPE;

    safeApply(fn?: () => void): void;

    loadData(): Promise<void>;

    onSwitchDisplay(): void;

    isGlobal(indicator: Indicator): boolean;

    isMonthly(indicator: Indicator): boolean;

    isWeekly(indicator: Indicator): boolean;

    getSelectedFilterLabel(): Array<string>;

    launchResearch(): Promise<void>;

    resetIndicator(): Promise<void>;

    updateDate(): Promise<void>;

    switchIndicator(): void;

    openFilter(): void;

    openCSVOptions(): void;

    switchExportType(exportType : string): void;

    export(exportType? : string): void;

    /* search bar methods */

    searchStudent(value: string): void;

    selectStudent(model: any, teacher: any): void;

    searchAudience(value: string): Promise<void>;

    selectAudience(model: any, audience: any): void;

    removeSelection(type: any, value: any): Promise<void>;
}

export const mainController = ng.controller('MainController',
    ['$scope', 'route', 'ReasonService', 'PunishmentsTypeService', 'SearchService', 'GroupService',
        'ViescolaireService', 'SettingService',
        function ($scope, route, ReasonService: ReasonService, punishmentTypeService: IPunishmentsTypeService,
                  SearchService, GroupService: GroupService, ViescolaireService: IViescolaireService,
                  settingService: SettingsService) {
            const vm: ViewModel = this;

            vm.$onInit = async () => {
                vm.indicators = [];
                vm.indicatorType = INDICATOR_TYPE;
                vm.exportType = EXPORT_TYPE;
                vm.reasons = [];
                vm.punishmentTypes = [];
                vm.setting = null;
                vm.loading = false;

                vm.displayType = DISPLAY_TYPE;

                vm.search = {
                    student: {
                        value: null,
                        list: null
                    },
                    audience: {
                        value: null,
                        list: null
                    }
                };

                vm.filter = {
                    show: false,
                    showCSV: false,
                    from: new Date,
                    to: new Date,
                    students: [],
                    audiences: [],
                    filterTypes: [],
                    exportType: null
                }

                $scope.$watch(() => window.structure, async () => {
                    if ('structure' in window) {
                        await init();
                    }
                });

                template.open('main', 'main');
                template.open('filter', 'filter');
                $scope.idiom = idiom;
            };

            async function init() {
                await vm.loadData();
                buildIndicators();
                vm.filter.filterTypes = vm.indicator.cloneFilterTypes();
                if (!vm.indicator !== undefined) {
                    await vm.launchResearch();
                    vm.safeApply();
                }
            }

            function buildIndicators() {
                vm.indicators = [];
                for (const name of window.indicators) {
                    vm.indicators.push(IndicatorFactory.create(name, vm.reasons, vm.punishmentTypes));
                }

                vm.indicator = vm.indicators[0];
            }

            vm.safeApply = function (fn?) {
                const phase = $scope.$root.$$phase;
                if (phase == '$apply' || phase == '$digest') {
                    if (fn && (typeof (fn) === 'function')) {
                        fn();
                    }
                } else {
                    $scope.$apply(fn);
                }
            };

            vm.onSwitchDisplay = async (): Promise<void> => {
                proceedOnChangeFilterMonth();
                await vm.resetIndicator();
            };

            const proceedOnChangeFilterMonth = (): void => {

                /** fetch all absence filter without counting eventType
                 * @return Array<FilterType>
                 */
                const getAbsencesTypeFilters = (): Array<FilterType> => {
                    return vm.filter.filterTypes.filter((f: FilterType) => f.name() === FILTER_TYPE.NO_REASON ||
                        f.name() === FILTER_TYPE.UNREGULARIZED || f.name() === FILTER_TYPE.REGULARIZED);
                };

                /** fetch all event type filter without counting absence filter
                 * @return Array<FilterType>
                 */
                const getEventTypeFilter = (): Array<FilterType> => {
                    return vm.filter.filterTypes.filter((f: FilterType) => f.name() !== FILTER_TYPE.NO_REASON &&
                        f.name() !== FILTER_TYPE.UNREGULARIZED && f.name() !== FILTER_TYPE.REGULARIZED);
                };

                if (vm.indicator.name() === INDICATOR_TYPE.monthly && vm.indicator.display === DISPLAY_TYPE.TABLE) {
                    // check if absence filter has at least one selected
                    if (getAbsencesTypeFilters().some(f => f.selected())) {
                        vm.filter.filterTypes.forEach((type: FilterType) => {
                            // we remove all event type since we have one absence filter selected
                            if (type.name() !== FILTER_TYPE.NO_REASON && type.name() !== FILTER_TYPE.REGULARIZED && type.name() !== FILTER_TYPE.UNREGULARIZED) {
                                type.select(false);
                            }
                        });
                    } else {
                        // this case occurs if none absence filter are selected
                        // therefore we check if all filters are empty or if at least 2 differents events are selected
                        if (vm.filter.filterTypes.every((type: FilterType) => !type.selected()) ||
                            getEventTypeFilter().filter((type: FilterType) => type.selected()).length > 1) {
                            // we reinitialise the filter by its default "NO_REASON"
                            vm.filter.filterTypes.forEach((type: FilterType) => type.select(false));
                            vm.filter.filterTypes.find((type: FilterType) => type.name() === FILTER_TYPE.NO_REASON).select(true);
                        }
                    }
                }
            };

            /**
             * Returns true if indicator is Monthly
             * @param indicator Indicator type
             */
            vm.isGlobal = (indicator: Indicator): boolean => {
                return indicator && indicator.name() === INDICATOR_TYPE.global;
            };

            vm.isMonthly = (indicator: Indicator): boolean => {
                return indicator && indicator.name() === INDICATOR_TYPE.monthly
            };

            vm.isWeekly = (indicator: Indicator): boolean => {
                return indicator && indicator.name() === INDICATOR_TYPE.weekly
            };

            vm.loadData = async (): Promise<void> => {
                if (!window.structure) return;
                await Promise.all([
                    ReasonService.getReasons(window.structure.id),
                    punishmentTypeService.get(window.structure.id),
                    settingService.retrieve(window.structure.id),
                ]).then((values: [Array<Reason>, Array<IPunishmentType>, Setting]) => {
                    vm.reasons = values[0].filter(reason => reason.id !== -1);
                    vm.punishmentTypes = values[1];
                    vm.setting = values[2];
                });
            };

            vm.getSelectedFilterLabel = (): Array<string> => {
                return vm.indicator ? vm.indicator.cloneFilterTypes()
                    .filter((filterType: FilterType) => filterType.selected())
                    .map((filterType: FilterType) => 'statistics-presences.indicator.filter.type.' + filterType.name()) : [];
            };

            async function resetSearch(type) {
                type.value = '';
                type.list = null;
                await vm.resetIndicator();
                vm.safeApply();
            }

            async function searchSelection(type, searchType, object) {
                if (_.findWhere(type, {id: object.id})) {
                    type.value = '';
                    return;
                }

                type.push(object);
                if (vm.isWeekly(vm.indicator)) await (<Weekly> vm.indicator)
                    .initTimeslot(
                        vm.filter.students.map(student => student.id),
                        vm.filter.audiences.map(audience => audience.id)
                    )
                await resetSearch(searchType);
            }

            vm.searchStudent = async function (value: string) {
                const structureId = window.structure.id;
                try {
                    vm.search.student.list = await SearchService.searchUser(structureId, value, 'Student');
                    vm.safeApply();
                } catch (err) {
                    vm.search.student.list = [];
                    throw err;
                }
            };

            vm.selectStudent = async function (model, student) {
                if (vm.isWeekly(vm.indicator)) {
                    vm.filter.students = [];
                }
                vm.filter.audiences = [];
                vm.search.audience.list = null;
                vm.search.audience.value = '';
                searchSelection(vm.filter.students, vm.search.student, student);
            };

            vm.searchAudience = async function (value: string) {
                const structureId = window.structure.id;
                try {
                    vm.search.audience.list = await GroupService.search(structureId, value);
                    vm.search.audience.list.map((obj) => obj.toString = () => obj.name);
                    vm.safeApply();
                } catch (err) {
                    vm.search.audience.list = [];
                    throw err;
                }
                return;
            };

            vm.selectAudience = async function (model, audience) {
                if (vm.isWeekly(vm.indicator)) {
                    vm.filter.audiences = [];
                }
                vm.filter.students = [];
                vm.search.student.list = null;
                vm.search.student.value = '';
                searchSelection(vm.filter.audiences, vm.search.audience, audience);
            };

            vm.removeSelection = async function (type, value): Promise<void> {
                vm.filter[type] = _.without(vm.filter[type], _.findWhere(vm.filter[type], value));
                await vm.resetIndicator();
            };

            vm.launchResearch = async (): Promise<void> => {
                vm.loading = true;
                let users: Array<string> = [];
                let audiences: Array<string> = [];
                vm.filter.students.map(student => users.push(student.id));
                vm.filter.audiences.map(audience => audiences.push(audience.id));
                vm.filter.show = false;
                vm.indicator.from = DateUtils.setFirstTime(vm.filter.from);
                vm.indicator.to = DateUtils.setLastTime(vm.filter.to);
                template.open('indicator', `indicator/${vm.indicator.name()}`);
                try {
                    await vm.indicator.search(vm.indicator.from, vm.indicator.to, users, audiences);
                    if (vm.filter.students.length > 0 && (vm.indicator.values as IMonthly).data) {
                        (vm.indicator.values as IMonthly).data.forEach((audience: MonthlyStatistics) => {
                            audience.isClicked = true;
                        });
                    }
                } catch (err) {
                    vm.loading = false;
                    vm.safeApply();
                }

                vm.loading = false;
                vm.safeApply();
            }

            vm.resetIndicator = async (): Promise<void> => {
                vm.indicator.page = 0;
                vm.indicator.resetValues();
                vm.indicator.setFilterTypes(vm.filter.filterTypes);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                await vm.launchResearch();
                vm.safeApply();
            }

            vm.updateDate = async (): Promise<void> => {
                vm.indicator.page = 0;
                vm.indicator.resetValues();
                vm.indicator.setFilterTypes(vm.filter.filterTypes);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                if (vm.filter.from && vm.filter.to &&
                    ((vm.indicator.from.toDateString() !== vm.filter.from.toDateString()) ||
                        (vm.indicator.to.toDateString() !== vm.filter.to.toDateString()))) {
                    await vm.launchResearch();
                }
                vm.safeApply();
            }

            vm.switchIndicator = async (): Promise<void> => {
                if (model.calendar.callbacks)
                    model.calendar.callbacks['date-change'] = [];
                vm.indicator.page = 0;
                vm.indicator.resetValues();
                vm.indicator.resetDisplayMode();
                vm.filter.filterTypes = vm.indicator.cloneFilterTypes();
                vm.indicator.setFilterTypes(vm.filter.filterTypes);
                await vm.indicator.resetDates();
                vm.filter.from = vm.indicator.from;
                vm.filter.to = vm.indicator.to;
                if (vm.isWeekly(vm.indicator)) {
                    vm.filter.students = [];
                    vm.filter.audiences = [];
                    await (<Weekly> vm.indicator).initTimeslot(
                        [],
                        []
                    );
                }
                await vm.launchResearch();
                vm.safeApply();
            };

            vm.openFilter = function () {
                vm.filter.filterTypes = vm.indicator.cloneFilterTypes();
                vm.filter.show = true;
            }

            vm.openCSVOptions = (): void => {
                if (vm.indicator.name() === INDICATOR_TYPE.monthly) {
                    vm.filter.showCSV = true;
                    vm.filter.exportType = EXPORT_TYPE.ALL;
                } else {
                    vm.export();
                }

            }

            vm.switchExportType = (exportType : string): void => {
                vm.filter.exportType = exportType;
            }

            vm.export = (exportType? : string): void => {
                let users = [];
                let audiences = [];
                vm.filter.students.map(student => users.push(student.id));
                vm.filter.audiences.map(audience => audiences.push(audience.id));
                vm.indicator.export(vm.indicator.from, vm.indicator.to, users, audiences, exportType);
                if (exportType) {
                    vm.filter.showCSV = false
                }
            }
        }]);
