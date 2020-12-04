import {model, moment, ng} from 'entcore';
import {IPExcludeField, IPunishment, IPunishmentBody} from "@incidents/models";
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
    start_date: string;
    end_date: string;

    formatStartDate(): void;

    formatEndDate(): void;

    searchOwner(value: string): Promise<void>;

    selectOwner(model, owner: User): void;

    getDisplayOwnerName(): string;
}

export const PunishmentExcludeForm = ng.directive('punishmentExcludeForm', ['SearchService',
    (SearchService: SearchService) => {
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
            <label class="twelve cell twelve-mobile">
                <div class="two cell twelve-mobile">
                    <i18n>presences.responsible</i18n>:
                </div>
                <div class="seven cell twelve-mobile">
                    <div class="incident-lightbox-body-responsible-autocomplete search-input">
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
                    vm.start_date = moment().startOf('day');
                    vm.end_date = moment().startOf('day');
                    vm.form.fields = {
                        start_date: DateUtils.format(vm.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                        end_date: DateUtils.format(vm.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
                        mandatory_presence: false,
                    } as IPExcludeField;
                    vm.owner = model.me;
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
                    vm.owner = vm.punishment.owner;
                }
            },
            link: function ($scope, $element: HTMLDivElement) {
                const vm: IViewModel = $scope.vm;
                vm.usersSearch = new UsersSearch(window.structure.id, SearchService);

                vm.formatStartDate = (): void => {
                    (<IPExcludeField>vm.form.fields).start_at =
                        DateUtils.format(vm.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
                };

                vm.formatEndDate = (): void => {
                    (<IPExcludeField>vm.form.fields).end_at =
                        DateUtils.format(vm.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
                };

                vm.getDisplayOwnerName = (): string => {
                    return vm.owner.displayName || vm.owner.lastName + " " + vm.owner.firstName;
                };

                vm.searchOwner = async (value: string): Promise<void> => {
                    await vm.usersSearch.searchUsers(value);
                    $scope.$apply();
                };

                vm.selectOwner = function (model, owner: User): void {
                    vm.owner = owner;
                    vm.form.owner_id = owner.id;
                    vm.ownerSearch = '';
                    $scope.$apply();
                };

                $scope.$watch(() => vm.start_date, () => vm.formatStartDate());
                $scope.$watch(() => vm.end_date, () => vm.formatEndDate());

                $scope.$on('$destroy', () => {
                    vm.form = {} as IPunishmentBody;
                    vm.owner = null;
                    vm.ownerSearch = '';
                });
            }
        };
    }]);