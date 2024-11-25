import {ng} from "entcore";
import {ILocationService, IParseService, IScope, IWindowService} from "angular";
import {ROOTS} from "../../core/enum/roots";

declare let window: any;

interface IViewModel extends ng.IController, IInitTeachersCheckLightboxProps {
    structureId: string;
    submitInit?(): Promise<void>;

    closeForm(): void;
}

interface IInitTeachersCheckLightboxProps {
    display: boolean;

    onSubmit?;

    teachers: Array<{ id: string, displayName: string }>;
    numberOfTeachers: number;
}

interface IInitTeachersCheckLightboxScope extends IScope, IInitTeachersCheckLightboxProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    display: boolean;
    teachers: any[] = [];
    numberOfTeachers: number = 0;
    structureId: string;

    constructor(private $scope: IInitTeachersCheckLightboxScope,
                private $location: ILocationService,
                private $window: IWindowService) {
        this.structureId = window.structure.id;
    }

    $onInit() {
    }

    closeForm = (): void => {
        this.display = false;
    }
}

function directive($parse: IParseService): ng.IDirective {
    return {
        restrict: 'E',
        templateUrl: `${ROOTS.directive}init-teachers-check-lightbox/init-teachers-check-lightbox.html`,
        scope: {
            display: '=',
            teachers: '=',
            numberOfTeachers: '=',
            onSubmit: '&'
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$location', '$window', '$parse', Controller],
        /* interaction DOM/element */
        link: function ($scope: IInitTeachersCheckLightboxScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {

            vm.submitInit = async (): Promise<void> => {
                $parse($scope.vm.onSubmit())({});
                vm.closeForm();
            }

        }
    }
}

export const initTeachersCheckLightbox = ng.directive('initTeachersCheckLightbox', directive);
