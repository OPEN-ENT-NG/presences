import {ng} from 'entcore';
import {User, UserService} from "../services";

declare let window: any;

interface Filter {
    student: string;
    students: User[];
}

interface ViewModel {
    filter: Filter;

    selectStudent(model: any, student: any): void;

    searchStudent(value: string): void;
}

export const dashboardController = ng.controller('DashboardController', ['$scope', 'route', '$location', 'UserService',
    function ($scope, route, $location, UserService: UserService) {
        const vm: ViewModel = this;
        vm.filter = {
            students: null,
            student: null
        };

        vm.selectStudent = function (model, student) {
            console.log('selected student: ', student);
            $location.path(`/calendar/${student.id}`);
        };

        vm.searchStudent = async function (value) {
            const structureId = window.structure.id;
            try {
                vm.filter.students = await UserService.search(structureId, value, 'Student');
                $scope.safeApply();
            } catch (err) {
                vm.filter.students = [];
            }
        }
    }]);