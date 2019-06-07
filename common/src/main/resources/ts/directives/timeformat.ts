import {ng} from 'entcore';

export const timeformat = ng.directive('ngModel', function () {
    return {
        require: '?ngModel',
        link: function (scope, elem, attr, ngModel) {
            if (!ngModel)
                return;
            if (attr.type !== 'time')
                return;

            ngModel.$formatters.unshift(function (value) {
                return value.replace(/:00\.000$/, '')
            });
        }
    }
});
