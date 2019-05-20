
import {ng} from 'entcore';

export const Panel = ng.directive('panel', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            opened: '='
        },
        template: `
        <div class="content" ng-class="{opened: opened}">
            <div ng-transclude/>
        </div>
        `,
    };
});