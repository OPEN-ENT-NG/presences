import {ng} from 'entcore';
import {Indicator} from "../indicator";
import {DISPLAY_TYPE} from "../core/constants/DisplayMode";

type DisplayMode = {
    class: string;
    display: string;
    selected?: boolean;
};

interface IViewModel {
    $onInit(): any;

    $onDestroy(): any;

    displayMode: Array<DisplayMode>;
    indicator: Indicator;
    
    switchDisplay(mode: DisplayMode): void;
}

export const displayStatisticsMode = ng.directive('displayStatisticsMode', () => {
    return {
        restrict: 'E',
        scope: {
            onChange: '&'
        },
        template: `
            <div class="displayStatisticsMode cell">
                <i ng-repeat="mode in vm.displayMode track by $index" 
                   ng-class="{selected: mode.selected}" 
                   ng-click="vm.switchDisplay(mode)"
                   class="[[mode.class]]">
                </i>
            </div>
        `,
        controllerAs: 'vm',
        bindToController: {
            indicator: "="
        },
        controller: function ($scope) {
            const vm: IViewModel = <IViewModel>this;

            vm.$onInit = () => {
                vm.displayMode = [
                    {display: DISPLAY_TYPE.TABLE, class: 'presences-table', selected: true}, // default
                    {display: DISPLAY_TYPE.GRAPH, class: 'statistics-presences', selected: false},
                ];
            };

        },
        link: function ($scope) {
            const vm: IViewModel = $scope.vm;

            vm.switchDisplay = (mode: DisplayMode): void => {
                if (!mode.selected) {
                    mode.selected = !mode.selected;
                    vm.displayMode.filter((d: DisplayMode) => d !== mode).forEach((d: DisplayMode) => d.selected = false);
                    vm.indicator.display = mode.display;
                    $scope.$parent.$eval($scope.onChange);
                }
            };
        }
    };
});