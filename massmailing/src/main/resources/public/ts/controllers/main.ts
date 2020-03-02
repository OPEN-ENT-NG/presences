import {idiom, ng, template} from 'entcore';

interface ViewModel {
    homeState(): boolean

    historyState(): boolean
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

        route({
            home: () => template.open('content', 'home'),
            history: () => template.open('content', `history/history`)
        });
    }]);