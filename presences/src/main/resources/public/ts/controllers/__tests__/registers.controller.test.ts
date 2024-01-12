import {ng} from '@presences/models/__mocks__/entcore';
import * as angular from 'angular';
import * as angularMock from 'angular-mocks/angular-mocks';
import {Course} from "@presences/models";
import DoneCallback = jest.DoneCallback;
import {registersController, ViewModel} from "@presences/controllers";
import {GroupService, groupService, SearchService} from "@common/services";
import {ReasonService, reasonService} from "@presences/services";

describe('RegistersController', () => {

    let registersVm: ViewModel;

    beforeEach(() => {
        const testApp = angular.module('app', []);
        let $controller, $rootScope;

        angularMock.module('app');

        registersController;

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


        registersVm = $controller('RegistersController', {
            $scope: $scope,
            $timeout: undefined,
            $route: undefined,
            $location: $rootScope.location,
            $rootScope: $rootScope,
            searchService: SearchService,
            groupService: GroupService,
            reasonService: ReasonService
        });


    });

    it('test isFuturCourse', (done: DoneCallback): void => {
        let course: Course = undefined;

        expect(registersVm.isFuturCourse(course)).toEqual(true);

        let startDate: string = '2020-01-01 00:00:00';
        course = new Course();
        course.startDate = startDate;

        expect(registersVm.isFuturCourse(course)).toEqual(false);

        done();
    });


});