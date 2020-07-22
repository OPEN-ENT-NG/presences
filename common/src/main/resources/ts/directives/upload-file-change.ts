import {ng} from 'entcore';

export const UploadFileOnChange = ng.directive('uploadFileOnChange', () => {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            var onChangeHandler = scope.$eval(attrs.uploadFileOnChange);
            element.on('change', onChangeHandler);
            element.bind('change', onChangeHandler);
            element.on('$destroy', function () {
                element.off();
            });
        }
    };
});