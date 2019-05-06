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

    $scope.lang = idiom;
    $scope.template = template;

    $scope.safeApply = function (fn?) {
        var phase = this.$root.$$phase;
        if (phase == '$apply' || phase == '$digest') {
            if (fn && (typeof (fn) === 'function')) {
                fn();
            }
        } else {
            this.$apply(fn);
        }
    };

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
