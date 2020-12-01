import {model, moment, ng} from 'entcore';
import {IPDutyField, IPunishment, IPunishmentBody} from "@incidents/models";
import {DateUtils, UsersSearch} from "@common/utils";
import {User} from "@common/model/User";
import {SearchService} from "@common/services";

declare let window: any;

interface IViewModel {
    form: IPunishmentBody;
    punishment: IPunishment;
    usersSearch: UsersSearch;
    owner: User;
    ownerSearch: string;
    date: string;

    formatDate(): void;

    searchOwner(value: string): Promise<void>;

    selectOwner(model, owner: User): void;

    getDisplayOwnerName(): string;
}

export const PunishmentDutyForm = ng.directive('punishmentDutyForm', ['SearchService',
    (SearchService: SearchService) => {
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
            <label>
                <i18n>presences.responsible</i18n>
                <div class="search-input">
                    <async-autocomplete data-ng-disabled="false"
                                        data-ng-model="vm.ownerSearch"
                                        data-ng-change="vm.selectOwner"
                                        data-on-search="vm.searchOwner"
                                        data-options="vm.usersSearch.users"
                                        data-placeholder="incidents.search.personal"
                                        data-search="vm.ownerSearch">
                    </async-autocomplete>
                </div>
                <div ng-show="vm.owner" class="margin-top-sm">
                    <span class="font-bold">[[vm.getDisplayOwnerName()]]</span>
                </div>
            </label>
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
                    vm.owner = model.me;
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
                    vm.owner = vm.punishment.owner;
                }
            },
            link: function ($scope, $element: HTMLDivElement) {
                const vm: IViewModel = $scope.vm;
                vm.usersSearch = new UsersSearch(window.structure.id, SearchService);

                vm.formatDate = (): void => {
                    (<IPDutyField>vm.form.fields).delay_at = DateUtils.format(vm.date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
                };

                vm.getDisplayOwnerName = (): string => {
                    return vm.owner.displayName || vm.owner.lastName + " " + vm.owner.firstName;
                };

                vm.searchOwner = async (value: string): Promise<void> => {
                    await vm.usersSearch.searchUsers(value);
                    $scope.$apply();
                };

                vm.selectOwner = (model, owner: User): void => {
                    vm.owner = owner;
                    vm.form.owner_id = owner.id;
                    vm.ownerSearch = '';
                    $scope.$apply();
                };

                $scope.$watch(() => vm.date, () => vm.formatDate());

                $scope.$on('$destroy', () => {
                    vm.form = {} as IPunishmentBody;
                    vm.owner = null;
                    vm.ownerSearch = '';
                });
            }
        };
    }]);