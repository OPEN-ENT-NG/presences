import {idiom as lang, model, ng} from 'entcore';
import {COURSE_EVENTS} from "@common/model";
import {ROOTS} from "../../core/enum/roots";

export const SideWidget = ng.directive('sideWidget', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            name: '=',
            course: '=',
            opened: '=?'
        },
        templateUrl: `${ROOTS.directive}side-widget/side-widget.html`,
        controller: function ($scope) {
            $scope.translatedTitle = lang.translate($scope.name);
            $scope.seeMoreText = lang.translate('presences.see.more');
            $scope.opened = $scope.opened || true;
            const sidePanelStateKey: string = $scope.name + ".open." + model.me.userId;


            $scope.hasCourse = (): boolean => {
                return $scope.course != undefined && Object.keys($scope.course).length !== 0
            };

            $scope.openRegister = (): void => {
                $scope.$broadcast(COURSE_EVENTS.OPEN_REGISTER, $scope.course);
            };

            $scope.init = (): void => {
                let sidePanelStateValue: boolean = JSON.parse(sessionStorage.getItem(sidePanelStateKey));
                if ((sidePanelStateValue !== undefined) && (sidePanelStateValue !== null)) {
                    $scope.opened = sidePanelStateValue;
                }
            }

            $scope.changePanelState = () => {
                $scope.opened = !$scope.opened;
                sessionStorage.setItem(sidePanelStateKey, JSON.stringify($scope.opened));
            }
        },
        link: ($scope) => {
            $scope.init();
        }
    };
});