import {ng} from 'entcore';

interface Scope {
    menu: { hovered: string, active: string, timeout: number };

    hoverIn(menuItem: string): void;

    hoverOut(): void;

    getCurrentState(): string;

    $apply();
}

export const navigationController = ng.controller('NavigationController', ['$scope', '$rootScope', '$route', '$timeout', ($scope: Scope, $rootScope, $route, $timeout) => {
    $scope.menu = {
        hovered: '',
        active: '',
        timeout: null
    };

    $scope.hoverIn = (menuItem) => {
        $scope.menu.hovered = menuItem;
        $timeout.cancel($scope.menu.timeout);
    };

    $scope.hoverOut = () => {
        $scope.menu.timeout = $timeout(() => {
            $scope.menu.hovered = '';
        }, 250);
    };

    $scope.getCurrentState = () => $route.current.action;
}]);