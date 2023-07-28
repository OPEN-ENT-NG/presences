import {moment, ng} from "entcore";
import {ILocationService, IParseService, IScope, IWindowService} from "angular";
import {DateUtils, safeApply} from "@common/utils";

interface IViewModel extends ng.IController, IDateRangePickerProps {
    updateDate?(changedDate: Date): void;
}

interface IDateRangePickerProps {
    startDate: Date;
    endDate: Date;

    onUpdate?(): string;
}

interface IDateRangePickerScope extends IScope, IDateRangePickerProps {
    vm: IViewModel;
}

class Controller implements IViewModel {

    startDate: Date;
    endDate: Date;

    constructor(private $scope: IDateRangePickerScope,
                private $location: ILocationService,
                private $window: IWindowService) {
    }

    $onInit() {
    }

    $onDestroy() {
    }

}

function directive($parse: IParseService) {
    return {
        restrict: 'E',
        template: `
           <div class="date-range-picker cell eight top5">
               <i18n>presences.from</i18n>&#58;
                <span class="card date-picker">
                    <date-picker ng-change="vm.updateDate(vm.startDate);" ng-model="vm.startDate"></date-picker>
                </span>
                <i18n>presences.to</i18n>&#58;
                <span class="card date-picker">
                    <date-picker ng-change="vm.updateDate(vm.endDate);" ng-model="vm.endDate"></date-picker>
                </span>
            </div>
        `,
        scope: {
            onUpdate: '&',
            startDate: '=',
            endDate: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$location', '$window', '$parse', Controller],
        /* interaction DOM/element */
        link: function ($scope: IDateRangePickerScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {

            vm.updateDate = (changedDate: Date) => {

                let start: string = moment(vm.startDate).format(DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC']);
                let end: string = moment(vm.endDate).format(DateUtils.FORMAT['YEAR-MONTH-DAY-HOUR-MIN-SEC']);

                if (changedDate && !DateUtils.isPeriodValid(start, end)) {
                    vm.startDate = changedDate;
                    vm.endDate = changedDate;
                }
                $parse($scope.vm.onUpdate())({
                    startDate: vm.startDate,
                    endDate: vm.endDate
                })
            }
        }
    }
}

export const dateRangePicker = ng.directive('dateRangePicker', directive);
