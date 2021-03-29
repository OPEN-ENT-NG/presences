import {model, ng} from 'entcore';
import {IPBlameField, IPunishment, IPunishmentBody} from "@incidents/models";
import {SearchService} from "@common/services";
import {User} from "@common/model/User";
import {UsersSearch} from "@common/utils";

declare let window: any;

interface IViewModel {
    $onInit(): any;

    $onDestroy(): any;

    form: IPunishmentBody;
    punishment: IPunishment;
    usersSearch: UsersSearch;
    owner: User;
    ownerSearch: string;


    searchOwner(value: string): Promise<void>;

    selectOwner(model, owner: User): void;

    getDisplayOwnerName(): string;
}

export const PunishmentBlameForm = ng.directive('punishmentBlameForm', ['SearchService',
    (SearchService: SearchService) => {
        return {
            restrict: 'E',
            transclude: true,
            scope: {
                form: '=',
                punishment: '='
            },
            template: `
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
            `,
            controllerAs: 'vm',
            bindToController: true,
            replace: false,
            controller: function () {
                const vm: IViewModel = <IViewModel>this;
                vm.$onInit = () => {
                    vm.form = {} as IPunishmentBody;
                    if (!vm.punishment || !vm.punishment.id) {
                        vm.form.owner_id = model.me.userId;
                        vm.owner = model.me;
                        vm.form.fields = {} as IPBlameField;
                    } else {
                        vm.form.owner_id = vm.punishment.owner.id;
                        vm.owner = vm.punishment.owner;
                        vm.form.fields = {} as IPBlameField;
                    }
                };

            },
            link: function ($scope, $element: HTMLDivElement) {
                const vm: IViewModel = $scope.vm;
                vm.usersSearch = new UsersSearch(window.structure.id, SearchService);

                vm.getDisplayOwnerName = (): string => {
                    if (vm && vm.owner) {
                        return vm.owner.displayName || vm.owner.lastName + " " + vm.owner.firstName;
                    }
                    return "";
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

                vm.$onDestroy = () => {
                    vm.form = {} as IPunishmentBody;
                    vm.owner = null;
                    vm.ownerSearch = '';
                };
            }
        };
    }]);