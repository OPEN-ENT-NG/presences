import {Behaviours, idiom, ng, template} from 'entcore';

/**
	Wrapper controller
	------------------
	Main controller.
**/
export const mainController = ng.controller('MainController', ['$scope', 'route', '$rootScope', '$route',
	($scope, route, $rootScope, $route) => {
    idiom.addBundle('/presences/i18n');
    Behaviours.load('presences').then($scope.$apply);
	const openContainer = () => template.open('main', `containers/${$route.current.action}`);
	$rootScope.$on("$routeChangeSuccess", openContainer);

	route({
		incidents: () => {
		}
	})

}]);
