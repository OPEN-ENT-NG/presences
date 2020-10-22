import {_, idiom, moment, ng, template} from 'entcore';
import {Reason} from "@presences/models";
import {IViescolaireService, ReasonService, SearchService, ViescolaireService} from "../services";
import {GroupService} from "@common/services/GroupService";
import {Indicator, IndicatorFactory} from "../indicator";
import {INFINITE_SCROLL_EVENTER} from "@common/core/enum/infinite-scroll-eventer";
import {FilterType} from "../filter";
import {DateUtils} from "@common/utils";

declare let window: any;

interface Search {
    student: {
        value: any
        list: Array<any>
    },
    audience: {
        value: any
        list: Array<any>
    }
}

interface Filter {
    show: boolean
    from: Date
    to: Date
    students: Array<any>
    audiences: Array<any>
    filterTypes: FilterType[]
}

interface ViewModel {
    search: Search
    filter: Filter
    reasons: Array<Reason>
    indicator: Indicator
    indicators: Array<Indicator>

    safeApply(fn?: () => void): void

    loadData(): Promise<void>

    searchStudent(value: string): void

    selectStudent(model: any, teacher: any): void

    searchAudience(value: string): Promise<void>

    selectAudience(model: any, audience: any): void

    removeSelection(type: any, value: any): void

    launchResearch(): void

    resetIndicator(): void;

    openFilter(): void;

    export(): void;
}

export const mainController = ng.controller('MainController',
    ['$scope', 'route', 'ReasonService', 'SearchService', 'GroupService', 'ViescolaireService',
        function ($scope, route, ReasonService: ReasonService, SearchService: SearchService, GroupService: GroupService, ViescolaireService: IViescolaireService) {
            const vm: ViewModel = this;
            vm.indicators = [];
            vm.reasons = [];

            function buildIndicators() {
                vm.indicators = [];
                for (const name of window.indicators) {
                    vm.indicators.push(IndicatorFactory.create(name, vm.reasons));
                }

                vm.indicator = vm.indicators[0];
            }

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
                from: new Date,
                to: new Date,
                students: [],
                audiences: [],
                filterTypes: []
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

            vm.loadData = async function () {
                if (!window.structure) return;
                const schoolYear = await ViescolaireService.getSchoolYearDates(window.structure.id);
                vm.filter.from = DateUtils.setFirstTime(new Date());
                vm.filter.to = DateUtils.setLastTime(new Date());
                vm.reasons = await ReasonService.getReasons(window.structure.id);
                vm.reasons = vm.reasons.filter(reason => reason.id !== -1);
            };

            function resetSearch(type) {
                type.value = '';
                type.list = null;
                vm.resetIndicator();
                vm.safeApply();
            }

            function searchSelection(type, searchType, object) {
                if (_.findWhere(type, {id: object.id})) {
                    type.value = '';
                    return;
                }

                type.push(object);
                resetSearch(searchType);
            }

            vm.searchStudent = async function (value) {
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
                searchSelection(vm.filter.students, vm.search.student, student);
            };

            vm.searchAudience = async function (value) {
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
                searchSelection(vm.filter.audiences, vm.search.audience, audience);
            };

            vm.removeSelection = function (type, value) {
                vm.filter[type] = _.without(vm.filter[type], _.findWhere(vm.filter[type], value));
                vm.resetIndicator();
            };

            vm.launchResearch = async function () {
                let users = [];
                let audiences = [];
                vm.filter.students.map(student => users.push(student.id));
                vm.filter.audiences.map(audience => audiences.push(audience.id));
                vm.filter.show = false;
                vm.filter.from = DateUtils.setFirstTime(vm.filter.from);
                vm.filter.to = DateUtils.setLastTime(vm.filter.to);
                template.open('indicator', `indicator/${vm.indicator.name()}`);
                await vm.indicator.search(vm.filter.from, vm.filter.to, users, audiences);
                vm.safeApply();
            }

            vm.resetIndicator = function () {
                vm.indicator.page = 0;
                vm.indicator.resetValues();
                vm.indicator.setFilterTypes(vm.filter.filterTypes);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                vm.launchResearch();
            }

            vm.openFilter = function () {
                vm.filter.filterTypes = vm.indicator.cloneFilterTypes();
                vm.filter.show = true;
            }

            vm.export = function () {
                let users = [];
                let audiences = [];
                vm.filter.students.map(student => users.push(student.id));
                vm.filter.audiences.map(audience => audiences.push(audience.id));
                vm.indicator.export(vm.filter.from, vm.filter.to, users, audiences);
            }

            async function init() {
                await vm.loadData();
                buildIndicators();
                vm.filter.filterTypes = vm.indicator.cloneFilterTypes();
                if (!vm.indicator !== undefined) vm.launchResearch();
            }

            $scope.$watch(() => window.structure, init);
            template.open('main', 'main');
            template.open('filter', 'filter');
            $scope.idiom = idiom;
        }]);
