import {ng} from "@presences/models/__mocks__/entcore";
import * as angular from 'angular';
import * as angularMock from 'angular-mocks/angular-mocks';
import {registryController} from "@presences/controllers";
import {GroupService, SearchService} from "@common/services";
import {RegistryEvent, registryService} from "@presences/services";

describe('RegistryController', () => {
    let registryControllerTest: any;

    let registryEventNotRegularized: RegistryEvent = {
        type: "ABSENCE",
        startDate: "2020-01-01 08:00:00",
        endDate: "2020-01-01 09:00:00",
        student_id: "1",
        counsellor_regularisation: false,
        followed: false
    };

    let registryEventRegularized: RegistryEvent = {
        type: "ABSENCE",
        startDate: "2020-01-01 08:00:00",
        endDate: "2020-01-01 09:00:00",
        student_id: "2",
        counsellor_regularisation: true,
        followed: false
    };

    let registryEventLateness: RegistryEvent = {
        type: "LATENESS",
        startDate: "2020-01-01 08:00:00",
        endDate: "2020-01-01 09:00:00",
        student_id: "2",
        counsellor_regularisation: false,
        followed: false
    };

    beforeEach(() => {
        // Registering a module for testing
        const testApp = angular.module('app', []);
        let $controller, $rootScope;

        // Mockup test module
        angularMock.module('app');

        // Instantiation of the services, controllers, directives we need
        registryController;

        // Adding services, controllers, directives in the module
        ng.initMockedModules(testApp);

        // Controller Injection
        angularMock.inject((_$controller_, _$rootScope_) => {
            // The injector unwraps the underscores (_) from around the parameter names when matching
            $controller = _$controller_;
            $rootScope = _$rootScope_;
        });

        // Creates a new instance of scope
        let $scope = $rootScope.$new();

        // Fetching $location
        testApp.run(($rootScope, $location) => {
            $rootScope.location = $location;
        });

        // Controller Recovery
        registryControllerTest = $controller('RegistryController', {
            $scope: $scope,
            $route: undefined,
            $location: $rootScope.location,
            searchService: SearchService,
            groupService: GroupService,
            registryService: registryService
        });
    });

    it('test isAbsenceRegularized', done => {
        expect(registryControllerTest.isAbsenceRegularized([registryEventNotRegularized, registryEventRegularized]))
            .toEqual(false);
        expect(registryControllerTest.isAbsenceRegularized([registryEventRegularized])).toEqual(true);
        expect(registryControllerTest.isAbsenceRegularized([registryEventRegularized, registryEventLateness])).toEqual(true);
        done();
    });
});