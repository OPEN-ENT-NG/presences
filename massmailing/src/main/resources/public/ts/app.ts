import {ng, routes} from 'entcore';
import * as controllers from './controllers';
import * as services from './services';
import * as directives from './directives';
import {DependencyManager} from '@common/manager'

new DependencyManager().load();

for (let controller in controllers) {
    ng.controllers.push(controllers[controller]);
}

for (let service in services) {
    ng.services.push(services[service]);
}

for (let directive in directives) {
    ng.directives.push(directives[directive]);
}

routes.define(function ($routeProvider) {
    $routeProvider
        .when('/', {
            action: 'home'
        })
        .when('/history', {
            action: 'history'
        })
        .otherwise({
            redirectTo: '/'
        });
});