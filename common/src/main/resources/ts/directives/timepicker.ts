import {ng} from 'entcore';

export * from '@common/directives/timeformat';

export const TimePicker = ng.directive('timePicker', () => {
    return {
        restrict: 'E',
        scope: {
            ngModel: '=',
            ngChange: '&?',
            ngDisabled: '=?'
        },
        template: `
            <label class="time-picker">
               <input type="time" data-ng-model="ngModel" ng-disabled="ngDisabled" ng-change="onChange()" step="60" pattern="[0-9]{2}:[0-9]{2}">
            </label>
        `,
        link: function ($scope, $element, $attrs) {
            $scope.onChange = function () {
                setTimeout(function () {
                    if ($attrs.ngChange) $scope.$parent.$eval($attrs.ngChange);
                }, 0);
            }
        }
    };
});