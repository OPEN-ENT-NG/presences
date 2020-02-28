import {_, Me, model, ng, toasts} from 'entcore';
import {alertService} from "../services";
import {AlertType} from "../models";
import {SearchService} from "@common/services/SearchService";
import {GroupService} from "@common/services/GroupService";
import rights from "../rights";
import {Alert} from "@presences/models/Alert";

declare let window: any;

interface Filter {
    types: {
        ABSENCE?: boolean;
        LATENESS?: boolean;
        FORGOTTEN_NOTEBOOK?: boolean;
        INCIDENT?: boolean;
    };
    student: string;
    students: any[];
    class: string;
    classes: any[];
    selected: { students: any[], classes: any[] };
}

interface StudentAlert {
    "id": number,
    "student_id": string,
    "type": string,
    "count": number,
    "exceed_date": string,
    "name": string,
    "audience": string,
    selected?: boolean
}

interface ViewModel {
    alerts: Alert;
    filters: string[];
    alertType: string[];
    filter: Filter;
    listAlert: Array<StudentAlert>;
    selection: { all: boolean };

    params: {
        loading: boolean,
        type: string
    };

    getStudentAlert(students?: any[], classes?: string[]): void;

    searchStudent(value: string): void;

    selectStudent(model: any, student: any): void;

    searchClass(value: string): Promise<void>;

    selectClass(model: any, classObject: any): void;

    dropFilter(object, list): void;

    excludeClassFromFilter(audience): void;

    /*  switch alert type */
    switchFilter(filter: string): void;

    someSelectedAlert(): boolean;

    updateFilter(student?, audience?): void;

    selectAll(): void;

    reset(): void;
}

export const alertsController = ng.controller('AlertsController', ['$scope', '$route', '$location',
    'SearchService', 'GroupService',
    function ($scope, $route, $location, SearchService: SearchService, GroupService: GroupService) {
        console.log('AlertsController');
        const vm: ViewModel = this;
        vm.filters =
            [AlertType[AlertType.ABSENCE], AlertType[AlertType.LATENESS], AlertType[AlertType.INCIDENT], AlertType[AlertType.FORGOTTEN_NOTEBOOK]];
        vm.filter = {
            student: '',
            class: '',
            students: undefined,
            classes: undefined,
            selected: {
                students: [],
                classes: [],
            },
            types: {}
        };

        const initFilter = function (value: boolean) {
            vm.filters.forEach(filter => vm.filter.types[filter] = value);
        };

        initFilter(true);

        vm.alertType = [];
        vm.selection = {all: false};

        /* Fetching information from URL Param and cloning new object RegistryRequest */
        vm.params = Object.assign({loading: false}, $location.search());

        const initData = async () => {
            if (!window.structure) {
                window.structure = await Me.preference("presences.structure");
            } else {
                if (vm.params.type) {
                    initFilter(false);
                    vm.filter.types[vm.params.type] = true;
                }
                await vm.getStudentAlert();
            }
        };

        vm.getStudentAlert = async (student, classes) => {
            vm.params.loading = true;
            $scope.safeApply();
            vm.alertType = [];
            Object.keys(vm.filter.types).forEach(key => {
                if (vm.filter.types[key]) vm.alertType.push(key);
            });

            try {
                if (vm.alertType.length > 0) {
                    let studentsAlerts: any = await alertService.getStudentsAlerts(window.structure.id, vm.alertType, student, classes);
                    vm.listAlert = studentsAlerts;
                } else {
                    vm.listAlert = [];
                }
            } catch (e) {
                toasts.warning('presences.error.get.alert');
                throw e;
            }
            vm.params.loading = false;
            $scope.safeApply();
        };

        vm.searchStudent = async function (value) {
            const structureId = window.structure.id;
            try {
                vm.filter.students = await SearchService.searchUser(structureId, value, 'Student');
                $scope.safeApply();
            } catch (err) {
                vm.filter.students = [];
                throw err;
            }
        };

        vm.someSelectedAlert = function () {
            return vm.listAlert && vm.listAlert.filter(alert => alert.selected).length > 0;
        };

        vm.selectStudent = async function (model, student) {
            if (_.findWhere(vm.filter.selected.students, {id: student.id})) {
                return;
            }
            vm.filter.selected.students.push(student);
            vm.filter.student = '';
            vm.filter.students = undefined;
            await vm.getStudentAlert(extractSelectedStudentIds(), extractSelectedGroupsName());
            $scope.safeApply();
        };

        vm.searchClass = async function (value) {
            const structureId = window.structure.id;
            try {
                vm.filter.classes = await GroupService.search(structureId, value);
                vm.filter.classes.map((obj) => obj.toString = () => obj.name);
                $scope.safeApply();
            } catch (err) {
                vm.filter.classes = [];
                throw err;
            }
            return;
        };

        vm.selectClass = async function (model, classObject) {
            if (_.findWhere(vm.filter.selected.students, {id: classObject.id})) {
                return;
            }
            vm.filter.selected.classes.push(classObject);
            vm.filter.class = '';
            vm.filter.classes = undefined;
            await vm.getStudentAlert(extractSelectedStudentIds(), extractSelectedGroupsName());
            $scope.safeApply();
        };

        vm.dropFilter = function (object, list) {
            vm.filter.selected[list] = _.without(vm.filter.selected[list], object);
            vm.getStudentAlert(extractSelectedStudentIds(), extractSelectedGroupsName());
        };

        const extractSelectedStudentIds = function () {
            const ids = [];
            vm.filter.selected.students.map((student) => ids.push(student.id));
            return ids;
        };

        const extractSelectedGroupsName = function (): string[] {
            const ids = [];
            if (model.me.hasWorkflow(rights.workflow.search)) {
                vm.filter.selected.classes.map((group) => ids.push(group.id));
            }
            return ids;
        };

        /* ----------------------------
           Switch type methods
          ---------------------------- */
        vm.switchFilter = async function (type) {
            vm.filter.types[type] = !vm.filter.types[type];
            await vm.getStudentAlert();
        };

        vm.selectAll = function () {
            vm.listAlert.forEach(alert => alert.selected = vm.selection.all);
        };

        vm.reset = async function () {
            try {
                let alertsId = [];
                vm.listAlert.forEach(alert => {
                    if (alert.selected) alertsId.push(alert.id);
                });
                await alertService.reset(alertsId);
                await vm.getStudentAlert();
                toasts.confirm('presences.alert.reset.success');
            } catch (e) {
                toasts.warning('presences.error.reset.alert');
                throw e;
            }
        };

        $scope.$watch(() => window.structure, initData);
    }]);