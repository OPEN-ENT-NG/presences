import {idiom, model, ng, template} from 'entcore';
import rights from '../rights'
import {Idiom, Template} from '@common/interfaces'

export interface Scope {
    lang: Idiom;
    template: Template;

    safeApply(fn?: () => void): void;

    hasSearchRight(): boolean;

    hasRight(right: string): boolean;

    redirectTo(path: string): void;

    $watch(value: any, trigger: (oldVal: any, newVal: any) => void): void;
}

/**
 Wrapper controller
 ------------------
 Main controller.
 **/
export const mainController = ng.controller('MainController', ['$scope', 'route', '$rootScope', '$route', '$location',
    ($scope: Scope, route, $rootScope, $route, $location) => {
        idiom.addBundle('/incidents/i18n');

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
            absences: () => {
                template.open('main', `containers/absences`);
            },
            'group-absences': () => {
                template.open('main', `containers/group-absences`);
            },
            exemptions: () => {
                template.open('main', `containers/exemptions`);
            },
            calendar: () => template.open('main', 'containers/calendar')
        });

        $scope.lang = idiom;
        $scope.template = template;

        $scope.safeApply = function (fn?) {
            const phase = this.$root.$$phase;
            if (phase == '$apply' || phase == '$digest') {
                if (fn && (typeof (fn) === 'function')) {
                    fn();
                }
            } else {
                this.$apply(fn);
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
    }]);
