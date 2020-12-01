import {ng} from 'entcore';
import {Student} from "@common/model/Student";
import {UPDATE_STUDENTS_EVENTS} from "@common/core/enum/select-children-events";
import {IStructure} from "@common/model";

interface IViewModel {

    child: Student;
    children: Array<Student>;

    structure: IStructure;

    selectChild();

    selectStructure();
}

export const SelectChildren = ng.directive('selectChildren', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            child: '=',
            children: '='
        },
        template: `

        <div class="selected-child">
            <!-- select children section -->
            <div ng-if="vm.children && vm.children.length > 1" class="selected-child-select">
                <label>
                    <select class="card" required
                            data-ng-model="vm.child"
                            data-ng-change="vm.selectChild()"
                            data-ng-options="child as child.displayName for child in vm.children">
                    </select>
                </label>
            </div>
            
            <!-- select structure section -->
            <div ng-if="vm.child.structures && vm.child.structures.length > 1" class="selected-child-select">
                <label>
                    <select class="card" required
                            data-ng-model="vm.child.structure"
                            data-ng-change="vm.selectStructure()"
                            data-ng-options="structure as structure.name for structure in vm.child.structures">
                    </select>
                </label>
            </div>
        </div>
        `,
        controllerAs: 'vm',
        bindToController: true,
        replace: false,
        controller: async function () {
            const vm: IViewModel = <IViewModel>this;
        },
        link: function ($scope) {
            const vm: IViewModel = $scope.vm;

            vm.selectChild = (): void => {
                vm.child.structure = vm.child.structures[0];
                $scope.$emit(UPDATE_STUDENTS_EVENTS.UPDATE, vm.child);
            }

            vm.selectStructure = (): void => {
                $scope.$emit(UPDATE_STUDENTS_EVENTS.UPDATE, vm.child);
            }
        }
    };
});