import {model, moment, ng} from 'entcore';
import {IPDutyField, IPunishment, IPunishmentBody} from "@incidents/models";
import {DateUtils} from "@common/utils";

interface IViewModel {
    form: IPunishmentBody;
    punishment: IPunishment;
    owner: string;
    date: string;

    formatDate(): void;
}

export const PunishmentDutyForm = ng.directive('punishmentDutyForm', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            form: '=',
            punishment: '='
        },
        template: `
        <div class="punishment-duty-form">
             <!-- Initial Date -->
             <div class="punishment-duty-form-date">
                <i18n>incidents.for.the</i18n> &#58;&nbsp;
                <span class="card date-picker">
                    <date-picker required ng-model="vm.date"></date-picker>
                </span>
             </div>
           
            
            <!-- instruction -->
            <div class="punishment-duty-form-instruction">
                <i18n>incidents.instruction</i18n>&nbsp;
                <textarea i18n-placeholder="incidents.write.text" data-ng-model="vm.form.fields.instruction"></textarea>
            </div>
            
            <!-- responsible -->
            <div>
                 <i18n>presences.responsible</i18n>&nbsp;
                 <span class="font-bold">[[vm.owner]]</span>
            </div>
        </div>
        `,
        controllerAs: 'vm',
        bindToController: true,
        replace: true,
        controller: function () {
            const vm: IViewModel = <IViewModel>this;
            if (!vm.punishment.id) {
                vm.form.owner_id = model.me.userId;
                vm.date = moment().startOf('day');
                vm.form.fields = {
                    delay_at: DateUtils.format(vm.date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                    instruction: ""
                } as IPDutyField;
                vm.owner = model.me.lastName + " " + model.me.firstName;
            } else {
                vm.form.owner_id = vm.punishment.owner.id;
                vm.form.fields = vm.punishment.fields;
                if (!(Object.keys(vm.form.fields).length > 0)) {
                    vm.form.fields = {
                        delay_at: DateUtils.format(vm.date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                        instruction: ""
                    } as IPDutyField;
                }
                vm.date = moment((<IPDutyField>vm.form.fields).delay_at);
                vm.owner = vm.punishment.owner.displayName;
            }
        },
        link: function ($scope, $element: HTMLDivElement) {
            const vm: IViewModel = $scope.vm;

            vm.formatDate = (): void => {
                (<IPDutyField>vm.form.fields).delay_at = DateUtils.format(vm.date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
            };

            $scope.$watch(() => vm.date, () => vm.formatDate());
        }
    };
});