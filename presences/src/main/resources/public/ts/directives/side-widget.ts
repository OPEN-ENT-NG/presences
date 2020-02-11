import {idiom as lang, ng} from 'entcore';
import {COURSE_EVENTS} from "@common/model";

export const SideWidget = ng.directive('sideWidget', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            name: '=',
            course: '=',
            opened: '=?'
        },
        template: `
        <aside class="side-widget" ng-class="{opened: opened}">
            <div class="title">
                <h2 title="[[::translatedTitle]]" >
                    <span class="triangle open-button" ng-click="opened = !opened" role="button">&nbsp;</span>
                    [[::translatedTitle]]
                    
                    <!-- see current register -->
                    <span class="course" ngModel="course" ng-show="opened && hasCourse()" ng-click="openRegister()">
                         [[::seeMoreText]]
                    </span>
                </h2>
            </div>
            <div class="content">
                <div ng-transclude/>
            </div>
        </aside>
        `,
        controller: function ($scope) {
            $scope.translatedTitle = lang.translate($scope.name);
            $scope.seeMoreText = lang.translate('presences.see.more');
            $scope.opened = $scope.opened || false;

            $scope.hasCourse = (): boolean => {
                return $scope.course != undefined && Object.keys($scope.course).length !== 0
            };

            $scope.openRegister = (): void => {
                $scope.$broadcast(COURSE_EVENTS.OPEN_REGISTER, $scope.course);
            };
        }
    };
});