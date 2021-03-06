import {idiom, ng, template} from 'entcore';
import {Idiom, Template} from '@common/interfaces'

import {IRootScopeService} from "angular";

export interface Scope extends IRootScopeService {
    lang: Idiom;
    template: Template;

    safeApply(fn?: () => void): void;
}

/**
 Wrapper controller
 ------------------
 Main controller.
 **/
export const mainController = ng.controller('MainController', ['$scope', 'route', '$rootScope', '$route',
    ($scope: Scope, route, $rootScope, $route) => {
        const openContainer = () => template.open('main', `containers/${$route.current.action}`);
        $rootScope.$on("$routeChangeSuccess", openContainer);

        route({
            incidents: () => {
            },
            punishment: () => {
                template.open('main', `containers/punishment`);
            },
        });

        $scope.lang = idiom;

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
    }]);
