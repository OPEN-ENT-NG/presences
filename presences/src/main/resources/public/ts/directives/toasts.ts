import {_, idiom, ng} from 'entcore';

interface Toast {
    text: string;
    status: string; // Should be : Confirm, Warning, Info
    show: boolean; // Internal property
    animate: boolean; // Internal property
}

export const Toasts = ng.directive('toasts', () => ({
    restrict: 'E',
    scope: {
        notifications: '='
    },
    template: `
        <div class="toasts">
            <toast toast="toast" ng-repeat="toast in notifications"></toast>
        </div>
    `,
    controller: ['$scope', '$timeout', ($scope, $timeout) => {
        $scope.deleteToast = (toast: Toast) => {
            $scope.notifications = _.without($scope.notifications, toast);
        };

        $scope.$on('toast.delete', (evt, toast: Toast) => $scope.deleteToast(toast));
    }]
}));

export const Toast = ng.directive('toast', ['$timeout', ($timeout) => ({
    restrict: 'E',
    require: '^toasts',
    scope: {
        toast: '='
    },
    template: `
        <div class="vertical-spacing horizontal-spacing [[toast.status]] toast-content"
            ng-class="{ show: toast.show }">
            <div class="content">
                [[translate(toast.text)]]
            </div>
            <div class="timer" ng-class="{ animation: toast.animate }"></div>
        </div>
    `,
    link: ($scope) => {
        const isValidToast = (toast: Toast) => (toast.hasOwnProperty('text')
            && toast.hasOwnProperty('status'));

        if (isValidToast($scope.toast)) {
            $timeout(() => {
                ($scope.toast as Toast).show = true;
                ($scope.toast as Toast).animate = true;
                $timeout(() => {
                    $scope.$emit('toast.delete', $scope.toast);
                }, 3000);
            }, 100);
        } else {
            $scope.$emit('toast.delete', $scope.toast);
            throw new TypeError('Invalid toast type. Must contains text and status');
        }

        $scope.translate = (key: string) => idiom.translate(key);
    }
})]);