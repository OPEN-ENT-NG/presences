import {ng, routes} from 'entcore';
import * as controllers from './controllers';
import {DependencyManager} from '@common/manager'

new DependencyManager().load();

for (let controller in controllers) {
    ng.controllers.push(controllers[controller]);
}

routes.define(function ($routeProvider) {
    $routeProvider
        .otherwise({
            action: 'defaultView'
        });
});