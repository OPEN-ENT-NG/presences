import {_, ng, toasts} from 'entcore';
import {alertService} from "../services";
import {AlertType} from "../models";

declare let window: any;

interface Filter {
    absences: boolean;
    lateness: boolean;
    forgottenNotebook: boolean;
    incidents: boolean;
}

interface ViewModel {
    alertType: string[];
    filter: Filter;

    getStudentAlert(): void;

    /*  switch event type */
    switchAbsencesFilter(): void;

    switchLatenessFilter(): void;

    switchforgottenNotebookFilter(): void;

    switchIncidentFilter(): void;

    updateFilter(): void;
}

export const alertsController = ng.controller('AlertsController', ['$scope', '$route',
    function($scope, $route) {
        console.log('AlertsController');
        const vm: ViewModel = this;

        vm.filter = {
            absences: true,
            lateness: true,
            forgottenNotebook: true,
            incidents: true,
        };

        vm.alertType = [];

        const initData = async () => {
            await vm.getStudentAlert();
        };

        vm.getStudentAlert = async () => {
            if (vm.filter.absences) {
                if (!vm.alertType.some(e => e == AlertType[AlertType.ABSENCE])) {
                    vm.alertType.push(AlertType[AlertType.ABSENCE]);
                }
            } else {
                vm.alertType = _.without(vm.alertType, AlertType[AlertType.ABSENCE]);
            }
            if (vm.filter.lateness) {
                if (!vm.alertType.some(e => e == AlertType[AlertType.LATENESS])) {
                    vm.alertType.push(AlertType[AlertType.LATENESS]);
                }
            }
            else {
                vm.alertType = _.without(vm.alertType, AlertType[AlertType.LATENESS]);
            }
            if (vm.filter.forgottenNotebook) {
                if (!vm.alertType.some(e => e == AlertType[AlertType.FORGOTTEN_NOTEBOOK])) {
                    vm.alertType.push(AlertType[AlertType.FORGOTTEN_NOTEBOOK]);
                }
            }
            else {
                vm.alertType = _.without(vm.alertType, AlertType[AlertType.FORGOTTEN_NOTEBOOK]);
            }
            if (vm.filter.incidents) {
                if (!vm.alertType.some(e => e == AlertType[AlertType.INCIDENT])) {
                    vm.alertType.push(AlertType[AlertType.INCIDENT]);
                }
            }
            else {
                vm.alertType = _.without(vm.alertType, AlertType[AlertType.INCIDENT]);
            }
            try {
                let studentsAlerts: any = await alertService.getStudentsAlerts(window.structure.id, vm.alertType);
            } catch (e) {
                toasts.warning('error');
                throw e;
            }
        };

        /* ----------------------------
           Switch type methods
          ---------------------------- */

        vm.switchAbsencesFilter = async function () {
            vm.filter.absences = !vm.filter.absences;
            await vm.getStudentAlert()
        };

        vm.switchLatenessFilter = async function () {
            vm.filter.lateness = !vm.filter.lateness;
            await vm.getStudentAlert()
        };

        vm.switchforgottenNotebookFilter = async function () {
            vm.filter.forgottenNotebook = !vm.filter.forgottenNotebook;
            await vm.getStudentAlert()
        };

        vm.switchIncidentFilter = async function () {
            vm.filter.incidents = !vm.filter.incidents;
            await vm.getStudentAlert()
        };


        $scope.$watch(() => window.structure, () => {
            if ($route.current.action === "alerts") {
                initData();
            } else {
                $scope.redirectTo('/alerts');
            }
        });
    }]);