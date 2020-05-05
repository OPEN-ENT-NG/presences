import {model, ng} from 'entcore';
import {IPBlameField, IPunishment, IPunishmentBody} from "@incidents/models";

interface IViewModel {
    form: IPunishmentBody;
    punishment: IPunishment;
    owner: string;
}

export const PunishmentBlameForm = ng.directive('punishmentBlameForm', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            form: '=',
            punishment: '='
        },
        template: `
            <div>
                 <i18n>presences.responsible</i18n>&nbsp;
                 <span class="font-bold">[[vm.owner]]</span>
            </div>
        `,
        controllerAs: 'vm',
        bindToController: true,
        replace: true,
        controller: function () {
            const vm: IViewModel = <IViewModel>this;
            if (!vm.punishment.id) {
                vm.form.owner_id = model.me.userId;
                vm.owner = model.me.username;
                vm.form.fields = {} as IPBlameField;
            } else {
                vm.form.owner_id = vm.punishment.owner.id;
                vm.owner = vm.punishment.owner.displayName;
                vm.form.fields = {} as IPBlameField;
            }
            console.log("Blame: ", vm);
        },
        link: function ($scope, $element: HTMLDivElement) {
            const vm: IViewModel = $scope.vm;

            $scope.$on('$destroy', () => {
                vm.form = {} as IPunishmentBody;
                vm.owner = "";
            });
        }
    };
});