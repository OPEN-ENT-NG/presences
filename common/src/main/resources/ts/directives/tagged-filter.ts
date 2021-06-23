import {idiom as lang, ng} from 'entcore';

interface IViewModel {
    $onInit(): any;

    $onDestroy(): any;

    labels: Array<string>;

    getText(text: string): string;
}

export const taggedFilter = ng.directive('taggedFilter', () => {
    return {
        restrict: 'E',
        scope: {
            labels: '<*'
        },
        template: `
            <div class="row">
                <div class="cell" ng-repeat="label in vm.labels track by $index">
                    <div class="card horizontal-margin-small horizontal-spacing">
                        <span>[[vm.getText(label)]]</span>
                    </div>
                </div>
            </div>
        `,
        controllerAs: 'vm',
        bindToController: true,
        transclude: true,
        controller: function () {
            const vm: IViewModel = <IViewModel>this;

            vm.$onInit = () => {
            };

            vm.getText = (text: string): string => {
                return lang.translate(text);
            };
        }
    };
});