import {idiom as lang, ng} from "entcore";
import {ILocationService, IParseService, IScope, IWindowService} from "angular";
import {ROOTS} from "../../core/enum/roots";
import {IInitFormDay, InitForm} from "../../models/init-form.model";
import {safeApply} from "@common/utils";
import {initService} from "../../services";
import {INIT_TYPE} from "../../core/enum/init-type";

declare let window: any;

interface IViewModel extends ng.IController, IInitLightboxProps {
    form: InitForm;
    lang: typeof lang;

    setDay(day: IInitFormDay, isFullDay: boolean);

    submitInit(): Promise<void>;

    closeForm(): void;
}

interface IInitLightboxProps {
    display: boolean;
}

interface IInitLightboxScope extends IScope, IInitLightboxProps {
    vm: IViewModel;
}

class Controller implements IViewModel {

    display: boolean;

    form: InitForm;

    lang: typeof lang;

    constructor(private $scope: IInitLightboxScope,
                private $location: ILocationService,
                private $window: IWindowService) {
        this.form = new InitForm();
    }

    $onInit() {
        this.lang = lang;
    }

    closeForm = (): void => {
        this.display = false;
    }

    $onDestroy() {
    }

    submitInit = async (): Promise<void> => {
        await initService.initViesco(window.structure.id, INIT_TYPE.ONE_D, this.form);
    }

    setDay = (day: IInitFormDay, isFullDay: boolean): void => {
        if (isFullDay) {
            this.form.timetable.setFullDay(day);
        } else {
            this.form.timetable.setHalfDay(day);
        }
        safeApply(this.$scope);
    }

}

function directive($parse: IParseService) {
    return {
        restrict: 'E',
        templateUrl: `${ROOTS.directive}init-lightbox/init-lightbox.html`,
        scope: {
            display: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$location', '$window', '$parse', Controller],
        /* interaction DOM/element */
        link: function ($scope: IInitLightboxScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {


        }
    }
}

export const initLightbox = ng.directive('initLightbox', directive);