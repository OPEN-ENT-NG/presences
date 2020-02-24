import {idiom, ng, template} from 'entcore';
import {Idiom, Template} from '@common/interfaces'

import {IRootScopeService} from "angular";
import {PreferencesUtils} from "@common/utils";

declare let window: any;

export interface Scope extends IRootScopeService {
    lang: Idiom;
    template: Template;
    structure: { id: string, name: string };

    safeApply(fn?: () => void): void;
}

/**
 Wrapper controller
 ------------------
 Main controller.
 **/
export const mainController = ng.controller('MainController', ['$scope', 'route', '$rootScope', '$route',
    async ($scope: Scope, route, $rootScope, $route) => {
        const openContainer = () => template.open('main', `containers/${$route.current.action}`);
        $rootScope.$on("$routeChangeSuccess", openContainer);

        route({
            incidents: () => {
            }
        });

        /**
         * Init user's preference by default
         */
        await PreferencesUtils.initPreference();

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

        $scope.$watch(() => window.structure, () => $scope.structure = window.structure);

    }]);
