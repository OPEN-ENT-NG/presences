import {ng} from "@presences/models/__mocks__/entcore"
import * as angular from 'angular';
import 'angular-mocks';
import {punishmentController} from "@incidents/controllers";
import {GroupService, searchService, SearchService, ViescolaireService} from "@common/services";
import {punishmentService, punishmentsTypeService} from "@incidents/services";
import {IPunishmentType} from "@incidents/models/PunishmentType";

describe('PunishmentsControllers', () => {
    let punishmentControllerTest: any;

    beforeEach(() => {
        //Registering a module for testing
        const testApp = angular.module('app', []);
        let $controller, $rootScope;

        //Mockup test module
        angular.mock.module('app');

        //Instantiation of the services, controllers, directives we need
        punishmentController;
        searchService;

        //Adding services, controllers, directives in the module
        ng.initMockedModules(testApp);

        //Controller Injection
        angular.mock.inject(function (_$controller_, _$rootScope_) {
            // The injector unwraps the underscores (_) from around the parameter names when matching
            $controller = _$controller_;
            $rootScope = _$rootScope_;
        });

        //Creates a new instance of scope
        let $scope = $rootScope.$new();

        //Fetching $location
        testApp.run(function($rootScope, $location) {
            $rootScope.location = $location;
        });

        //Controller Recovery
        punishmentControllerTest = $controller('PunishmentController', {
            $scope: $scope,
            $route: undefined,
            $location: $rootScope.location,
            searchService: SearchService,
            groupService: GroupService,
            punishmentService: punishmentService,
            punishmentTypeService: punishmentsTypeService,
            viescolaireService: ViescolaireService
        });
    })

    it('test the correct functioning the getSelectedPunishmentType method', done => {
        expect(punishmentControllerTest.getSelectedPunishmentType()).toEqual(0);
        const punishmentTypeSelected: IPunishmentType = {
            hidden: false,
            id: 0,
            isSelected: true,
            label: "",
            punishment_category: undefined,
            punishment_category_id: 0,
            structure_id: "",
            type: "",
            used: false
        }
        const punishmentTypeNotSelected: IPunishmentType = {
            hidden: false,
            id: 0,
            isSelected: false,
            label: "",
            punishment_category: undefined,
            punishment_category_id: 0,
            structure_id: "",
            type: "",
            used: false
        }
        punishmentControllerTest.punishmentsTypes = [punishmentTypeSelected, punishmentTypeNotSelected];
        expect(punishmentControllerTest.getSelectedPunishmentType()).toEqual(1);
        punishmentTypeNotSelected.isSelected = true;
        expect(punishmentControllerTest.getSelectedPunishmentType()).toEqual(2);
        done();
    });
});
