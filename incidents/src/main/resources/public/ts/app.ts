import { ng, routes } from 'entcore';
import * as controllers from './controllers';

for(let controller in controllers){
    ng.controllers.push(controllers[controller]);
}

routes.define(function($routeProvider){
	$routeProvider
		.otherwise({
			action: 'defaultView'
		});
})