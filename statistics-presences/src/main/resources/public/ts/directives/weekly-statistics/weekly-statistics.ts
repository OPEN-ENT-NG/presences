import {ng} from 'entcore';
import {ROOTS} from "../../core/constants/roots";
import {Weekly} from "@statistics/indicator/Weekly";
import {IScope} from "angular";

declare let window: any;

interface IViewModel {
    $onInit(): any;

    $onDestroy(): any;

    indicator: Weekly;

}

class Controller implements ng.IController, IViewModel {
    indicator: Weekly;

    constructor(private $scope: IScope) {
        this.$scope['vm'] = this;
    }

    $onDestroy(): any {}

    $onInit(): any {}
}

export const weeklyStatistics = ng.directive('weeklyStatistics', () => {
    return {
        restrict: 'E',
        scope: {
            indicator: '='
        },
        templateUrl: `${ROOTS.directive}/weekly-statistics/weekly-statistics.html`,
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', Controller]
    };
});