import {Me, model, ng, routes} from 'entcore';
import rights from './rights';
import * as controllers from './controllers';
import * as directives from './directives';
import * as services from './services';
import {DependencyManager} from '@common/manager';
import {PreferencesUtils} from "@common/utils";
import {IStructure} from "@common/model";

declare let window: any;

new DependencyManager().load();
Me.preference(PreferencesUtils.PREFERENCE_KEYS.PRESENCE_STRUCTURE)
    .then((structure: IStructure) => window.structure = structure);

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
        .when('/events', {
            action: 'events'
        })
        .when('/planned-absences', {
            action: 'planned-absences'
        })
        .when('/collective-absences', {
            action: 'collective-absences'
        })
        .otherwise({
            redirectTo: '/dashboard'
        });

    if (model.me.hasWorkflow(rights.workflow.widget_alerts)) {
        $routeProvider.when('/alerts', {
            action: 'alerts'
        });
    }

    if (model.me.hasWorkflow(rights.workflow.readRegister)) {
        $routeProvider
            .when('/registers', {
                action: 'registers'
            })
            .when('/registers/:id', {
                action: 'getRegister'
            });
    }

    if (model.me.hasWorkflow(rights.workflow.readRegistry)) {
        $routeProvider
            .when('/registry', {
                action: 'registry'
            });
    }

    if (model.me.hasWorkflow(rights.workflow.search)) {
        $routeProvider
            .when('/registry', {
                action: 'registry'
            });
    }

    if (model.me.hasWorkflow(rights.workflow.viewCalendar)) {
        $routeProvider
            .when('/calendar/:studentId', {
                action: 'calendar'
            });
    }

    if (model.me.hasWorkflow(rights.workflow.readExemption)) {
        $routeProvider.when('/exemptions', {
            action: 'exemptions'
        });
    }

    if (model.me.hasWorkflow(rights.workflow.readPresences) || model.me.hasWorkflow(rights.workflow.readPresencesRestricted)) {
        $routeProvider
            .when('/presences', {
                action: 'presences'
            });
    }

    if (model.me.hasWorkflow(rights.workflow.manageStatementAbsences) || model.me.hasWorkflow(rights.workflow.manageStatementAbsencesRestricted)) {
        $routeProvider
            .when('/statements-absences', {
                action: 'statements-absences'
            });
    }
});