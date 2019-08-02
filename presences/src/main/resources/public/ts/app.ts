import {model, ng, routes} from 'entcore';
import rights from './rights'
import * as controllers from './controllers';
import * as directives from './directives';
import * as services from './services';

for (let controller in controllers) {
    ng.controllers.push(controllers[controller]);
}

for (let directive in directives) {
    ng.directives.push(directives[directive]);
}

for (let service in services) {
    ng.services.push(services[service]);
}

routes.define(function ($routeProvider) {
    $routeProvider
        .when('/dashboard', {
            action: 'dashboard'
        })
        .when('/absences', {
            action: 'absences'
        })
        .when('/group-absences', {
            action: 'group-absences'
        })
        .otherwise({
            redirectTo: '/dashboard'
        });


    if (model.me.hasWorkflow(rights.workflow.readRegister)) {
        $routeProvider
            .when('/registers', {
                action: 'registers'
            })
            .when('/registers/:id', {
                action: "getRegister"
            })
    }

    if (model.me.hasWorkflow(rights.workflow.search)) {
        $routeProvider
            .when('/calendar/:studentId', {
                action: 'calendar'
            })
    }

    if (model.me.hasWorkflow(rights.workflow.readExemption)) {
        $routeProvider.when('/exemptions', {
            action: 'exemptions'
        });
    }
});