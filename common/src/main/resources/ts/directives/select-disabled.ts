import {angular, ng} from 'entcore';

export const optionsDisabled = ng.directive('optionsDisabled', function ($parse) {
    let disableOptions = function (scope, attr, element, data, fnDisableIfTrue) {
        // refresh the disabled options in the select element.
        let options = element.find('option');
        let index = 0;
        for (let pos = 0; pos < options.length; pos++) {
            let elem = angular.element(options[pos]);
            if (elem.val() != "" && data) {
                let locals = {};
                locals[attr] = data[index];
                elem.attr('disabled', fnDisableIfTrue(scope, locals));
                index++;
            }
        }
    };
    return {
        priority: 0,
        require: 'ngModel',
        link: function (scope, el, attrs, ctrl) {
            // parse expression and build array of disabled options
            let expElements = attrs.optionsDisabled.match(/^\s*(.+)\s+for\s+(.+)\s+in\s+(.+)?\s*/);
            let attrToWatch = expElements[3];
            let fnDisableIfTrue = $parse(expElements[1]);

            scope.$watch(attrToWatch, function (newValue, oldValue) {
                if (newValue) {
                    disableOptions(scope, expElements[2], el, newValue, fnDisableIfTrue);
                }
            }, true);

            // handle model updates properly
            scope.$watch(attrs.ngModel, function (newValue, oldValue) {
                let disOptions = $parse(attrToWatch)(scope);
                if (newValue) {
                    disableOptions(scope, expElements[2], el, disOptions, fnDisableIfTrue);
                }
            });
        }
    };
});
