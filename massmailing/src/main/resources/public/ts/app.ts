import {Me, ng, routes} from 'entcore';
import * as controllers from './controllers';
import * as services from './services';
import * as directives from './directives';
import {DependencyManager} from '@common/manager'
import {PreferencesUtils} from "@common/utils";
import {IStructure} from "@common/model";

declare let window: any;

new DependencyManager().load();
Me.preference(PreferencesUtils.PREFERENCE_KEYS.PRESENCE_STRUCTURE)
    .then((structure: IStructure) => window.structure = structure);

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