import {idiom, model, ng, template} from 'entcore';
import rights from '../rights'
import {Idiom, Template} from '@common/interfaces'
import {IRootScopeService} from "angular";
import {UserUtils} from "@common/utils";

declare let window: any;

export interface Scope extends IRootScopeService {
    lang: Idiom;
    template: Template;
    structure: { id: string, name: string };

    safeApply(fn?: () => void): void;

    hasSearchRight(): boolean;

    hasRight(right: string): boolean;

    redirectTo(path: string): void;

    isRelative(): boolean;

    isChild(): boolean;
}

/**
 Wrapper controller
 ------------------
 Main controller.
 **/
export const mainController = ng.controller('MainController', ['$scope', 'route', '$rootScope', '$route', '$location',
    ($scope: Scope, route, $rootScope, $route, $location) => {
        $scope.structure = {
            id: '',
            name: ''
        };

        route({
            dashboard: () => {
                /* Handle redirect URL as child/relative user */
                if ($scope.isChild() || $scope.isRelative()) {
                    template.open('main', `containers/dashboard-student`);
                } else {
                    template.open('main', `containers/dashboard`);
                }
            },
            registers: () => {
                /* Handle redirect URL as child/relative user */
                if ($scope.isChild() || $scope.isRelative()) {
                    template.open('main', `containers/dashboard-student`);
                } else {
                    template.open('main', `containers/registers`);
                }
            },
            getRegister: () => {
                /* Handle redirect URL as child/relative user */
                if ($scope.isChild() || $scope.isRelative()) {
                    template.open('main', `containers/dashboard-student`);
                } else {
                    template.open('main', `containers/registers`);
                }
            },
            presences: () => {
                /* Handle redirect URL as child/relative user */
                if ($scope.isChild() || $scope.isRelative()) {
                    template.open('main', `containers/dashboard-student`);
                } else {
                    template.open('main', `containers/presences`);
                }
            },
            registry: () => {
                /* Handle redirect URL as child/relative user */
                if ($scope.isChild() || $scope.isRelative()) {
                    template.open('main', `containers/dashboard-student`);
                } else {
                    if (!window.item) $location.path('/');
                    template.open('main', `containers/registry`);
                }
            },
            events: () => {
                /* Handle redirect URL as child/relative user */
                if ($scope.isChild() || $scope.isRelative()) {
                    template.open('main', `containers/dashboard-student`);
                } else {
                    template.open('main', `containers/events`);
                }
            },
            alerts: () => {
                /* Handle redirect URL as child/relative user */
                if ($scope.isChild() || $scope.isRelative()) {
                    template.open('main', `containers/dashboard-student`);
                } else {
                    template.open('main', `containers/alerts`);
                }
            },
            'group-absences': () => {
                /* Handle redirect URL as child/relative user */
                if ($scope.isChild() || $scope.isRelative()) {
                    template.open('main', `containers/dashboard-student`);
                } else {
                    template.open('main', `containers/group-absences`);
                }
            },
            exemptions: () => {
                /* Handle redirect URL as child/relative user */
                if ($scope.isChild() || $scope.isRelative()) {
                    template.open('main', `containers/dashboard-student`);
                } else {
                    template.open('main', `containers/exemptions`);
                }
            },
            calendar: () => {
                /* Handle redirect URL as child/relative user */
                if ($scope.isChild() || $scope.isRelative()) {
                    template.open('main', `containers/dashboard-student`);
                } else {
                    if (!window.item) $location.path('/');
                    template.open('main', 'containers/calendar')
                }
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

        $scope.hasRight = function (right: string) {
            return model.me.hasWorkflow(rights.workflow[right]);
        };

        $scope.isRelative = (): boolean => {
            return UserUtils.isRelative(model.me.type)
        };

        $scope.isChild = (): boolean => {
            return UserUtils.isChild(model.me.type);
        };

        $scope.redirectTo = (path: string) => {
            $location.path(path);
        };

        $scope.$watch(() => window.structure, () => $scope.structure = window.structure);
    }]);
