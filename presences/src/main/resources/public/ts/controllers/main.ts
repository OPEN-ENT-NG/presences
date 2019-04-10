import {ng, template} from 'entcore';

/**
	Wrapper controller
	------------------
	Main controller.
**/
export const mainController = ng.controller('MainController', ['$scope', 'route', '$rootScope', '$route', ($scope, route, $rootScope, $route) => {
    const openContainer = () => template.open('main', `containers/${$route.current.action}`);
    $rootScope.$on("$routeChangeSuccess", openContainer);

    $scope.getCurrentState = () => $route.current.action;

    route({
        dashboard: () => {
        },
        registers: () => {
        },
        absences: () => {
        },
        'group-absences': () => {
        },
        permissions: () => {
        },
        incidents: () => {
        },
        punishments: () => {
        },
        sanctions: () => {
        },
        'mass-mailings': () => {
        }
    })
}]);
