import {idiom as lang, ng} from "entcore";
import {ILocationService, IParseService, IScope, IWindowService} from "angular";
import {ROOTS} from "../../core/enum/roots";
import {IInitFormDay, InitForm} from "../../models/init-form.model";
import {safeApply} from "@common/utils";
import {IInitTeachersResponse, initService} from "../../services";
import {INIT_TYPE} from "../../core/enum/init-type";

declare let window: any;

interface IViewModel extends ng.IController, IInitLightboxProps {
    form: InitForm;
    lang: typeof lang;

    teachers: Array<{ id: string, displayName: string }>;
    numberOfTeachers: number;

    setDay(day: IInitFormDay, isFullDay: boolean);

    submitInit(): Promise<void>;

    initViesco(): Promise<void>;

    fetchTeachers(): Promise<void>;

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
    displayTeachers: boolean;

    teachers: Array<{ id: string, displayName: string }>;
    numberOfTeachers: number;

    form: InitForm;

    lang: typeof lang;

    constructor(private $scope: IInitLightboxScope,
                private $location: ILocationService,
                private $window: IWindowService) {
        this.form = new InitForm();
        this.displayTeachers = false;
    }

    $onInit() {
        this.lang = lang;
        this.fetchTeachers();
    }

    closeForm = (): void => {
        this.display = false;
    }

    $onDestroy() {
    }

    submitInit = async (): Promise<void> => {
        if (this.numberOfTeachers > 0) {
            this.displayTeachers = true;
            return;
        }
        await this.initViesco();
    }

    initViesco = async (): Promise<void> => {
        await initService.initViesco(window.structure.id, INIT_TYPE.ONE_D, this.form);
        this.closeForm();
        safeApply(this.$scope);
    }

    fetchTeachers = async (): Promise<void> => {
        try {
            initService.getTeachersInitializationStatus(window.structure.id).then((res: IInitTeachersResponse) => {
                this.teachers = res.teachers;
                this.numberOfTeachers = this.teachers.length;
                safeApply(this.$scope);
            });
        } catch (error) {
            console.error("Error fetching teachers:", error);
        }
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