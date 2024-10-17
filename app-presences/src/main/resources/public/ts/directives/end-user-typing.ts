import {ng} from 'entcore';

export const EndUserTyping = ng.directive('endUserTyping', () => {
    return {
        restrict: 'A',
        link: function ($scope, $element, $attrs) {
            let userTimeout;

            const endUserTyping = function () {
                $scope.$eval($attrs.endUserTyping)
            };

            $element.on('keydown', function () {
                clearTimeout(userTimeout);
            });

            $element.on('keyup', function () {
                clearTimeout(userTimeout);
                userTimeout = setTimeout(endUserTyping, 750);
            });

        }
    };
});