import {ng} from 'entcore';

declare const window: any;

export const StudentName = ng.directive('studentName', () => {
    return {
        restrict: 'E',
        scope: {
            id: '=',
            name: '='
        },
        template: `
            <span ng-click="openMemento(id)">[[name]]</span>
        `,
        link: function ($scope, element) {
            $scope.openMemento = function () {
                if (window.memento) {
                    window.memento.load($scope.id);
                }
            }
        }
    };
});
