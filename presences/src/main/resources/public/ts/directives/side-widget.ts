import {idiom as lang, ng} from 'entcore';

export const SideWidget = ng.directive('sideWidget', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            title: '=',
            opened: '=?'
        },
        template: `
        <aside class="side-widget" ng-class="{opened: opened}">
            <div class="title">
                <h2>
                    <span class="triangle open-button" ng-click="opened = !opened" role="button">&nbsp;</span>
                    [[::translatedTitle]]
                </h2>
            </div>
            <div class="content">
                <div ng-transclude/>
            </div>
        </aside>
        `,
        controller: function ($scope) {
            $scope.translatedTitle = lang.translate($scope.title);
            $scope.opened = $scope.opened || false;
        }
    };
});