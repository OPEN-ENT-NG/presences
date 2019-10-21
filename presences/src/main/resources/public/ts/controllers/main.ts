import {idiom, model, ng, template} from 'entcore';
import rights from '../rights'
import {Idiom, Template} from '@common/interfaces'
import {IRootScopeService} from "angular";

declare let window: any;

export interface Scope extends IRootScopeService {
    lang: Idiom;
    template: Template;
    structure: { id: string, name: string };
    
    safeApply(fn?: () => void): void;

    hasSearchRight(): boolean;

    hasRight(right: string): boolean;

    redirectTo(path: string): void;
}

/**
 Wrapper controller
 ------------------
 Main controller.
 **/
export const mainController = ng.controller('MainController', ['$scope', 'route', '$rootScope', '$route', '$location',
    ($scope: Scope, route, $rootScope, $route, $location) => {
        idiom.addBundle('/incidents/i18n');
        $scope.structure = {
            id: '',
            name: ''
        };

        route({
            dashboard: () => {
                template.open('main', `containers/dashboard`);
            },
            registers: () => {
                template.open('main', `containers/registers`);
            },
            getRegister: () => {
                template.open('main', `containers/registers`);
            },
            registry: () => {
                if (!window.item) $location.path('/');
                template.open('main', `containers/registry`);
            },
            events: () => {
                template.open('main', `containers/events`);
            },
            'group-absences': () => {
                template.open('main', `containers/group-absences`);
            },
            exemptions: () => {
                template.open('main', `containers/exemptions`);
            },
            calendar: () => {
                if (!window.item) $location.path('/');
                template.open('main', 'containers/calendar')
            }
        });

        $scope.lang = idiom;
        $scope.template = template;

        $scope.safeApply = function (fn?) {
            const phase = $scope.$root.$$phase;
            if (phase == '$apply' || phase == '$digest') {
                if (fn && (typeof (fn) === 'function')) {
                    fn();
                }
            } else {
                $scope.$apply(fn);
            }
        };

        $scope.hasSearchRight = function () {
            return model.me.hasWorkflow(rights.workflow.search);
        };

        $scope.hasRight = function (right) {
            return model.me.hasWorkflow(rights.workflow[right]);
        };

        $scope.redirectTo = (path: string) => {
            $location.path(path);
        };

        $scope.$watch(() => window.structure, () => $scope.structure = window.structure);
    }]);
