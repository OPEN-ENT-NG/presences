import {Behaviours, idiom, ng} from 'entcore';

/**
	Wrapper controller
	------------------
	Main controller.
**/
export const mainController = ng.controller('MainController', ['$scope', 'route', ($scope, route) => {
    idiom.addBundle('/presences/i18n');
    Behaviours.load('presences').then($scope.$apply);
}]);
