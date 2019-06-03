import {ng} from 'entcore';

export const StaticInclude = ng.directive('staticInclude', function ($http, $templateCache, $compile) {
    return function (scope, element, attrs) {
        let templatePath = attrs.staticInclude;
        $http.get(templatePath, {cache: $templateCache}).success(function (response) {
            let contents = element.html(response).contents();
            $compile(contents)(scope);
        });
    };
});