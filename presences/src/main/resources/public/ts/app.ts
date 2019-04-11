import {ng, routes} from 'entcore';
import * as controllers from './controllers';

for(let controller in controllers){
    ng.controllers.push(controllers[controller]);
}

routes.define(function($routeProvider){
	$routeProvider
        .when('/dashboard', {
            action: 'dashboard'
        })
        .when('/registers', {
            action: 'registers'
        })
        .when('/absences', {
            action: 'absences'
        })
        .when('/group-absences', {
            action: 'group-absences'
        })
        .when('/permissions', {
            action: 'permissions'
        })
		.otherwise({
            redirectTo: '/dashboard'
		});
});