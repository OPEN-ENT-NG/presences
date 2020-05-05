import {model, moment, ng} from 'entcore';
import {IPExcludeField, IPunishment, IPunishmentBody} from "@incidents/models";
import {DateUtils} from "@common/utils";

interface IViewModel {
    form: IPunishmentBody;
    punishment: IPunishment;
    owner: string;
    start_date: string;
    end_date: string;

    formatStartDate(): void;

    formatEndDate(): void;
}

export const PunishmentExcludeForm = ng.directive('punishmentExcludeForm', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            form: '=',
            punishment: '='
        },
        template: `
         <div class="punishment-exclude-form">
             <!-- Date -->
             <div class="punishment-exclude-form-date twelve cell">
                <i18n>presences.from</i18n>&nbsp;&#58;&nbsp;
                <span class="card date-picker"><date-picker ng-model="vm.start_date"></date-picker></span>

                <i18n>presences.to</i18n>&nbsp;&#58;&nbsp;
                <span class="card date-picker"><date-picker ng-model="vm.end_date"></date-picker></span>
             </div>
           
            <!-- mandatory -->
            <div class="punishment-exclude-form-mandatory">
                <i18n>incidents.presences.mandatory.inside.structure</i18n>&nbsp;
                 <switch ng-model="vm.form.fields.mandatory_presence">
                    <label class="switch"></label>
                 </switch>
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
                vm.start_date = moment().startOf('day');
                vm.end_date = moment().startOf('day');
                vm.form.fields = {
                    start_date: DateUtils.format(vm.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                    end_date: DateUtils.format(vm.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                    mandatory_presence: false,
                } as IPExcludeField;
                vm.owner = model.me.username;
            } else {
                vm.form.owner_id = vm.punishment.owner.id;
                vm.form.fields = vm.punishment.fields;
                if (!(Object.keys(vm.form.fields).length > 0)) {
                    vm.form.fields = {
                        start_date: DateUtils.format(vm.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                        end_date: DateUtils.format(vm.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                        mandatory_presence: false,
                    } as IPExcludeField;
                }
                vm.start_date = moment((<IPExcludeField>vm.form.fields).start_at);
                vm.end_date = moment((<IPExcludeField>vm.form.fields).end_at);
                vm.owner = vm.punishment.owner.displayName;
            }
        },
        link: function ($scope, $element: HTMLDivElement) {
            const vm: IViewModel = $scope.vm;

            vm.formatStartDate = (): void => {
                (<IPExcludeField>vm.form.fields).start_at =
                    DateUtils.format(vm.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
            };

            vm.formatEndDate = (): void => {
                (<IPExcludeField>vm.form.fields).end_at =
                    DateUtils.format(vm.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
            };

            $scope.$watch(() => vm.start_date, () => vm.formatStartDate());
            $scope.$watch(() => vm.end_date, () => vm.formatEndDate());
        }
    };
});