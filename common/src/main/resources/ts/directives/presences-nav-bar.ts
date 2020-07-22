import {ng} from 'entcore';
import {IRouting} from "@common/model/Route";
import {ROUTING_EVENTS} from "@common/core/enum/routing-keys";

interface IViewModel {
    title: string;
    routingKeys: Array<IRouting>;

    switchRoute(routerKeys: string): void;
}

/**
 * Nav sub menu component
 */
export const PresencesNavBar = ng.directive('presencesNavBar', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            mobileMode: '=', // if no specified, will do desktop/mobile else set true to display only MOBILE
            routingKeys: '=' // add routing data to fill nav (will communicate using EMIT)
        },
        template: `
        <div class="presences-nav-bar">
           <nav class="tabs" data-ng-class="{'zero-desktop': vm.mobileMode}" side-nav>
                <header class="horizontal-spacing row ellipsis" 
                        data-ng-repeat="routing in vm.routingKeys"
                        data-ng-click="vm.switchRoute(routing.key)"
                        data-ng-class="{ selected: routing.isSelected }">
                        <span>[[routing.label]]</span>
                </header>
            </nav>
        </div>
        `,
        controllerAs: 'vm',
        bindToController: true,
        replace: true,
        controller: function () {
            const vm: IViewModel = <IViewModel>this;
        },
        link: function ($scope, $element: HTMLDivElement) {
            const vm: IViewModel = $scope.vm;

            vm.switchRoute = (routerKeys: string) => {
                // reset all selected to false
                vm.routingKeys.map((router: IRouting) => router.isSelected = false);

                // set current router selected to true
                vm.routingKeys.find((router: IRouting) => router.key === routerKeys).isSelected = true;

                // emitting current router
                $scope.$emit(ROUTING_EVENTS.SWITCH, routerKeys);
            };
        }
    };
});