import {ng} from 'entcore';

interface ViewModel {

}


export const alertsController = ng.controller('AlertsController', ['$scope', 'route', ($scope, route) => {
    console.log('AlertsController');
    const vm: ViewModel = this;

}]);