import {model, ng, routes} from 'entcore';
import rights from './rights'
import * as controllers from './controllers';
import * as directives from './directives';
import * as services from './services';
import {DependencyManager} from '@common/manager'

new DependencyManager().load();

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

    if (model.me.hasWorkflow(rights.workflow.readPunishment) || model.me.hasWorkflow(rights.workflow.readSanction)) {
        $routeProvider.when('/punishment/sanction', {action: 'punishment'})
    }
});