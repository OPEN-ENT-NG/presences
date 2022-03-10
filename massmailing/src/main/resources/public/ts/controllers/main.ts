import {idiom, model, ng, template} from 'entcore';
import rights from "../rights";

interface ViewModel {
    homeState(): boolean;

    historyState(): boolean;

    hasRight(right: string): boolean;

    redirectTo(path: string): void;
}

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location',
    function ($scope, route, $location) {
        $scope.lang = idiom;
        template.open('main', 'main');

        const vm: ViewModel = this;

        vm.homeState = function () {
            return $location.path() === '/'
        };

        vm.historyState = function () {
            return $location.path() === '/history'
        };

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

       $scope.hasRight = (right: string): boolean => {
            return model.me.hasWorkflow(rights.workflow[right]);
        };

        $scope.redirectTo = (path: string): void => {
            $location.path(path);
        };

        route({
            home: () => template.open('content', 'home/home'),
            history: () => template.open('content', `history/history`)
        });
    }]);