import {idiom, ng, template} from 'entcore';

/**
	Wrapper controller
	------------------
	Main controller.
**/
export const mainController = ng.controller('MainController', ['$scope', 'route', '$rootScope', '$route', ($scope, route, $rootScope, $route) => {
    idiom.addBundle('/incidents/i18n');
    const openContainer = () => template.open('main', `containers/${$route.current.action}`);
    $rootScope.$on("$routeChangeSuccess", openContainer);

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
        exemptions: () => {
        }
    })
}]);
