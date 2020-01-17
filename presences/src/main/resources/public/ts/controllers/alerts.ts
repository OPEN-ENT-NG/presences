import {_, ng, toasts} from 'entcore';
import {Alert, alertService} from "../services";
import {AlertType, Student, Students} from "../models";
import {GroupService} from "@common/services/GroupService";

declare let window: any;

interface Filter {
    ABSENCE?: boolean;
    LATENESS?: boolean;
    FORGOTTEN_NOTEBOOK?: boolean;
    INCIDENT?: boolean;
    students: any;
    classes: any;
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
        type: string
    };

    getStudentAlert(): void;

    /*  switch event type */
    switchAbsencesFilter(): void;

    excludeClassFromFilter(audience): void;

    /*  switch alert type */
    switchFilter(filter: string): void;

    updateFilter(student?, audience?): void;

    selectAll(): void;

    reset(): void;
}

export const alertsController = ng.controller('AlertsController', ['$scope', '$route', '$location', 'GroupService',
    function ($scope, $route, $location, GroupService: GroupService) {
        console.log('AlertsController');
        const vm: ViewModel = this;
        vm.students = new Students();
        vm.filters =
            [AlertType[AlertType.ABSENCE], AlertType[AlertType.LATENESS], AlertType[AlertType.INCIDENT], AlertType[AlertType.FORGOTTEN_NOTEBOOK]];

        const initFilter = function (value: boolean) {
            vm.filter = {classes: [], students: []};
            vm.filters.forEach(filter => vm.filter[filter] = value);
        };

        initFilter(true);

        vm.alertType = [];
        vm.studentSearchInput = '';
        vm.classesSearchInput = '';
        vm.selection = {all: false};

        /* Fetching information from URL Param and cloning new object RegistryRequest */
        vm.params = Object.assign({}, $location.search());

        const setStudentToSync = () => {
            vm.alerts.userId = vm.filter.students ? vm.filter.students
                .map(students => students.id)
                .filter(function () {
                    return true
                })
                .toString() : '';
        };

        const setClassToSync = () => {
            vm.alerts.classes = vm.filter.classes ? vm.filter.classes
                .map(classes => classes.id)
                .filter(function () {
                    return true
                })
                .toString() : '';
        };

        const initData = async () => {
            if (vm.params.type) {
                initFilter(false);
                vm.filter[vm.params.type] = true;
            }
            await vm.getStudentAlert();
        };

        vm.getStudentAlert = async () => {
            vm.alertType = [];
            Object.keys(vm.filter).forEach(key => {
                if (vm.filter[key]) vm.alertType.push(key);
            });

            try {
                let studentsAlerts: any = await alertService.getStudentsAlerts(window.structure.id, vm.alertType);
                vm.listAlert = studentsAlerts;
            } catch (e) {
                toasts.warning('presences.error.get.alert');
                throw e;
            }
        };

        /* ----------------------------
           Switch type methods
          ---------------------------- */
        vm.switchFilter = async function (type) {
            vm.filter[type] = !vm.filter[type];
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
            } catch (e) {
                toasts.warning('presences.error.reset.alert');
                throw e;
            }
        };

        $scope.$watch(() => window.structure, initData);
    }]);