import {_, idiom as lang, ng} from 'entcore';
import {Reason, ReasonService} from '@presences/services/ReasonService';
import {GroupService, SearchService} from '../services';
import {MassmailingAnomaliesResponse, MassmailingStatusResponse} from "../model";

interface Filter {
    show: boolean
    start_date: Date
    end_date: Date
    start_at: number
    status: {
        JUSTIFIED: boolean
        UNJUSTIFIED: boolean
        LATENESS: boolean
    },
    massmailing_status: {
        mailed: boolean,
        waiting: boolean
    },
    allReasons: boolean,
    reasons: any,
    noReasons: boolean,
    student: any,
    students: any[],
    group: any,
    groups: any[],
    selected: {
        students: any[],
        groups: any[]
    },
    anomalies: {
        MAIL: boolean
    }
}

interface ViewModel {
    filter: Filter
    formFilter: any;
    reasons: Array<Reason>
    massmailingStatus: MassmailingStatusResponse
    massmailingAnomalies: MassmailingAnomaliesResponse[]
    massmailingCount: {
        MAIL?: number
    }
    errors: {
        TYPE: boolean,
        REASONS: boolean,
        STATUS: boolean
    }

    fetchData(): void;

    loadData(): Promise<void>;

    switchAllReasons(): void;

    getActivatedReasonsCount(): number;

    searchStudent(value: string): void;

    selectStudent(model: any, teacher: any): void;

    selectGroup(model: any, classObject: any): void;

    searchGroup(value: string): Promise<void>;

    dropFilter(object, list): void;

    openForm(): void;

    validForm(): void;

    getKeys(object): string[]

    switchToJustifiedAbsences(): void;

    switchToUnjustifiedAbsences(): void;

    filterInError(): boolean;

    getErrorMessage(): string;
}

declare let window: any;

export const homeController = ng.controller('HomeController', ['$scope', 'route', 'MassmailingService', 'ReasonService',
    'SearchService', 'GroupService',
    function ($scope, route, MassmailingService, ReasonService: ReasonService, SearchService: SearchService, GroupService: GroupService) {
        const vm: ViewModel = this;
        vm.massmailingStatus = {};
        vm.errors = {
            TYPE: false,
            STATUS: false,
            REASONS: false
        };

        vm.filter = {
            show: false,
            start_date: new Date(),
            end_date: new Date(),
            start_at: 1,
            status: {
                JUSTIFIED: false,
                UNJUSTIFIED: true,
                LATENESS: false
            },
            massmailing_status: {
                mailed: false,
                waiting: true
            },
            allReasons: true,
            noReasons: true,
            reasons: {},
            students: undefined,
            student: "",
            group: "",
            groups: undefined,
            selected: {
                students: [],
                groups: []
            },
            anomalies: {
                MAIL: true
            }
        };

        vm.massmailingCount = {
            MAIL: 0
        };

        vm.fetchData = function () {
            fetchMassmailingAnomalies();
            fetchMassmailingStatus();
        };

        vm.openForm = function () {
            vm.filter.show = true;
            vm.formFilter = JSON.parse(JSON.stringify(vm.filter));
            vm.formFilter.selected.students.forEach((item) => item.toString = () => item.displayName);
            vm.formFilter.selected.groups.forEach((obj) => obj.toString = () => obj.name);
        };

        vm.validForm = function () {
            const {start_date, end_date} = vm.filter;
            vm.filter = {...vm.formFilter, show: false, start_date, end_date};
            vm.formFilter = {};
            vm.fetchData();
        };

        vm.loadData = async function () {
            if (!window.structure) return;
            vm.reasons = await ReasonService.getReasons(window.structure.id);
            vm.reasons.map((reason: Reason) => vm.filter.reasons[reason.id] = !reason.proving);
            vm.fetchData();
            $scope.$apply();
        };

        vm.switchAllReasons = function () {
            vm.formFilter.allReasons = !vm.formFilter.allReasons;
            vm.formFilter.noReasons = vm.formFilter.allReasons;
            for (let reason in vm.formFilter.reasons) {
                vm.formFilter.reasons[reason] = vm.formFilter.allReasons;
            }
        };


        vm.getActivatedReasonsCount = function () {
            let count = 0;
            for (let reason in vm.formFilter.reasons) {
                count += vm.formFilter.reasons[reason];
            }

            return count;
        };

        // If mailed AND waiting, return null. Empty massmailed parameter = non parameter filter.
        function massmailedParameter(): boolean {
            const {waiting, mailed} = vm.filter.massmailing_status;
            if (mailed && waiting) {
                return null;
            }

            return mailed;
        }

        async function retrieveMassmailings(type: string): Promise<any> {
            const {waiting, mailed} = vm.filter.massmailing_status;
            if (!checkFilter()) {
                return type === 'Status' ? {} : [];
            }

            const reasons: Array<Number> = [];
            const types: Array<String> = [];
            for (let i = 0; i < vm.reasons.length; i++) if (vm.filter.reasons[vm.reasons[i].id]) reasons.push(vm.reasons[i].id);
            for (let status in vm.filter.status) if (vm.filter.status[status]) types.push(status);
            const students = [], groups = [];
            vm.filter.selected.students.forEach(({id}) => students.push(id));
            vm.filter.selected.groups.forEach(({id}) => groups.push(id));
            const data = await MassmailingService[`get${type}`](window.structure.id, massmailedParameter(), reasons, vm.filter.start_at, vm.filter.start_date, vm.filter.end_date, groups, students, types, vm.filter.noReasons);
            return data;
        }

        function fetchMassmailingStatus() {
            retrieveMassmailings('Status').then(data => {
                vm.massmailingStatus = data;
                $scope.safeApply();
            });
        }

        function fetchMassmailingAnomalies() {
            retrieveMassmailings('Anomalies').then(data => {
                vm.massmailingAnomalies = data;
                resetMassmailingCount();
                data.forEach(({bug}) => {
                    Object.keys(bug).forEach(key => {
                        vm.massmailingCount[key]++
                    });
                });
                $scope.safeApply();
            });
        }

        function resetMassmailingCount() {
            vm.massmailingCount = {
                MAIL: 0
            }
        }

        vm.searchStudent = async function (value) {
            const structureId = window.structure.id;
            try {
                vm.formFilter.students = await SearchService.searchUser(structureId, value, 'Student');
                $scope.safeApply();
            } catch (err) {
                vm.formFilter.students = [];
                throw err;
            }
        };

        vm.selectStudent = async function (model, student) {
            if (_.findWhere(vm.filter.selected.students, {id: student.id})) {
                return;
            }
            vm.formFilter.selected.students.push(student);
            vm.formFilter.student = '';
            vm.formFilter.students = undefined;
            $scope.safeApply();
        };

        vm.searchGroup = async function (value) {
            const structureId = window.structure.id;
            try {
                vm.formFilter.groups = await GroupService.search(structureId, value);
                vm.formFilter.groups.map((obj) => obj.toString = () => obj.name);
                $scope.safeApply();
            } catch (err) {
                vm.formFilter.groups = [];
                throw err;
            }
            return;
        };

        vm.selectGroup = async function (model, group) {
            if (_.findWhere(vm.filter.selected.groups, {id: group.id})) {
                return;
            }
            vm.formFilter.selected.groups.push(group);
            vm.formFilter.group = '';
            vm.formFilter.groups = undefined;
            $scope.safeApply();
        };

        vm.dropFilter = function (object, list) {
            vm.formFilter.selected[list] = _.without(vm.formFilter.selected[list], object);
            $scope.safeApply();
        };

        vm.getKeys = (object) => Object.keys(object);

        vm.switchToJustifiedAbsences = function () {
            vm.formFilter.status.JUSTIFIED = !vm.formFilter.status.JUSTIFIED;
            vm.reasons.forEach(function (reason: Reason) {
                if (reason.proving) vm.formFilter.reasons[reason.id] = vm.formFilter.status.JUSTIFIED;
            });
        };

        vm.switchToUnjustifiedAbsences = function () {
            vm.formFilter.status.UNJUSTIFIED = !vm.formFilter.status.UNJUSTIFIED;
            vm.formFilter.noReasons = vm.formFilter.status.UNJUSTIFIED;
            vm.reasons.forEach(function (reason: Reason) {
                if (!reason.proving) vm.formFilter.reasons[reason.id] = vm.formFilter.status.UNJUSTIFIED;
            });
        };

        function checkFilter() {
            function allIsFalse(map): boolean {
                let bool = false;
                const keys = Object.keys(map);
                keys.forEach((key) => bool = bool || map[key]);

                return !bool;
            }

            const {noReasons, massmailing_status, status, reasons} = vm.filter;
            const reasonCheck = allIsFalse(reasons) && !noReasons;
            const massmailingStatusCheck = allIsFalse(massmailing_status);
            const statusCheck = allIsFalse(status);

            vm.errors = {
                REASONS: reasonCheck,
                STATUS: massmailingStatusCheck,
                TYPE: statusCheck
            };


            return !(reasonCheck || massmailingStatusCheck || statusCheck);
        }

        vm.filterInError = function () {
            let inError = false;
            const errorTypes = Object.keys(vm.errors);
            errorTypes.forEach((type) => inError = inError || vm.errors[type]);

            return inError;
        };

        vm.getErrorMessage = function () {
            let message = `[`;
            const types = Object.keys(vm.errors);
            types.forEach(type => {
                if (vm.errors[type]) message += `${lang.translate(`massmailing.filter.${type}`)}/`;
            });
            message = `${message.substr(0, message.length - 1)}]`;
            return message
        };

        $scope.$watch(() => window.structure, vm.loadData);
    }]);
