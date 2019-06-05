import {ng, routes} from 'entcore';
import * as controllers from './controllers';
import * as directives from './directives';
import * as services from './services';

for (let directive in directives) {
    ng.directives.push(directives[directive]);
}

for (let controller in controllers) {
    ng.controllers.push(controllers[controller]);
}

for (let service in services) {
    ng.services.push(services[service]);
}

routes.define(function ($routeProvider) {
    $routeProvider
        .when('/incidents', {
            action: 'incidents'
        })
        .otherwise({
            redirectTo: '/incidents'
        });
})